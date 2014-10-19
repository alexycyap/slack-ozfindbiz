package com.slackozfindbiz.algorithm

class NameNormalizer {
    private final static def COMMON_NAME_STOPWORDS = ['at', 'the', 'and', 'a', 'company', 'australia']
    private final static def CATEGORY_NAME_STOPWORDS = ['or', 'and']

    static def tokenise(original, phrasesToRemove) {

        new NameNormalizer(original)
        .toLower()
        .removeAprosS()
        .removeBracketed()
        .removeNonAlphaNumeric()
        .removePtyLtd()
        .removeExactPhrases(phrasesToRemove)
        .compactSpaces()
        .removePlurals()
        .removeStopWords(COMMON_NAME_STOPWORDS)
        .toTokens()
    }
    
    static def removeCategoryNamesFromTokens(originalTokens, categoryNames) {
        def concatCatNames = categoryNames.join(' ')
        def normalizedCategoryTokens = new NameNormalizer(concatCatNames)
                                    .toLower()
                                    .removeAprosS()
                                    .removeBracketed()
                                    .removeNonAlphaNumeric()
                                    .compactSpaces()
                                    .removePlurals()
                                    .removeStopWords(CATEGORY_NAME_STOPWORDS)
                                    .toTokens()
                                    
        def tokens = []
        tokens.addAll(originalTokens) 
        tokens.removeAll normalizedCategoryTokens
        // Do not remove the stop words if all tokens are stop words
        if ( tokens.isEmpty() ) {
            originalTokens
        } else {
            tokens
        }
    }
            
    private String normalized
    
    private NameNormalizer(original) {
        normalized = original.trim()
    }
    
    def toLower() {
        normalized = normalized.toLowerCase()
        this
    }
    
    def removeAprosS() {
        normalized = normalized.replaceAll('\'s','')
        this
    }
    
    def removeBracketed() {
        normalized = normalized.replaceAll('\\(.*\\)','')
        this
    }

    def removeNonAlphaNumeric() {
        normalized = normalized.replaceAll('[^ a-zA-Z0-9]', ' ')
        this
    }
        
    def removePtyLtd() {
        normalized = normalized.trim().replaceAll('\\s+p\\s*l$','').replaceAll('\\s+pty\\s*ltd$','')
        this
    }

        
    def removeExactPhrases(phrases) {
        for (phrase in phrases) {
            normalized = normalized.replaceAll(phrase, ' ')
        }
        normalized = normalized.trim()
        this
    }
    
    def compactSpaces() {
        normalized = normalized.replaceAll('\\s+', ' ')
        this
    }
    
    def removePlurals() {
        def tokens = toTokens()
        def singularTokens = tokens.collect { token ->
            def singularToken = token
            if (token.length() > 3 && token.endsWith('es')) {
                singularToken = token.substring(0, token.length() - 2)
            } else if (token.length() > 2 && token.endsWith('s')) {
                singularToken = token.substring(0, token.length() - 1)
            }
            singularToken
        }
        normalized = singularTokens.join(' ')        
        this
    }
    
    def removeStopWords(stopwords) {
        def tokens = toTokens()
        tokens.removeAll stopwords
        // Do not remove the stop words if all tokens are stop words
        if ( !tokens.isEmpty() ) {
            normalized = tokens.join(' ')
        }
        this
    }

    def toTokens() {
        normalized.split(' ') as List
    }
    
    String toString() {
        normalized
    }

    
}
