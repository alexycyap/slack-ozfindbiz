package com.slackozfindbiz.utils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

class IOUtils {

    static streamJsonStr(jsonStr, resp, sout, status = 200) {
        byte[] outputBytes = jsonStr ? jsonStr.getBytes() : new byte[0]
        streamBytes(outputBytes, 'application/json', resp, sout, status)
    }
    

    static streamBytes(bytes, contentType, resp, sout, status = 200) {
        resp.status = status
        resp.setHeader('Content-Type', contentType)
        def outputLength = bytes.length
        resp.setIntHeader('Content-Length', bytes.length)
       
        try {
            sout << bytes
        } finally {
            sout.flush()
            sout.close()
        }

    }
    
}
