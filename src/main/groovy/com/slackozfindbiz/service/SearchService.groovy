package com.slackozfindbiz.service
 
import groovyx.gaelyk.logging.GroovyLogger
import com.slackozfindbiz.datasource.SapiDataSource
import com.slackozfindbiz.utils.StringUtils

class SearchService {
    private final static def LOG = new GroovyLogger(SearchService.class.name)
    private final static def SEARCH_PREFIXES = [ 'find me ', 'search for ', 'map me ' ]
    private final static def WHAT_WHERE_SEPARATORS = [ ' near ', ' in ', ' around ', ' nearby ', ' on ', ' at ' ]
    private final static def MAP_BASE_URL = 'http://maps.googleapis.com/maps/api/staticmap?size=400x400&maptype=roadmap'
    
    private def sapiDataSource
    private def defaultWhere
    private def slackTokens
    
    def SearchService(config) {
        this.sapiDataSource = new SapiDataSource(config)
        this.defaultWhere = config.ozfindbiz.defaultLocation
        this.slackTokens = config.ozfindbiz.slack.tokens
    }
    
    def search( params, userIp, userSessionId ) {
        def token = params.token
        def tokenIsValid = (slackTokens.size() == 0) || (token != null && slackTokens.contains(token))
        
        def inText = params.text?.trim()?.toLowerCase()
        def searchPrefix = SEARCH_PREFIXES[0]
        SEARCH_PREFIXES.each( { prefix ->
            if ( inText.startsWith(prefix) ) {
                searchPrefix = prefix
            }
        } )
        
        def mapLinkOnly = (searchPrefix == 'map me ')
         
        def outText = ''
         
        if (tokenIsValid && inText != null && inText.startsWith(searchPrefix) && inText.length() > searchPrefix.length()) {
            def queryText = inText.substring(searchPrefix.length())
            LOG.info("Query text : " + queryText)
            def parts = splitIntoWhatWhere(queryText)
            LOG.info("Searching for ${parts.what}${parts.joinWord}${parts.where} ...")
            def searchResults = sapiDataSource.search( parts.what, parts.where )
            def listings = searchResults.listings
            def centre = searchResults.searchCentre
            LOG.info("Found ${listings.size()} ${parts.what}${parts.joinWord}${parts.where}.")
            
            def lines = listings.collect({ listing -> 
                def listingText = StringUtils.encodeForSlack(listing.name + ' at ' + listing.displayAddress)
                def shouldGenerateListingLink = listing.url && !mapLinkOnly
                "${listing.position}. " + (shouldGenerateListingLink ? '<' + StringUtils.encodeForSlack(listing.url) + '|' + listingText + '>' : listingText)
            })
            
            def whatAndWhereMsg = StringUtils.encodeForSlack("${parts.what}${parts.joinWord}${parts.where}")
            
            if (lines.empty) {
                outText = "Found no ${whatAndWhereMsg}."
            } else {
                def mapCenterParam = centre ? "&center=${centre.latitude},${centre.longitude}" : ''
                def markerParams = listings.collect({ listing -> 
                    "&markers=label:${listing.position}%7C${listing.latitude},${listing.longitude}"
                })
                def fullMapUrl = MAP_BASE_URL + mapCenterParam + markerParams.join('')
                def mapText = mapLinkOnly ? '(<' + StringUtils.encodeForSlack(fullMapUrl) + '|Map view>) ' : ''
                outText = "${whatAndWhereMsg} ${mapText}:\\n" + lines.join(",\\n")
                
                // TODO Make these calls async if they cause too much delay
                sapiDataSource.reportAppearance(listings, userIp, userSessionId)
                if (mapLinkOnly) {
                    sapiDataSource.reportViewMap(listings, userIp, userSessionId)
                }
            }
        }
        
        outText
    }
    
    private splitIntoWhatWhere(origQueryText) {
        def queryText = origQueryText            
        
        if (queryText.length() > 1 && queryText.endsWith('.')) {
            queryText = queryText.substring(0, queryText.length() - 1).trim()
        }
    
        def what = queryText
        def where = defaultWhere
        def joinWord = WHAT_WHERE_SEPARATORS[0]
        
        for ( sep in WHAT_WHERE_SEPARATORS) {
            def sepIdx = queryText.indexOf(sep)
            if (sepIdx > 0) { // Separator found and not at the start
                joinWord = sep
                what = queryText.substring(0, sepIdx)
                where = queryText.substring(sepIdx + sep.length())
                where = ('me' == where) ? defaultWhere : where
                break;
            }
        }
        
        // Remove certain stopwords from the front of the what, if there is some text following it.
        def strippedStopWords = what.replaceFirst('^(the|a|an|any|some)\\s+', '')
        if (strippedStopWords.trim().length() > 0) {
            what = strippedStopWords
        } 
        
        [
            what: StringUtils.decodeFromSlack(what),
            where: StringUtils.decodeFromSlack(where),
            joinWord: joinWord
        ]
    }
}
