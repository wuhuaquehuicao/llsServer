package com.lssservlet.api;

import java.util.ArrayList;

import javax.annotation.security.PermitAll;
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
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSLocation;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.managers.LocationManager;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.ResultFilter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/locations")
@Api("Merchant")
public class HandleLocation extends HandleBase {
    public static class LocationParams {
        public String name;
        public String password;
        public String address;
        public String phone;
        public String email;
        public String contact;
        public String active_adlist_id;
        public ArrayList<String> adlist_ids;
    }

    public static class DevicesParams {
        public ArrayList<String> device_ids;
    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("List all merchant info")
    public Response getLocations(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset,
            @QueryParam("filter") String[] filter, @QueryParam("clause") String[] clauses, @QueryParam("or") String or,
            @QueryParam("order") String order, @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSLocation> locations = null;
            int total = 0;
            if (clauses != null && clauses.length > 0 || order != null || desc != null) {
                QueryParams1 params = new QueryParams1();
                params.type = ADSDbKey.Type.ELocation.getValue();
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
                locations = (ArrayList<ADSLocation>) DataManager.getInstance().queryFromDatabase(params);
                {
                    params.limit = null;
                    params.offset = null;
                    ArrayList<ADSLocation> allLocations = (ArrayList<ADSLocation>) DataManager.getInstance()
                            .queryFromDatabase(params);
                    total = allLocations.size();
                }
            } else {
                locations = LocationManager.getInstance().getLocations(false);
                // locations = DataManager.getInstance().getDataList(ADSDbKey.Type.ELocation);
                total = locations.size();
                locations = ResultFilter.filter(locations, filter, (limit != null) ? limit : LIST_LIMIT,
                        (offset != null) ? offset : 0, false);
            }
            if (locations != null && locations.size() > 0) {
                response = createOkResponse(locations, total);
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
    @ApiOperation("Get merchant info")
    public Response createLocation(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            LocationParams locParams) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSLocation location = LocationManager.getInstance().createLocation(locParams.name, locParams.password,
                    locParams.address, locParams.phone, locParams.email, locParams.contact, locParams.active_adlist_id,
                    (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(location)).build();
        });

    }

    @PermitAll
    @GET
    @Path("/{location_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get merchant info")
    public Response getLocation(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("location_id") String locationId) {

        return handleException(() -> {
            ADSLocation location = ADSLocation.getLocation(locationId);
            if (location == null) {
                throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location: " + locationId);
            }
            return Response.status(Status.OK).entity(createOkResponse(location)).build();
        });

    }

    @PermitAll
    @PUT
    @Path("/{location_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get merchant info")
    public Response updateLocation(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("location_id") String locationId, LocationParams locParams) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSLocation location = ADSLocation.getLocation(locationId);
            if (location == null) {
                throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location: " + locationId);
            }
            location = LocationManager.getInstance().updateLocation(locationId, locParams.name, locParams.address,
                    locParams.active_adlist_id, locParams.email, locParams.contact, locParams.phone, locParams.password,
                    (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(location)).build();
        });

    }

    // @PermitAll
    // @POST
    // @Path("/{location_id}/adlists/")
    // @Produces(MediaType.APPLICATION_JSON)
    // @ApiOperation("Get merchant info")
    // public Response createLocationAdlist(
    // @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
    // LocationParams locParams) {
    //
    // return handleException(() -> {
    // ADSLocation location = LocationManager.getInstance().createLocation(locParams.name, locParams.password,
    // locParams.address, locParams.phone, locParams.email, locParams.contact);
    // return Response.status(Status.OK).entity(createOkResponse(location)).build();
    // });
    //
    // }
    @PermitAll
    @POST
    @Path("/{location_id}/adddevices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get merchant info")
    public Response addDevices(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("location_id") String locationId, DevicesParams params) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSLocation location = ADSLocation.getLocation(locationId);
            if (location == null) {
                throw new DataException(ErrorCode.LOCATION_NOT_FOUND_LOCATION, "Not found location: " + locationId);
            }
            LocationManager.getInstance().addDevices(locationId, params.device_ids,
                    (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }

}
