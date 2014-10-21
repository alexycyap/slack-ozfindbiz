package com.slackozfindbiz.utils

class StringUtils {

    static encodeForSlack(inText) {
        inText?.replaceAll('&','&amp;')?.replaceAll('<','&lt;')?.replaceAll('>','&gt;')
    }

    // Matching the strings starting from the left-side (more meaningful) towards the right (less meaningful), till where they differ.
    // The index where they differ is diffIndex
    // Returns the ratio of diffIndex over the length of the longer string.
    // If both strings are identical, returns 1. If there is no overlap at all, returns 0.
    // If shorter string is empty, returns 0.
    static overlapFromLeftRatio(str1, str2) {
        def longerStr = str1
        def shorterStr = str2        
        if ( str2.length() > str1.length() ) {
            longerStr = str2
            shorterStr = str1
        }
        
        int shorterLength = shorterStr.length()
        if (shorterLength == 0) {
            return 0.0
        } 
        
        int diffIndex = 0
        while (diffIndex < shorterLength) {
            if (longerStr[diffIndex] != shorterStr[diffIndex]) {
                break
            }
            diffIndex = diffIndex + 1
        }
        
        int longerLength = longerStr.length()
        if (diffIndex == longerLength) {
            1.0
        } else {
            (diffIndex as Double) / longerLength
        }
    }
}
