package com.lssservlet.api;

import java.util.ArrayList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAd;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.managers.AdlistManager;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.utils.ResultFilter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/ads")
@Api("Deal")
public class HandleAd extends HandleBase {
    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response getAds(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset, @QueryParam("filter") String[] filter,
            @QueryParam("clause") String[] clauses, @QueryParam("or") String or, @QueryParam("order") String order,
            @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSAd> ads = null;
            int total = 0;
            if (clauses != null && clauses.length > 0 || order != null || desc != null) {
                QueryParams1 params = new QueryParams1();
                params.type = ADSDbKey.Type.EAd.getValue();
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
                ads = (ArrayList<ADSAd>) DataManager.getInstance().queryFromDatabase(params);
                {
                    params.limit = null;
                    params.offset = null;
                    ArrayList<ADSAd> allAds = (ArrayList<ADSAd>) DataManager.getInstance().queryFromDatabase(params);
                    total = allAds.size();
                }
            } else {
                ads = DataManager.getInstance().getDataList(ADSDbKey.Type.EAd);// AdlistManager.getInstance().getAdlists(false);
                total = ads.size();
                ads = ResultFilter.filter(ads, filter, (limit != null) ? limit : 10, (offset != null) ? offset : 0,
                        false);
            }
            if (ads != null && ads.size() > 0) {
                response = createOkResponse(ads, total);
            } else {
                response = createNullListResponse(total);
            }
            return Response.status(Status.OK).entity(response).build();
        });

    }

    @PermitAll
    @GET
    @Path("/statics")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response getAdStatics(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset, @QueryParam("from") Long from, @QueryParam("to") Long to,
            @QueryParam("order") String order, @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSAd> ads = AdlistManager.getInstance().getAdStatics(from, to, order, desc);// AdlistManager.getInstance().getAdlists(false);
            int total = ads.size();
            ads = ResultFilter.filter(ads, null, (limit != null) ? limit : LIST_LIMIT, (offset != null) ? offset : 0,
                    false);

            if (ads != null && ads.size() > 0) {
                response = createOkResponse(ads, total);
            } else {
                response = createNullListResponse(total);
            }
            return Response.status(Status.OK).entity(response).build();
        });

    }
}
