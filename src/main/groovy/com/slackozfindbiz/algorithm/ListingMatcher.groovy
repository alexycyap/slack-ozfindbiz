package com.slackozfindbiz.algorithm
import groovyx.gaelyk.logging.GroovyLogger
import com.google.common.geometry.S2LatLng
import com.slackozfindbiz.utils.StringUtils

class ListingMatcher {
    private final static def LOG = new GroovyLogger(ListingMatcher.class.name)  
    private final static def THRESHOLD_DISTANCE_METERS = 10
    private final static double NAME_OVERLAP_THRESHOLD = 0.55
    
    def dedup( listings, resultSize ) {
        listings.inject([]) { uniqueList, listing ->
            if (uniqueList.size() < resultSize) {
                def foundIndex = (uniqueList.empty) ? -1 : listingIndexInList(uniqueList, listing)
                if (foundIndex >= 0) {
                    def listingInUniqueList = uniqueList[foundIndex]
                    if (listing.rankScore > listingInUniqueList.rankScore) {
                        uniqueList[foundIndex] = listing
                        LOG.info("Evicted duplicate listing \"${listingInUniqueList.name}\"")
                    } else {
                        LOG.info("Rejected duplicate listing \"${listing.name}\"")
                    }
                } else {
                    uniqueList << listing
                }
            }
            uniqueList
        }
    }

        
    private def listingIndexInList(theList, listing) {
        theList.findIndexOf { fromTheList ->
            nameExceedsThresholdMatch(fromTheList, listing) && (distanceWithinThreshold(fromTheList, listing) || addressMatch(fromTheList, listing))
        }
    }
    
    private def addressMatch(listing1, listing2) {
        if ((listing1.suburb == listing2.suburb) && (listing1.postcode = listing2.postcode)) {
            def normalizedStreet1 = StreetAddressLineNormalizer.normalize(listing1.streetAddressLine)
            def normalizedStreet2 = StreetAddressLineNormalizer.normalize(listing2.streetAddressLine)
            (normalizedStreet1 == normalizedStreet2)
        } else {
            false
        }
        
    }
    
    private def nameExceedsThresholdMatch(listing1, listing2) {
        def combinedCategories = [ listing1.categoryName, listing2.categoryName ]
        def listing1NormName = NameNormalizer.removeCategoryNamesFromTokens(listing1.nameTokens, combinedCategories).join(' ')
        def listing2NormName = NameNormalizer.removeCategoryNamesFromTokens(listing2.nameTokens, combinedCategories).join(' ')
        double overlapRatio = StringUtils.overlapFromLeftRatio(listing1NormName, listing2NormName)
        // LOG.info('Overlapping ' + listing1NormName + ' : ' + listing2NormName + ' , ratio = ' + overlapRatio)
        overlapRatio > NAME_OVERLAP_THRESHOLD
    }
    
    private def distanceWithinThreshold(listing1, listing2) {
        if (listing1.hasGeocode() && listing2.hasGeocode()) {
            distanceBetween(listing1, listing2) < THRESHOLD_DISTANCE_METERS
        } else {
            false
        }
    }
    
    private double distanceBetween(listing1, listing2) {
        def latLng1 = S2LatLng.fromDegrees(listing1.latitude.toDouble(), listing1.longitude.toDouble())
        def latLng2 = S2LatLng.fromDegrees(listing2.latitude.toDouble(), listing2.longitude.toDouble())
        latLng1.getEarthDistance(latLng2)
    }
}
