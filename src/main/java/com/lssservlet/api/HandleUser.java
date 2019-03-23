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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSToken.TokenValue;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.managers.UserManager;
import com.lssservlet.response.FResponse;
import com.lssservlet.rest.Secured;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.Codec;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.ResultFilter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Secured
@Path("/v{ver}/users")
@Api("Deal")
public class HandleUser extends HandleBase {
    protected static final Logger log = LogManager.getLogger(HandleUser.class);

    public static class UserParams {
        public String name; // fb
        public String old_password;
        public String password;
    }

    @PermitAll
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response login(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("deviceid") String deviceId, UserParams params) {

        return handleException(() -> {
            if (params.name == null || params.name.length() == 0) {
                throw new DataException(ErrorCode.USER_NAME_ERROR, "Invalid name.");
            }
            ADSUser user = UserManager.getInstance().getUserByName(params.name);
            if (user == null)
                throw new DataException(ErrorCode.USER_NAME_ERROR, "No found name");

            String pString = Codec.hmacSha256(user.salt, params.password);
            if (user.password == null || !user.password.equals(pString))
                throw new DataException(ErrorCode.USER_PASSWORD_ERROR, "Invalid password");

            if (user.token != null) {
                ADSToken token = ADSToken.getToken(user.token);
                if (token != null)
                    token.delete(true);
            }

            TokenValue value = new TokenValue();
            value.user_id = user.id;
            user.token = UserManager.getInstance().createToken(value, user.password,
                    Config.getInstance().getTokenExpiry(), 0);
            user.updated_at = DataManager.getInstance().dbtime();
            user.update(true);
            return Response.status(Status.OK).entity(createOkResponse(user)).build();
        });

    }

    @PermitAll
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("List all merchant info")
    public Response getUsers(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset,
            @QueryParam("filter") String[] filter, @QueryParam("clause") String[] clauses, @QueryParam("or") String or,
            @QueryParam("order") String order, @QueryParam("desc") Integer desc) {

        return handleException(() -> {
            FResponse response = null;
            ArrayList<ADSUser> users = null;
            int total = 0;
            if (clauses != null && clauses.length > 0 || order != null || desc != null) {
                QueryParams1 params = new QueryParams1();
                params.type = ADSDbKey.Type.EUser.getValue();
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
                users = (ArrayList<ADSUser>) DataManager.getInstance().queryFromDatabase(params);
                {
                    params.limit = null;
                    params.offset = null;
                    ArrayList<ADSUser> allUsers = (ArrayList<ADSUser>) DataManager.getInstance()
                            .queryFromDatabase(params);
                    total = allUsers.size();
                }
            } else {
                users = UserManager.getInstance().getUsers(false);
                // locations = DataManager.getInstance().getDataList(ADSDbKey.Type.ELocation);
                total = users.size();
                users = ResultFilter.filter(users, filter, (limit != null) ? limit : LIST_LIMIT,
                        (offset != null) ? offset : 0, false);
            }
            if (users != null && users.size() > 0) {
                response = createOkResponse(users, total);
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
    public Response createUser(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            UserParams params) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            ADSUser user = UserManager.getInstance().createUser(params.name, (admin != null) ? admin.getId() : null);
            return Response.status(Status.OK).entity(createOkResponse(user)).build();
        });

    }

    @PermitAll
    @PUT
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response updateUser(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("user_id") String userId, UserParams params) {

        return handleException(() -> {
            ADSUser user = UserManager.getInstance().updateUser(userId, params.old_password, params.password);
            return Response.status(Status.OK).entity(createOkResponse(user)).build();
        });

    }

    @PermitAll
    @GET
    @Path("/{user_id}/resetpwd")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response resetUserPwd(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("user_id") String userId) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            ADSUser admin = token.getUser();

            UserManager.getInstance().resetUserPwd(userId, admin.getId());
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }

    @PermitAll
    @DELETE
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Get all deals")
    public Response deleteUser(
            @ApiParam(value = "Api version", defaultValue = "1", required = true) @PathParam("ver") int apiVersion,
            @PathParam("user_id") String userId, @QueryParam("adminid") String adminId, UserParams params) {

        return handleException(() -> {
            UserPrincipal userPricinpal = (UserPrincipal) _securityContext.getUserPrincipal();
            ADSToken token = userPricinpal.getToken();
            if (token == null)
                throw new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!");
            UserManager.getInstance().deleteUser(userId);
            return Response.status(Status.OK).entity(createOkNullResponse()).build();
        });

    }
}
