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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
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
@Path("/v{ver}/devices")
@Api("Devices")
public class HandleDevice extends HandleBase {
    protected static final Logger log = LogManager.getLogger(HandleDevice.class);

    public static class DeviceParams {
        public String mac;
        public String name; // fb
        public String location_id;
        public String password;
        public String version;
        public Integer battery_level; // percent
        public String battery_health; // percent
        public Integer pause_value; // percent
        public String model;
    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("List device")
    public Response getDevices(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset,
            @QueryParam("filter") String[] filter, @QueryParam("clause") String[] clauses, @QueryParam("or") String or,
            @QueryParam("order") String order, @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSDevice> devices = null;
            int total = 0;
            if (clauses != null && clauses.length > 0 || order != null || desc != null) {
                QueryParams1 params = new QueryParams1();
                params.type = ADSDbKey.Type.EDevice.getValue();
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
                devices = (ArrayList<ADSDevice>) DataManager.getInstance().queryFromDatabase(params);
                {
                    params.limit = null;
                    params.offset = null;
                    ArrayList<ADSDevice> allDevices = (ArrayList<ADSDevice>) DataManager.getInstance()
                            .queryFromDatabase(params);
                    total = allDevices.size();
                }
            } else {
                devices = DeviceManager.getInstance().getDevices(false);
                total = devices.size();
                devices = ResultFilter.filter(devices, filter, (limit != null) ? limit : LIST_LIMIT,
                        (offset != null) ? offset : 0, false);
            }

            if (devices != null && devices.size() > 0) {
                response = createOkResponse(devices, total);
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
    @ApiOperation("Create device")
    public Response createDevice(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            DeviceParams params) {

        return handleException(() -> {
            ADSDevice device = DeviceManager.getInstance().createDevice(params.mac, params.name, params.location_id);
            return Response.status(Status.OK).entity(createOkResponse(device)).build();
        });

    }

    @PermitAll
    @GET
    @Path("/{device_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Create device")
    public Response getDevice(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("device_id") String deviceId) {

        return handleException(() -> {
            ADSDevice device = DeviceManager.getInstance().getDevice(deviceId);
            return Response.status(Status.OK).entity(createOkResponse(device)).build();
        });

    }

    @PermitAll
    @PUT
    @Path("/{device_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("update device")
    public Response updateDevice(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("device_id") String deviceId, DeviceParams params, @QueryParam("deviceid") String aDeviceId) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSDevice device = DeviceManager.getInstance().updateDevice(deviceId, params.name, params.location_id,
                    params.battery_level, params.battery_health, params.version, params.model, params.password,
                    (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(device)).build();
        });

    }
}
