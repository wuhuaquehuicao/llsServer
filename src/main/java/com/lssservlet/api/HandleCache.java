package com.lssservlet.api;

import java.util.ArrayList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.cache.CacheManager;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.rest.Secured;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/cache")
@Api(value = "Cache")
public class HandleCache extends HandleBase {
    protected static final Logger log = LogManager.getLogger(HandleCache.class);

    @PermitAll
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "reload data from database")
    public Response loadCache(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @ApiParam(value = "cacheKey", defaultValue = "country:2", required = true) @PathParam("key") String key) {
        return handleException(() -> {
            ADSData result = DataManager.getInstance().loadFromDatabase(key);
            return Response.status(Status.OK).entity(result).build();
        });
    }

    public static class CacheParams {
        public ArrayList<String> ids;
    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "reload batch data from database")
    public Response loadCaches(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @ApiParam(value = "{ids:[cacheKeys, ...]}", required = true) CacheParams params) {
        return handleException(() -> {
            ArrayList<ADSData> results = null;
            if (params.ids != null && params.ids.size() > 0) {
                results = new ArrayList<ADSData>();
                for (String id : params.ids) {
                    ADSData result = null;
                    if (id != null && id.length() > 0) {
                        result = DataManager.getInstance().loadFromDatabase(id);
                    }

                    if (result != null) {
                        results.add(result);
                    }
                }
            }

            return Response.status(Status.OK).entity(results).build();
        });
    }

    // @RolesAllowed({ ADSDbKey.SUPER_ADMIN })
    @PermitAll
    @GET
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "reset cache in redis", notes = "Reset cache in redis")
    public Response resetCache(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion) {
        CacheManager.getInstance().clear();
        return Response.status(Status.OK).entity(null).build();
    }
}
