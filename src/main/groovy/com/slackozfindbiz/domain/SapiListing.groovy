package com.slackozfindbiz.domain

class SapiListing {
    def id
    def name
    def rankScore
    def nameTokens
    def displayAddress
    def streetAddressLine
    def suburb
    def postcode
    def url
    def latitude
    def longitude
    def position
    def reportingId
    def categoryName
    def yelpUrl    

    def hasGeocode() {
        latitude != null && longitude != null
    }
    
    public boolean equals(Object obj) {
        def eql = false
        if (obj instanceof SapiListing) {
            eql = (this.id == obj.id)
        }
        eql
    }
    
    public int hashCode() {
        id.hashCode()
    }
} 
