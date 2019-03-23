package com.lssservlet.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;

@Provider
public class JsonParseExceptionHandler implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        return Response.status(Status.BAD_REQUEST)
                .entity(new DataException(ErrorCode.BAD_REQUEST, "invalid request body", exception.getMessage()))
                .build();
    }

}
