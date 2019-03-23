package com.lssservlet.undertow;

import java.security.Principal;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSVersion;
import com.lssservlet.managers.UserManager;
import com.lssservlet.utils.Codec;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;

import io.undertow.util.HeaderMap;

public class UserPrincipal implements Principal {
    protected static final Logger log = LogManager.getLogger(UserPrincipal.class);
    private String _tokenId = null;
    private String _uuid;
    private int _apiVersion = 0;
    private String _clientIp = null;
    private ADSDbKey.Client _client = null;
    private ADSVersion _clientVersion = null;
    private long _requestTimeStamp = 0l;
    private long _requestId = 0l;
    private String _requestBody = null;
    private Float _lat = null;
    private Float _lng = null;
    private String _lang = null;

    public UserPrincipal(String uri, HeaderMap headers, Map<String, Deque<String>> query, String ip) {
        _requestTimeStamp = DataManager.getInstance().time();
        String path = uri.toLowerCase();
        String key = "/api/v";
        if (path.startsWith(key)) {
            int spos = path.indexOf(key);
            if (spos >= 0) {
                int epos = path.indexOf("/", spos + key.length());
                if (epos > 0) {
                    String verString = path.substring(spos + key.length(), epos);
                    if (verString != null && verString.length() > 0) {
                        try {
                            _apiVersion = Integer.parseInt(verString);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        } else {
            // web socket
            key = "/api/ws/v";
            if (path.startsWith(key)) {
                int spos = path.indexOf(key);
                if (spos >= 0) {
                    int epos = path.indexOf("/", spos + key.length());
                    if (epos > 0) {
                        String verString = path.substring(spos + key.length(), epos);
                        if (verString != null && verString.length() > 0) {
                            try {
                                _apiVersion = Integer.parseInt(verString);
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        }

        String authorizationHeader = getFirst(headers, "X-Access-Token");
        if (authorizationHeader == null) {
            authorizationHeader = getFirst(query, "token");
        }
        if (authorizationHeader == null) {
            authorizationHeader = getFirst(query, "userid");
        }

        String basicAuthorization = getFirst(headers, "Authorization");
        if (basicAuthorization != null && basicAuthorization.length() > 0) {
            if (basicAuthorization.startsWith("Basic ")) {
                String auth = basicAuthorization.substring(6);
                auth = new String(Codec.Base64decode(auth));
                String[] authInfo = auth.split(":");
                if (authInfo != null && authInfo.length == 2) {
                    // String username = authInfo[0];
                    // String password = authInfo[1];
                    if (authorizationHeader == null)
                        authorizationHeader = authInfo[1];
                }
            }
        }
        String clientVersion = getFirst(headers, "VERSION");
        if (clientVersion == null) {
            clientVersion = getFirst(query, "v");
        }
        _uuid = getFirst(headers, "deviceid");
        if (_uuid == null) {
            _uuid = getFirst(query, "deviceid");
        }
        String lat = getFirst(headers, "LAT");
        if (lat == null) {
            lat = getFirst(query, "lat");
        }
        String lng = getFirst(headers, "LNG");
        if (lng == null) {
            lng = getFirst(query, "lng");
        }
        _lang = getFirst(headers, "LANG");
        if (_lang == null) {
            _lang = getFirst(query, "lang");
        }

        if (authorizationHeader != null) {
            _tokenId = authorizationHeader.trim();
        }

        _clientIp = ip;

        if (clientVersion != null) {
            String[] sp = clientVersion.split("-");
            if (sp != null && sp.length == 2) {
                _client = ADSDbKey.Client.fromString(sp[0]);
                _clientVersion = ADSVersion.createVersion(sp[1]);
            }
        }
        try {
            if (lat != null && lng != null) {
                _lat = Float.valueOf(lat);
                _lng = Float.valueOf(lng);
            }
        } catch (Exception e) {
            _lat = null;
            _lng = null;
        }
    }

    public String getFirst(Map<String, Deque<String>> query, String key) {
        Deque<String> deque = query.get(key);
        if (deque != null)
            return deque.getFirst();
        return null;
    }

    public String getFirst(HeaderMap headers, String key) {
        return headers.getFirst(key);
    }

    private String getFirst(JsonObject obj, String key) {
        JsonArray values = obj.getJsonArray(key.toLowerCase());
        if (values != null && values.size() > 0) {
            return values.getString(0);
        }
        return null;
    }

    @Override
    public String getName() {
        return _tokenId;
    }

    public long getRequestId() {
        return _requestId;
    }

    public void setRequestBody(String body) {
        _requestBody = body;
    }

    public String getRequestBody() {
        return _requestBody;
    }

    public String getRequestFilterBody() {
        String body = _requestBody;
        if (body != null) {
            body = body.trim();
            if (body.startsWith("{") && body.contains("password")) {
                JsonObject postData = new JsonObject(body);
                if (postData != null) {
                    String value = postData.getString("password");
                    if (value != null) {
                        postData.put("password", value.length());
                    }
                    value = postData.getString("oldpassword");
                    if (value != null) {
                        postData.put("oldpassword", value.length());
                    }
                    body = postData.toString();
                }
            } else if (body.startsWith("{") && body.contains("cvc")) {
                JsonObject postData = new JsonObject(body);
                if (postData != null) {
                    String value = postData.getString("cvc");
                    if (value != null) {
                        postData.put("cvc", "**" + value.substring(value.length() - 1));
                    }
                    value = postData.getString("card_number");
                    if (value != null) {
                        String last4 = "***" + value.substring(value.length() - 4);
                        postData.put("card_number", last4);
                    }
                    value = postData.getString("expiry_date");
                    if (value != null) {
                        String expiry = "***" + value.substring(value.length() - 1);
                        postData.put("expiry_date", expiry);
                    }
                    body = postData.toString();
                }
            }
        }
        return body;
    }

    public long getRequestTimeStamp() {
        return _requestTimeStamp;
    }

    public Set<String> getRoles() {
        ADSToken token = ADSToken.getToken(_tokenId);
        Set<String> roles = new HashSet<>();
        if (token != null) {
            HashSet<String> roleSet = null;// token.getRoles();
            if (roleSet != null && !roleSet.isEmpty()) {
                roles.addAll(roleSet);
            }

            return roles;
        }
        return roles;
    }

    public String getVersion() {
        if (_client != null && _clientVersion != null)
            return _client.getValue() + "-" + _clientVersion.toString();
        return "";
    }

    public ADSToken getToken() {
        return UserManager.getInstance().getToken(_tokenId);
    }

    public String getUUID() {
        return _uuid;
    }

    public String getClientIp() {
        return _clientIp;
    }

    public Float getLat() {
        return _lat;
    }

    public Float getLng() {
        return _lng;
    }

    public String getLang() {
        return _lang;
    }

    public int getApiVersion() {
        return _apiVersion;
    }

    public ADSVersion getClientVersion() {
        return _clientVersion;
    }

    public ADSDbKey.Client getClientType() {
        return _client;
    }

    public String getTokenId() {
        return _tokenId;
    }
}
