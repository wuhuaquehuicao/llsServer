package com.lssservlet.api;

import java.util.ArrayList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSExportHistory;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.managers.ExportManager;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.DataException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/exports")
@Api("exports")
public class HandleExports extends HandleBase {
    public static class ExportRequestParams {
        public String url;
        public ArrayList<String> clauses;
        public Integer limit;
        public Integer offset;
        public String or;
        public String order;
        public Integer desc;
        public Long from;
        public Long to;

        public String toString() {
            String result = null;
            result = url + "?";
            if (clauses != null && clauses.size() > 0) {
                for (String c : clauses)
                    result += "clause=" + c + "&";
            }
            if (limit != null)
                result += "&limit=" + limit;
            if (offset != null)
                result += "&offset=" + offset;
            if (or != null)
                result += "&or=" + or;
            if (order != null)
                result += "&order=" + order;
            if (desc != null)
                result += "&desc=" + desc;

            return result;
        }

        public ADSDbKey.Type getType() {
            ADSDbKey.Type result = null;
            if (url != null) {
                if (url.contains("ads/statics"))
                    result = Type.EAdStatic;
                else if (url.contains("adlists"))
                    result = Type.EAdlist;
                else if (url.contains("devices"))
                    result = Type.EDevice;
                else if (url.contains("users"))
                    result = Type.EUser;
                else if (url.contains("ads"))
                    result = Type.EAd;
                else if (url.contains("locations"))
                    result = Type.ELocation;
            }
            return result;
        }
    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response getExports(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSExportHistory> ehs = null;
            int total = 0;

            ehs = ExportManager.getInstance().getExports(false);
            total = ehs.size();
            if (ehs != null && ehs.size() > 0) {
                response = createOkResponse(ehs, total);
            } else {
                response = createNullListResponse(total);
            }
            return Response.status(Status.OK).entity(response).build();
        });

    }

    @PermitAll
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response createExport(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            ExportRequestParams params) {

        return handleException(() -> {
            ADSUser admin = null;
            UserPrincipal principal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = principal.getToken();
            if (token != null)
                admin = token.getUser();
            ADSExportHistory eh = ExportManager.getInstance().createExport(params,
                    (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(eh)).build();
        });

    }

    @PermitAll
    @GET
    @Path("/{export_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response getExport(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("export_id") String exportId) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");

            ADSExportHistory eh = ADSExportHistory.getExportHistory(exportId);
            if (eh == null)
                throw new DataException(ErrorCode.BAD_REQUEST, "Not found export history: " + exportId);
            return Response.status(Status.OK).entity(createOkResponse(eh)).build();
        });

    }
}
