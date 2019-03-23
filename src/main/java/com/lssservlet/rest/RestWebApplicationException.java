package com.lssservlet.rest;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;

@Provider
public class RestWebApplicationException implements ExceptionMapper<WebApplicationException> {
    @Context
    HttpRequest request;
    @Context
    SecurityContext securityContext;
    protected static final Logger log = LogManager.getLogger(RestWebApplicationException.class);

    public static String getClientIpAddr(MultivaluedMap<String, String> headers) {
        String ip = null;
        if (headers != null) {
            ip = headers.getFirst("X-Real-IP");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = headers.getFirst("X-Forwarded-For");
            }
        }
        if (ip == null)
            ip = "";
        return ip;
    }

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception instanceof NotSupportedException) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new DataException(ErrorCode.BAD_REQUEST, "invalid media type", exception.getMessage()))
                    .build();
        }
        log.warn("handle web exception", exception);
        return exception.getResponse();
    }
}
