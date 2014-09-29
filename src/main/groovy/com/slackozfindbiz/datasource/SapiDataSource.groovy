package com.slackozfindbiz.datasource
import groovy.json.JsonSlurper
import groovyx.gaelyk.logging.GroovyLogger
import groovyx.net.http.URIBuilder
import com.slackozfindbiz.domain.SapiListing

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
    
    def SapiDataSource(config) {
        this.config = config
        this.resultSize = Math.min( MAX_RESULT_SIZE, config.ozfindbiz.resultSize.toInteger())
        def sapiMode = config.ozfindbiz.sapi.mode
        this.listingsEndPoint = "/v1/${ sapiMode }/search"
        this.landmarkEndPoint = "/v1/${ sapiMode }/oneSearch"
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
        def landmarkSapiParams = [
                        query: where,
                        includePois: true,
                        rows: 1,
                        mappable: true,
                        categoryId: ['27952','35408','624684','367865','358989','370208','338408','21962','33839','31445','1002502','1007376'],
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
                        listings.addAll( jsonInput.results.take(resultSize).collect({ listing ->
                            def sapiListing = new SapiListing()
                            sapiListing.name = listing.name
                            sapiListing.address = (listing.primaryAddress?.addressLine ? listing.primaryAddress?.addressLine + ', '  : '') + listing.primaryAddress.suburb
                            sapiListing.url = listing.detailsLink
                            sapiListing.latitude = listing.primaryAddress?.latitude
                            sapiListing.longitude = listing.primaryAddress?.longitude
                            sapiListing
                        }) )
                        
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

}
