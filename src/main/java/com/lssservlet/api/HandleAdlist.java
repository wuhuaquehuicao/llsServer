package com.lssservlet.api;

import java.util.ArrayList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAd;
import com.lssservlet.datamodel.ADSAdlist;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.managers.AdlistManager;
import com.lssservlet.managers.DeviceManager;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.ResultFilter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/adlists")
@Api("Deal")
public class HandleAdlist extends HandleBase {
    public static class PauseParams {
        public Long time;
        public Integer duration;
    }

    public static class AdParams {
        public String name;
        public String path;
        public String media_type;
        public String label;

        public String id;
        public Long running_time;
        public ArrayList<PauseParams> pauses;
    }

    public static class AdlistParams {
        public String name;
        public Integer layout;
        public String location_id;
        public String description;
        public Long slide_interval;
        public ArrayList<AdParams> ads;
        public Integer flag;
    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all ad-list")
    public Response getAdlists(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset, @QueryParam("filter") String[] filter,
            @QueryParam("clause") String[] clauses, @QueryParam("or") String or, @QueryParam("order") String order,
            @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSAdlist> adls = null;
            int total = 0;
            if (clauses != null && clauses.length > 0 || order != null || desc != null) {
                QueryParams1 params = new QueryParams1();
                params.type = ADSDbKey.Type.EAdlist.getValue();
                if (limit != null) {
                    params.limit = limit;
                } else {
                    params.limit = LIST_LIMIT;
                }
                if (offset != null) {
                    params.offset = offset;
                } else {
                    params.offset = 0;
                }
                params.orders = new ArrayList<>();
                if (order != null)
                    params.orders.add(order);
                params.clauses = new ArrayList<>();
                for (String c : clauses)
                    params.clauses.add(c);
                params.or = (or != null) ? Boolean.parseBoolean(or) : true;
                params.desc = (desc == null || desc == 0) ? false : true;
                adls = (ArrayList<ADSAdlist>) DataManager.getInstance().queryFromDatabase(params);
                {
                    params.limit = null;
                    params.offset = null;
                    ArrayList<ADSAdlist> allAdls = (ArrayList<ADSAdlist>) DataManager.getInstance()
                            .queryFromDatabase(params);
                    total = allAdls.size();
                }
            } else {
                adls = AdlistManager.getInstance().getAdlists(false);
                total = adls.size();
                adls = ResultFilter.filter(adls, filter, (limit != null) ? limit : LIST_LIMIT,
                        (offset != null) ? offset : 0, false);
            }
            if (adls != null && adls.size() > 0) {
                response = createOkResponse(adls, total);
            } else {
                response = createNullListResponse(total);
            }
            return Response.status(Status.OK).entity(response).build();
        });

    }

    @PermitAll
    @GET
    @Path("/{adlist_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get ad-list")
    public Response getAdlist(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @PathParam("adlist_id") String adlistId) {

        return handleException(() -> {
            ADSAdlist adl = ADSAdlist.getAdlist(adlistId);
            if (adl == null)
                throw new DataException(ErrorCode.BAD_REQUEST, "Not found adlist: " + adlistId);
            return Response.status(Status.OK).entity(createOkResponse(adl)).build();
        });

    }

    @PermitAll
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Create adlist")
    public Response createAdlist(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, AdlistParams params) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSAdlist adl = AdlistManager.getInstance().createAdlist(params.name, params.layout, params.description,
                    params.slide_interval, (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(adl)).build();
        });

    }

    @PermitAll
    @PUT
    @Path("/{adlist_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Update adlist")
    public Response updateAdlist(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("adlist_id") String adlistId, AdlistParams adlistParams) {

        return handleException(() -> {
            ADSAdlist adl = AdlistManager.getInstance().updateAdlist(adlistId, adlistParams.name, adlistParams.layout,
                    adlistParams.description, adlistParams.slide_interval,
                    (adlistParams.flag != null && adlistParams.flag == 0) ? true : false);
            return Response.status(Status.OK).entity(createOkResponse(adl)).build();
        });

    }

    @PermitAll
    @DELETE
    @Path("/{adlist_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Delete adlist")
    public Response deleteAdlist(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("adlist_id") String adlistId) {

        return handleException(() -> {
            AdlistManager.getInstance().deleteAdlist(adlistId);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }

    @PermitAll
    @POST
    @Path("/{adlist_id}/runningtime")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Update adlist")
    public Response createAdlistRunningTime(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("adlist_id") String adlistId, AdlistParams adlistParams,
            @QueryParam("deviceid") String deviceId) {

        return handleException(() -> {
            AdlistManager.getInstance().createAdlistRunningTimeHistory(adlistId, adlistParams, deviceId);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }

    @PermitAll
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response getActiveAdlist(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @QueryParam("version") String version,
            @QueryParam("platform") String platform, @QueryParam("batterystatus") String batteryStatus,
            @QueryParam("batteryhealth") String batteryHealth, @QueryParam("batterylevel") String batteryLevel,
            @QueryParam("model") String model) {

        return handleException(() -> {
            String did = (deviceId != null) ? deviceId.toLowerCase() : null;
            ADSDevice device = ADSDevice.getDevice(did);
            if (device != null)
                DeviceManager.getInstance().updateDevice(device.getId(), device.name, device.location_id,
                        (batteryLevel != null) ? Integer.parseInt(batteryLevel) : null, batteryHealth, version, model,
                        null, null);
            ADSAdlist adl = AdlistManager.getInstance().getActiveAdlistByDeviceId(deviceId);// AdlistManager.getInstance().getAdlistByDeviceId(deviceId)
            return Response.status(Status.OK).entity(createOkResponse(adl)).build();
        });

    }

    @PermitAll
    @POST
    @Path("/{adlist_id}/ads")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response createAds(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @PathParam("adlist_id") String adlistId, AdlistParams adParams) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSAdlist adl = ADSAdlist.getAdlist(adlistId);
            if (adl == null)
                throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist: " + adlistId);
            adl = AdlistManager.getInstance().addAds(adlistId, adParams, (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(adl)).build();
        });

    }

    @PermitAll
    @PUT
    @Path("/{adlist_id}/ads/{ad_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response updateAd(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("adlist_id") String adlistId, @PathParam("ad_id") String adId, AdParams adParams) {

        return handleException(() -> {
            ADSAdlist adl = ADSAdlist.getAdlist(adlistId);
            if (adl == null)
                throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist: " + adlistId);
            ADSAd ad = AdlistManager.getInstance().updateAd(adId, adParams.name, adParams.path, adParams.media_type);
            return Response.status(Status.OK).entity(createOkResponse(ad)).build();
        });

    }

    @PermitAll
    @DELETE
    @Path("/{adlist_id}/ads/{ad_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response deleteAd(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("adlist_id") String adlistId, @PathParam("ad_id") String adId) {

        return handleException(() -> {
            ADSAdlist adl = ADSAdlist.getAdlist(adlistId);
            if (adl == null)
                throw new DataException(ErrorCode.ADLIST_NOT_FOUND_ADLIST, "Not found adlist: " + adlistId);
            AdlistManager.getInstance().deleteAd(adlistId, adId);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }
}
