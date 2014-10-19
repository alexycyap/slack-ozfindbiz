package com.slackozfindbiz.algorithm

class StreetAddressLineNormalizer {

    static def normalize(original) {
        new StreetAddressLineNormalizer(original)
            .removeNonAlphaNumeric()
            .compactSpaces()
            .expandCommonAbbrev()
            .reverse()
            .truncateAfterEndOfNumber()
            .toString()
    }
    
    private String normalized
    
    private StreetAddressLineNormalizer(original) {
        normalized = original.trim()
    }
    
    private def removeNonAlphaNumeric() {
        normalized = normalized.replaceAll('[^ a-zA-Z0-9]', ' ')
        this
    }
    
    private def expandCommonAbbrev() {
        def tokens = normalized.split(' ') as List
        def expandedTokens = tokens.collect { token ->
            def expanded = token
            // TODO Replace switch-case with a map lookup when we have more expansions
            switch ( token ) {
                case 'lt': expanded = 'little'; break
            }
            expanded
        }
        normalized = expandedTokens.join(' ') 
        this
    }
    
    private def compactSpaces() {
        normalized = normalized.replaceAll('\\s+', ' ')
        this
    }
    
    private def reverse() {
        normalized = normalized.reverse()
        this
    }
    
    private def truncateAfterEndOfNumber() {
        if (normalized) {
            def regexMatch = normalized.find(~/^(.*?[0-9])[^0-9]/) { full, group -> group }
            normalized = regexMatch ? regexMatch : normalized
        }
        
        this
    }
    
    String toString() {
        normalized
    }


}
