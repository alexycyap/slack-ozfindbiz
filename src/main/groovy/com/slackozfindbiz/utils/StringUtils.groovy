package com.slackozfindbiz.utils

class StringUtils {


    static encodeForSlack(inText) {
        inText?.replaceAll('&','&amp;')?.replaceAll('<','&lt;')?.replaceAll('>','&gt;')
    }

    
}
