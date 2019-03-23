package com.lssservlet.api;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.main.Version;
import com.lssservlet.rest.Secured;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/application")
@Api(value = "Application")
public class HandleApplication extends HandleBase {
    protected static final Logger log = LogManager.getLogger(HandleApplication.class);

    public static class LogData {
        public Boolean skipCache;
        public String adId;

        public String toString() {
            return "\"skipCache\":" + skipCache + ",\"ad_id\":\"" + adId + "\"";
        }
    }

    public static class LogParams {
        public LogData data;
    }

    @PermitAll
    @GET
    @Path("/reload")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "reload config in database", notes = "Reload config from database")
    public Response reloadConfig(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int api_version) {
        try {
            Config.getInstance().reloadConfig();
        } catch (DataException e) {
        }
        return Response.status(Status.OK).entity(null).build();
    }

    @PermitAll
    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List application info", notes = "List application info")
    public Response info(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int api_version) {
        JsonObject result = new JsonObject();
        result.put("version", Version._VER);
        result.put("commit", Version._COMMIT_HASH);
        result.put("name", Config.getInstance().getServerName());
        result.put("serverId", Config.getInstance().getServerId());
        result.put("changes", Config.getInstance().getChanges().trim());
        result.put("nodes", DataManager.getInstance().getNodes());
        result.put("timestamp", DataManager.getInstance().time());

        return Response.status(Status.OK).entity(result).build();
    }

    public static class QueryParams {
        public ArrayList<String> columns;
        public String type;
        public ArrayList<String> clauses;
        public ArrayList<String> orders;
        public Integer limit;
    }

    @PermitAll
    @POST
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Query data from database", notes = "Query and return json object")
    public Response query(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            QueryParams params) {
        return handleException(() -> {
            List<?> results = DataManager.getInstance().queryFromDatabase(params);
            return Response.status(Status.OK).entity(results).build();
        });
    }

    @PermitAll
    @POST
    @Path("/log")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Query data from database", notes = "Query and return json object")
    public Response log(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @QueryParam("version") String version,
            @QueryParam("platform") String platform, @QueryParam("batterystatus") String batteryStatus,
            @QueryParam("batteryhealth") String batteryHealth, @QueryParam("batterylevel") String batteryLevel,
            @QueryParam("model") String model, LogParams logs) {
        return handleException(() -> {
            DataManager.getInstance().addLog(deviceId, logs.data);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });
    }
}
