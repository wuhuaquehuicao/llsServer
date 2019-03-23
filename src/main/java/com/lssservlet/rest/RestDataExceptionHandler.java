package com.lssservlet.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.lssservlet.utils.DataException;

@Provider
public class RestDataExceptionHandler implements ExceptionMapper<DataException> {

    @Override
    public Response toResponse(DataException exception) {
        return Response.status(exception.getStatus()).entity(exception).build();
    }

}
