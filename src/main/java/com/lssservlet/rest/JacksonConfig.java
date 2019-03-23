package com.lssservlet.rest;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lssservlet.utils.Json;

@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    protected static final Logger log = LogManager.getLogger(JacksonConfig.class);

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return Json.mapper;
    }
}
