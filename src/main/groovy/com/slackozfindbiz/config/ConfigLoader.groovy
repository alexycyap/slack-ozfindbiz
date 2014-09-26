package com.slackozfindbiz.config
import groovy.json.JsonSlurper
import groovyx.gaelyk.logging.GroovyLogger

class ConfigLoader {
    final static String CONTEXT_ATTRIBUTE_NAME = 'ozfindbizConfig'
    private final static def LOG = new GroovyLogger(ConfigLoader.class.name)
    
    static def init(context) {
        def config = context.getAttribute(CONTEXT_ATTRIBUTE_NAME)
        if (config == null) {
            def file = new File('config.json')
            LOG.config("Loading configuration from ${file.absolutePath}")
            config = new JsonSlurper().parseText(file.getText())
            context.setAttribute(CONTEXT_ATTRIBUTE_NAME, config)
        }
    }
    
    static def getConfig(context) {
        init(context)
        context.getAttribute(CONTEXT_ATTRIBUTE_NAME)
    }
} 
