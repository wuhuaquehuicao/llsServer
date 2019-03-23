package com.lssservlet.exception;

import javax.ws.rs.core.Response;

public class ErrorCode {
    // Bad request
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

    // Not found
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();

    // Unauthorized
    public static final int UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();

    // Forbidden
    public static final int FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();

    // Conflict
    public static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();

    // Internal server error:
    public static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    public static final int BAD_GATEWAY = Response.Status.BAD_GATEWAY.getStatusCode();

    public static final int USER_DUPLICATED_NAME = -101;
    public static final int USER_NAME_ERROR = -102;
    public static final int USER_PASSWORD_ERROR = -103;
    public static final int USER_NOT_FOUND = -104;
    public static final int USER_ROLE_ERROR = -105;

    public static final int LOCATION_NOT_FOUND_LOCATION = -201;
    public static final int LOCATION_INVALID_LOCATION = -202;

    public static final int DEVICE_INVALID_MAC = -301;
    public static final int DEVICE_DUPLICATED_MAC = -302;
    public static final int DEVICE_NOT_FOUND_DEVICE = -303;
    public static final int DEVICE_NO_LOCATION_ALLOCATED = -304;

    public static final int ADLIST_NOT_FOUND_ADLIST = -401;
    public static final int ADLIST_INVALID_ADLIST = -402;
    public static final int ADLIST_NOT_FOUND_AD = -403;
    public static final int CACHEKEY_NOT_FOUND = -20;

    public static final int EXPORT_INVALID_EXPORT = -501;

}
