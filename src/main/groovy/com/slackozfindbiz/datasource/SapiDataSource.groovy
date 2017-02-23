package com.slackozfindbiz.datasource
import groovy.json.JsonSlurper
import groovyx.gaelyk.logging.GroovyLogger
import groovyx.net.http.URIBuilder
import com.slackozfindbiz.domain.SapiListing
import com.slackozfindbiz.algorithm.NameNormalizer
import com.slackozfindbiz.algorithm.ListingMatcher


class SapiDataSource {
    private final static def LOG = new GroovyLogger(SapiDataSource.class.name)  
    private final static def MAX_TRIES = 5
    private final static def MAX_RESULT_SIZE = 20
    private final static def REQUEST_CONNECTION_MAP = [connectTimeout:5000, readTimeout:20000, requestProperties: ['Content-Type': 'application/json']]
    private final static def SAPI_HOST = 'http://api.sensis.com.au'
    def config
    def resultSize
    def listingsEndPoint
    def landmarkEndPoint
    def reportEndPoint
    
    def SapiDataSource(config) {
        this.config = config
        this.resultSize = Math.min( MAX_RESULT_SIZE, config.ozfindbiz.resultSize.toInteger())
        def sapiMode = config.ozfindbiz.sapi.mode
        this.listingsEndPoint = "/v1/${ sapiMode }/search"
        this.landmarkEndPoint = "/v1/${ sapiMode }/oneSearch"
        this.reportEndPoint = "/v1/${ sapiMode }/report"
    }
    
    def search(what, where) {
        def whereHasAlpha = where.matches('.*[a-z].*')
        def whereHasComma = where.contains(',')
        def startsWithNumber = where.matches('^[0-9].*')
        def startsWithFourNumbers = where.matches('^[0-9][0-9][0-9][0-9].*')
        def whereLikelyGeocode = whereHasComma && !whereHasAlpha
        def whereLikelyLandmark = false
        def whereLikelyPropertyAddress = startsWithNumber && (!startsWithFourNumbers) && whereHasAlpha && (where.length() > 10)
        
        def listingsSapiParams = [
                        query: what,
                        location: where,
                        sensitiveCategories: true,
                        includePois: true,
                        rows: 50,
                        mappable: true,
                        key: config.ozfindbiz.sapi.key
                        ]

        def searchResults = [
            listings: [],
            searchCentre: null
        ]

        if (whereLikelyGeocode) {
            listingsSapiParams.radius = 0.5
            LOG.info("${where} is treated as a geocode")
        } else if (whereLikelyPropertyAddress) {
            listingsSapiParams.radius = 1.0
            LOG.info("${where} is treated as a property address")
        } else if (!startsWithNumber) {   
            def landmarkGeocode = queryForLandmark(where)
            if (landmarkGeocode) {
                whereLikelyLandmark = true
                listingsSapiParams.radius = 1.0
                listingsSapiParams.location = landmarkGeocode.latitude + ',' + landmarkGeocode.longitude
            }
        }
        
        // LOG.info("Searching for ${what} around ${listingsSapiParams.location}...")
        if (whereLikelyGeocode || whereLikelyPropertyAddress || whereLikelyLandmark) {
            listingsSapiParams.sortBy = 'DISTANCE'
            while ((searchResults.listings.size() < resultSize) && listingsSapiParams.radius < 100) {
                searchResults = searchListingsWithParams(listingsSapiParams)
                
                LOG.info("Found ${ searchResults.listings.size() } ${what} within ${listingsSapiParams.radius} kms of ${listingsSapiParams.location}.")
                if (searchResults.listings.size() < resultSize) {
                    listingsSapiParams.radius = listingsSapiParams.radius * 10
                }
            }
        } else {
            searchResults = searchListingsWithParams(listingsSapiParams)
        }
        
        searchResults
    } 
    
    private def queryForLandmark(where) {
        def landmarkGeocode = null

        // 35947   : Convention centres
        // 27952   : Car Parking
        // 35408   : Universities
        // 21962   : Tourist attractions
        // 33839   : Shopping centres
        // 31445   : Hotels
        // 1001004 : Parks
        def landmarkSapiParams = [
                        query: where,
                        includePois: true,
                        rows: 1,
                        mappable: true,
                        intentOverride: 'NAME',
                        categoryId: ['35947', '27952','35408','21962','33839','31445','1002502','1007376', '1001004'],
                        key: config.ozfindbiz.sapi.key
                        ]
        
        def url = new URIBuilder(SAPI_HOST).setPath(landmarkEndPoint).setQuery(landmarkSapiParams).toURL()
        try {
            def responseText = url.getText(REQUEST_CONNECTION_MAP)
            if (responseText) {
                def jsonInput = new JsonSlurper().parseText(responseText)
                if (jsonInput.results) {
                    jsonInput.results.each({ landmark ->
                        LOG.info("${where} is treated as a landmark located in ${landmark.primaryAddress?.suburb} ${landmark.primaryAddress?.state}")
                        landmarkGeocode = [
                            latitude: landmark.primaryAddress?.latitude,
                            longitude: landmark.primaryAddress?.longitude
                        ]
                    })
                }
            } else {
                LOG.severe("Failed or timed-out searching for ${where}")
            }
        } catch (Exception e) {
            LOG.severe( "Error searching for ${where} : ${e.message}" )
        }
        landmarkGeocode
    }

    private def searchListingsWithParams(sapiParams) {
        def what = sapiParams.query
        def where = sapiParams.location
        
        def listings = []
        def searchCentre = null
        def url = new URIBuilder(SAPI_HOST).setPath(listingsEndPoint).setQuery(sapiParams).toURL()
        // LOG.info("About to query ${url}")
        def success = false
        def tries = 0
        while ((tries < MAX_TRIES) && (!success)) {
            tries = tries + 1
            try {
                def responseText = url.getText(REQUEST_CONNECTION_MAP)
                if (responseText) {
                    def jsonInput = new JsonSlurper().parseText(responseText)
                    if (jsonInput.results) {
                        success = true
                        def allListings = []
                        allListings.addAll( jsonInput.results.collect({ listing ->

                            def sapiListing = new SapiListing()
                            sapiListing.id = listing.id
                            sapiListing.name = listing.name
                            sapiListing.rankScore = calculateRankScore(listing)                            
                            sapiListing.displayAddress = (listing.primaryAddress.addressLine ? listing.primaryAddress.addressLine + ', '  : '') + listing.primaryAddress.suburb
                            sapiListing.streetAddressLine = listing.primaryAddress.addressLine?.toLowerCase()
                            sapiListing.suburb = listing.primaryAddress.suburb.toLowerCase()
                            sapiListing.postcode = listing.primaryAddress.postcode
                            sapiListing.url = listing.detailsLink
                            sapiListing.latitude = listing.primaryAddress?.latitude
                            sapiListing.longitude = listing.primaryAddress?.longitude
                            sapiListing.reportingId = listing.reportingId
                            sapiListing.categoryName = listing.categories ? listing.categories[0]?.name : null
                            sapiListing
                        }) )
                        
                        def firstListingWithCategory = allListings.find { it.categoryName != null }
                        def topCategory = (firstListingWithCategory != null) ? firstListingWithCategory.categoryName : ''
                        allListings = allListings.collect { lst ->
                            if (lst.categoryName == null) {
                                lst.categoryName = topCategory
                            }
                            def phrasesToRemove = [ lst.suburb ]
                            lst.nameTokens = NameNormalizer.tokenise(lst.name, phrasesToRemove)
                            lst
                        }
                        
                        listings = new ListingMatcher().dedup(allListings, resultSize)
                        
                        listings.eachWithIndex { listing, index -> 
                            listing.position = index + 1
                        }
                        
                        def responseCentre = jsonInput.bestLocation.coordinates.centre
                        searchCentre = [
                            latitude: responseCentre.latitude,
                            longitude: responseCentre.longitude
                        ]
                    }
                } else {
                    LOG.severe("Failed or timed-out searching for ${what} around ${where}")
                }
            } catch (Exception e) {
                LOG.severe( "Error searching for ${what} around ${where} : ${e.message}" )
            }
            
            if (!success) {
                Thread.sleep(2000)
            }
        } 
        
        [
            listings : listings,
            searchCentre: searchCentre
        ]  
    }
    
    def reportAppearance(listings, userIp, userSessionId) {
        report(listings,'appearance', userIp, userSessionId)
    }

    def reportViewMap(listings, userIp, userSessionId) {
        report(listings,'viewMap', userIp, userSessionId)
    }
    
    private def report(listings, eventType, userIp, userSessionId) {
        def reportingIds = listings.findAll({ it.reportingId != null }).collect({ it.reportingId })
        if (! reportingIds.empty) {
            def sapiParams = [
                             userIp: userIp,
                             userSessionId: userSessionId,
                             id: reportingIds,
                             key: config.ozfindbiz.sapi.key
                             ]
            def url = new URIBuilder(SAPI_HOST).setPath(reportEndPoint + '/' + eventType).setQuery(sapiParams).toURL()
            //LOG.info("About to post to ${url}")

            try {
                def sapiResponse = url.post( )
                if (sapiResponse.statusCode in 200..299 ) {
                    LOG.info("Successfully reported eventType ${eventType}.")
                } else {
                    LOG.severe("Got error code ${sapiResponse.statusCode} while trying to report eventType ${eventType}.")
                }
                
            } catch (Exception e) {
                LOG.severe("Error reporting eventType ${eventType} : ${e.class.name} : ${e.message}")
            }
        }
    }
    
    // POI gets score of 0, White gets 1, Free Yellow gets 2, Paid Yellow gets 2+
    private calculateRankScore(listing) {
        def score = 0
        if (listing.detailsLink) {
            score = score + (listing.detailsLink.contains('yellowpages.com.au') ? 2 : 1)
        }
        
        if (listing.openingHours) {
            score = score + 1
        }
        
        if (listing.imageGallery) {
            score = score + 1
        }
        
        if (listing.businessLogo) {
            score = score + 1
        }

        score
    }

}
