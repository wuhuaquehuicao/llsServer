package com.lssservlet.rest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSAd;
import com.lssservlet.datamodel.ADSAdlist;
import com.lssservlet.datamodel.ADSData;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSDevice;
import com.lssservlet.datamodel.ADSLocation;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.response.FFailResponse;
import com.lssservlet.rest.RequestThreadLocal.ThreadDataType;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.Json;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.ResultFilter;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ResponseFilter implements ContainerResponseFilter {
    protected static final Logger log = LogManager.getLogger(ResponseFilter.class);
    @Context
    SecurityContext _securityContext;

    static public Map<String, Object> filterData(ADSData d, Map<String, Object> data) {
        if (d == null)
            return data;
        UserPrincipal userPrincipal = (UserPrincipal) RequestThreadLocal.get(ThreadDataType.EUserData);
        String deviceId = userPrincipal.getUUID();
        Map<String, Object> newValue = new LinkedHashMap<String, Object>();
        newValue.putAll(data);
        if (d.getDataType() == Type.EAdlist) {
            ADSDevice device = ADSDevice.getDevice(deviceId);
            ADSAdlist adlist = d.model();
            String latest_version = Config.getInstance().getLatestClientVersion();
            String client_download_url = Config.getInstance().getClientDownloadUrl();
            newValue.put("ads", adlist.getAds());
            newValue.put("statics_interval", (Config.getInstance().getStaticsInterval() != null)
                    ? Config.getInstance().getStaticsInterval() : 12 * 60 * 60);// In second
            if (device != null) {
                ADSLocation location = ADSLocation.getLocation(device.location_id);
                if (location != null && location.upgrade == 1) {
                    latest_version = Config.getInstance().getLatestClientVersionInternal();
                    client_download_url = Config.getInstance().getClientDownloadUrlInternal();
                }
                if (device.password != null)
                    newValue.put("password", device.password);
                newValue.put("log", device.log);
            }
            newValue.put("latest_version", (latest_version != null) ? latest_version : "1.0.0");
            newValue.put("ads_update_interval", (Config.getInstance().getAdsUpdateInterval() != null)
                    ? Config.getInstance().getAdsUpdateInterval() : 5 * 60);
            if (client_download_url != null)
                newValue.put("client_download_url", client_download_url);
            if (deviceId != null)
                newValue.put("server_url", Config.getInstance().getServerUrl());
        } else if (d.getDataType() == Type.EUser) {
            newValue.remove("salt");
            newValue.remove("password");
        } else if (d.getDataType() == Type.ELocation) {
            ADSLocation location = d.model();
            newValue.remove("salt");
            newValue.put("devices", location.getDevices().size());
        } else if (d.getDataType() == Type.EDevice) {
            ADSDevice device = d.model();
            if (device.location_id != null) {
                ADSLocation location = ADSLocation.getLocation(device.location_id);
                if (location != null)
                    newValue.put("location_name", location.name);
            }
            // if ((DataManager.getInstance().dbtime() - device.updated_at > 600000) && device.active == 1)
            // newValue.put("active", 0);
        } else if (d.getDataType() == Type.EAd) {
            ADSAd ad = d.model();
            ADSAdlist adlist = ADSAdlist.getAdlist(ad.adlist_id);
            if (adlist != null)
                newValue.put("adlist_name", adlist.name);
        }
        return newValue;
    }

    @SuppressWarnings("unused")
    static private Map<String, Object> localize(Map<String, Object> localization, String lang) {
        lang = lang.toLowerCase();
        Object loc = null;
        if (loc == null)
            loc = localization.get(lang);
        if (loc == null) {
            String sp[] = lang.split("-");
            if (sp != null && sp.length == 2) {
                if (loc == null)
                    loc = localization.get(sp[0]);
            }
        }
        if (loc != null) {
            if (loc instanceof Map)
                return (Map) loc;
            else if (loc instanceof JsonObject)
                return ((JsonObject) loc).getMap();
        }
        return null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        try {
            if (Config.getInstance()._serverType != 1) {
                responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
                responseContext.getHeaders().add("ServerId", Config.getInstance().getServerId());
            }
            Object entity = responseContext.getEntity();
            if (entity == null) {
                // no entity
                logResponse(Level.INFO, requestContext, 200, null);
                return;
            }
            if (entity instanceof DataException) {
                DataException t = (DataException) entity;
                responseContext.setEntity(t.toString());
                responseContext.setStatus(t.getStatus());
                if (t.errorCode() != ErrorCode.UNAUTHORIZED && t.errorCode() != ErrorCode.BAD_REQUEST
                        && t.errorCode() != ErrorCode.FORBIDDEN && t.errorCode() != ErrorCode.NOT_FOUND) {
                    logResponse(Level.ERROR, requestContext, t.errorCode(), t);
                } else {
                    logResponse(Level.WARN, requestContext, t.errorCode(), t);
                }
            } else if (entity instanceof Throwable) {
                Throwable t = (Throwable) entity;
                responseContext.setStatus(500);
                responseContext.setEntity(t.getMessage());
                logResponse(Level.ERROR, requestContext, 500, t);
            } else {
                // updatecache
                String uri = requestContext.getUriInfo().getPath();
                if (!uri.toLowerCase().contains("updatecache") && !uri.toLowerCase().contains("/application/script")) {
                    RequestThreadLocal.put(ThreadDataType.EUserData, _securityContext.getUserPrincipal());
                    RequestThreadLocal.put(ThreadDataType.EUri, uri);
                }
                String ret = null;
                if (entity instanceof String) {
                    ret = (String) entity;
                    responseContext.setEntity(ret);
                } else if (entity instanceof byte[]) {
                    // png ?
                    byte[] stream = (byte[]) entity;
                    responseContext.getEntityStream().write(stream);
                } else {
                    ret = Json.encode(entity);
                    responseContext.setEntity(ret);
                }
                if (entity instanceof FFailResponse && ((FFailResponse) entity).status.equalsIgnoreCase("FAIL")) {
                    DataException t = new DataException(((FFailResponse) entity).error.code,
                            ((FFailResponse) entity).error.reason);
                    logResponse(Level.WARN, requestContext, 200, t);
                } else {
                    logResponse(Level.INFO, requestContext, 200, null);
                }
            }
        } finally {
            RequestThreadLocal.clear();
            ResultFilter.clear();
        }
    }

    private void logResponse(Level level, ContainerRequestContext requestContext, int code, Throwable t) {
        try {
            String uri = requestContext.getUriInfo().getRequestUri().toString();
            UserPrincipal user = (UserPrincipal) requestContext.getSecurityContext().getUserPrincipal();
            if (user != null) {
                String message = null;
                if (level.isMoreSpecificThan(Level.WARN)) {
                    // add body for exception, when we get error email, we can know the request body.
                    String filterBody = user.getRequestFilterBody();
                    message = "response: " + code + " - " + uri + " " + " response:" + user.getRequestId()
                            + ((t == null) ? "" : (" message:" + t.toString())) + " latency:"
                            + (DataManager.getInstance().time() - user.getRequestTimeStamp())
                            + (filterBody == null ? "" : (" params:\r\n" + filterBody));
                } else {
                    message = "response: " + code + " - " + uri + " " + " response:" + user.getRequestId()
                            + ((t == null) ? "" : (" message:" + t.toString())) + " latency:"
                            + (DataManager.getInstance().time() - user.getRequestTimeStamp());
                }
                log.log(level, message, t);
            }
        } catch (Exception e) {
            log.error("logResponse error", e);
        }
    }
}
