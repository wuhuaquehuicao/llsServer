package com.lssservlet.api;

import java.util.ArrayList;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.datamodel.ADSData;
import com.lssservlet.response.FError;
import com.lssservlet.response.FFailResponse;
import com.lssservlet.response.FItems;
import com.lssservlet.response.FOkResponse;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.utils.DataException;

@Secured
public abstract class HandleBase {

    protected static final Logger log = LogManager.getLogger(HandleBase.class);
    private static final String RESPONSE_STATUS_OK = "OK";
    private static final String RESPONSE_STATUS_FAIL = "FAIL";
    protected static final int LIST_LIMIT = 100;

    @Context
    SecurityContext _securityContext;

    public static class FlagParams {
        public String id;
        public Integer flag;
    }

    public static class QueryResultParams {
        public ArrayList<?> elements;
        public Integer page_index;
        public Integer page_count;
        public Integer total_page;
        public Integer total_count;
    }

    public static class QueryParams1 {
        public String type;
        public ArrayList<String> clauses;
        public Integer limit;
        public Integer offset;
        public ArrayList<String> orders;
        public Boolean or;
        public ArrayList<String> groups;
        public Boolean desc;
    }

    protected interface HandleInterface {
        public Response invoke() throws DataException, Exception;
    }

    protected Response handleException(HandleInterface handle) {
        try {
            return handle.invoke();
        } catch (DataException de) {
            log.warn(de.getLocalizedMessage());
            FResponse response = createFailResponse(de.errorCode(), de.getMessage());
            return Response.status(Status.OK).entity(response).build();
        } catch (Exception e) {
            FResponse response = createFailResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
            return Response.status(Status.OK).entity(response).build();
        }
    }

    protected FResponse createOkResponse(ADSData data) {
        FOkResponse response = new FOkResponse();
        response.status = RESPONSE_STATUS_OK;
        response.result = data;
        return response;
    }

    protected FResponse createOkResponse(String data) {
        FOkResponse response = new FOkResponse();
        response.status = RESPONSE_STATUS_OK;
        response.result = data;
        return response;
    }

    protected FResponse createOkNullResponse() {
        FOkResponse response = new FOkResponse();
        response.status = RESPONSE_STATUS_OK;
        response.result = null;
        return response;
    }

    protected FResponse createNullListResponse(int total) {
        FOkResponse response = new FOkResponse();
        response.status = RESPONSE_STATUS_OK;
        FItems items = new FItems();
        items.items = new ArrayList<>();
        items.count = 0;
        items.totalcount = total;
        response.result = items;
        return response;
    }

    protected FResponse createOkResponse(ArrayList<?> data, int total) {
        FOkResponse response = new FOkResponse();
        response.status = RESPONSE_STATUS_OK;
        FItems items = new FItems();
        items.items = data;
        if (data != null)
            items.count = data.size();
        items.totalcount = total;
        response.result = items;
        return response;
    }

    protected FResponse createFailResponse(int code, String reason) {
        FFailResponse response = new FFailResponse();
        response.status = RESPONSE_STATUS_FAIL;
        FError error = new FError();
        error.code = code;
        error.reason = reason;
        response.error = error;
        return response;
    }
}
