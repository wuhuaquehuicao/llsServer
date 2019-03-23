package com.lssservlet.service;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.cache.CacheMap;
import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.core.HttpMethod;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

public abstract class JCBaseIntegrationService {

    private static final String THIRD_PARTY_SERVICE_ID = "3pid";

    protected static final Logger log = LogManager.getLogger(JCBaseIntegrationService.class);
    protected static final AtomicLong sRequestCount = new AtomicLong();

    public enum ThirdPartyServiceType {
        Yiftee(0);

        private int _type;

        private ThirdPartyServiceType(int type) {
            _type = type;
        }

        public int getType() {
            return _type;
        }

        public String getId() {
            switch (this) {
            case Yiftee:
                return "yiftee";
            default:
                break;
            }

            return null;
        }

        public static ThirdPartyServiceType getType(int type) {
            for (ThirdPartyServiceType serviceType : ThirdPartyServiceType.values()) {
                if (serviceType.getType() == type) {
                    return serviceType;
                }
            }

            return null;
        }
    }

    protected String handleResponse(Response response) throws HttpResponseException {
        StatusLine statusLine = null;
        HttpEntity entity = null;
        Content content = null;
        try {
            HttpResponse httpRes = response.returnResponse();
            if (httpRes != null) {
                entity = httpRes.getEntity();
                content = entity != null
                        ? new Content(EntityUtils.toByteArray(entity), ContentType.getOrDefault(entity))
                        : Content.NO_CONTENT;
                statusLine = httpRes.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() < 300 && content != null) {
                    return content.asString(Charset.forName("UTF-8"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (statusLine != null && statusLine.getStatusCode() >= 300 && content != null) {
            try {
                if (entity != null)
                    EntityUtils.consume(entity);
            } catch (IOException e) {
                e.printStackTrace();
            }

            throw new HttpResponseException(statusLine.getStatusCode(), content.asString(Charset.forName("UTF-8")));
        }

        return null;
    }

    protected JsonObject request(ThirdPartyServiceType serviceType, ContentType contentType, String apiURL,
            HttpMethod method, ArrayList<Header> headers, JsonObject postBody) throws DataException {
        Response response = null;
        String result = null;
        Request request;
        try {
            switch (method) {
            case GET:
                request = Request.Get(apiURL).setCacheControl("NO-CACHE");
                break;
            case POST:
                if (contentType == ContentType.APPLICATION_FORM_URLENCODED) {
                    BasicNameValuePair pair = new BasicNameValuePair("call_function_data", postBody.toString());
                    request = Request.Post(apiURL).bodyForm(Arrays.asList(pair), Consts.UTF_8);
                } else {
                    request = Request.Post(apiURL).bodyString(postBody.toString(), contentType);
                }
                break;
            case PUT:
                if (contentType == ContentType.APPLICATION_FORM_URLENCODED) {
                    BasicNameValuePair pair = new BasicNameValuePair("call_function_data", postBody.toString());
                    request = Request.Put(apiURL).bodyForm(Arrays.asList(pair), Consts.UTF_8);
                } else {
                    request = Request.Put(apiURL).bodyString(postBody.toString(), contentType);
                }
                break;
            case DELETE:
                request = Request.Delete(apiURL);
                break;
            default:
                throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid method: " + method.name());
            }

            if (headers != null && headers.size() > 0) {
                for (Header header : headers) {
                    request.addHeader(header);
                }
            }

            response = request.connectTimeout(Config.getInstance().getRequestTimeout())
                    .socketTimeout(Config.getInstance().getRequestTimeout()).execute();

            result = handleResponse(response);
            log.info("{} Request: count={}\r\n{}: {}\r\n{}: {}", serviceType.name(), sRequestCount.incrementAndGet(),
                    method.name(), apiURL, postBody != null ? postBody.toString() : "", result != null ? result : "");

            if (result != null && result.length() > 0) {
                JsonObject ret = new JsonObject(result);
                return ret;
            }

            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "no response");
        } catch (ClientProtocolException ex) {
            if (result == null)
                result = ex.getMessage();
            if (ex instanceof HttpResponseException) {
                HttpResponseException de = (HttpResponseException) ex;
                String message = de.getMessage();
                if (de.getStatusCode() != 404)
                    log.error("{}: count={}\r\n{}: {}\r\n{}{}", serviceType.name(), sRequestCount.incrementAndGet(),
                            method.toString(), apiURL, postBody == null ? "" : postBody.toString() + "\r\n",
                            result == null ? "no response" : result);
                if (de.getStatusCode() == 401) {
                    throw new DataException(ErrorCode.UNAUTHORIZED, message);
                }
                if (de.getStatusCode() == 400) {
                    throw new DataException(ErrorCode.BAD_REQUEST, message);
                }
                if (de.getStatusCode() == 404) {
                    throw new DataException(ErrorCode.NOT_FOUND, message);
                }
                if (de.getStatusCode() == 502) {
                    throw new DataException(ErrorCode.BAD_GATEWAY, message);
                }
            }
            throw new DataException(ErrorCode.BAD_GATEWAY, result != null ? result : "");
        } catch (Exception ex) {
            if (result == null)
                result = ex.getMessage();
            log.error("{} Request: count={}\r\n{}: {}\r\n{}{}", serviceType, sRequestCount.incrementAndGet(),
                    method.name(), apiURL, postBody == null ? "" : postBody.toString() + "\r\n",
                    result == null ? "no response" : result);
            if (ex.getCause() instanceof ConnectException) {
                ConnectException ce = (ConnectException) ex;
                if (ce != null)
                    throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR,
                            serviceType.name() + " service unavailable", ex.getMessage());
            }
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, ex);
        }
    }

    protected void resetToken(ThirdPartyServiceType type, String key) {
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(THIRD_PARTY_SERVICE_ID, type.getId());
        if (cacheMap != null) {
            cacheMap.remove(key);
        }
    }

    protected String getToken(ThirdPartyServiceType type, String key) {
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(THIRD_PARTY_SERVICE_ID, type.getId());
        if (cacheMap != null && key != null && key.length() > 0) {
            return cacheMap.get(key);
        }

        return null;
    }

    protected void setToken(ThirdPartyServiceType type, String key, String token) {
        CacheMap<String, String> cacheMap = DataManager.getInstance().getCacheMap(THIRD_PARTY_SERVICE_ID, type.getId());
        if (cacheMap != null && key != null && key.length() > 0 && token != null && token.length() > 0) {
            cacheMap.put(key, token);
        }
    }

    protected void addToPath(StringBuilder path, String key, Object value, boolean begin) {
        String content = key + "=" + value;
        if (begin) {
            path.append("?");
        } else {
            path.append("&");
        }
        path.append(content);
    }
}
