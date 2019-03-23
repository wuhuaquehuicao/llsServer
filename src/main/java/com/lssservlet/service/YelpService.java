package com.lssservlet.service;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.lssservlet.core.Config;
import com.lssservlet.core.HttpMethod;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;
import com.lssservlet.utils.ShipCoordinates;
import com.lssservlet.utils.StringUtil;

public class YelpService {
    protected static final Logger log = LogManager.getLogger(YelpService.class);
    private static final String API_HOST = "api.yelp.com";
    private static final String DEFAULT_LOCATION = "San Francisco, CA";
    private static final int SEARCH_LIMIT = 20;

    public static final String DEFAULT_CATEGORY = "restaurants";

    // 0=Best matched (default), 1=Distance, 2=Highest Rated
    public static final int SORT_TYPE_BEST_MATCH = 0;
    public static final int SORT_TYPE_DISTANCE = 1;
    public static final int SORT_TYPE_RATING = 2;

    public enum RequestType {
        EBusinessSearch("search"), EBusinessDetail("detail");
        private String _type = null;

        RequestType(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static RequestType fromString(String type) {
            for (RequestType t : RequestType.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("unknown RequestType: " + type);
        }
    }

    public enum SortType {
        EBestMatche("best_match"), EDistance("distance"), ERating("rating"), EReviewCount("review_count");
        private String _type = null;

        public static SortType fromInt(int type) {
            switch (type) {
            case 0:
                return EBestMatche;
            case 1:
                return EDistance;
            case 2:
                return ERating;
            case 3:
                return EReviewCount;
            default:
                throw new IllegalArgumentException("unknown SortType: " + type);
            }
        }

        SortType(String type) {
            _type = type;
        }

        public String getValue() {
            return _type;
        }

        public static SortType fromString(String type) {
            for (SortType t : SortType.values()) {
                if (t.getValue().equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("unknown RequestType: " + type);
        }
    }

    public JsonArray searchBusiness(String keyword, String category, Double radius, int sortType, int countLimit,
            int offset, ShipCoordinates coordinate1, ShipCoordinates coordinate2) throws Exception {
        String qureyString = null;
        JsonObject searchResultObject = null;
        JsonArray resultArray = null;

        if (!StringUtil.isBlank(category) && category.contains(",")) {
            String[] catList = category.split(",");
            ArrayList<JsonArray> searchResultList = new ArrayList<>();

            for (int i = 0; i < catList.length; i++) {
                qureyString = formatQueryParams(keyword, catList[i], radius.intValue(), sortType, countLimit, offset,
                        coordinate1, coordinate2);
                try {
                    searchResultObject = (JsonObject) getBaseRequest(RequestType.EBusinessSearch, null, qureyString);
                    JsonArray businessItemArray = searchResultObject.getJsonArray("businesses");
                    searchResultList.add(businessItemArray);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }

            resultArray = new JsonArray();
            // Filter result for all search list
            int index = 0;
            while (resultArray.size() < countLimit && searchResultList.size() > 0) {
                index = index % searchResultList.size();
                JsonArray listArray = searchResultList.get(index);

                if (listArray.size() == 0) {
                    searchResultList.remove(index);
                    continue;
                }

                JsonObject item = listArray.getJsonObject(0);
                if (!arrayContainsBusiness(resultArray, item) && !isUnwantedCategory(item)) {
                    resultArray.add(item);
                }
                listArray.remove(0);
                index++;
            }

        } else {
            qureyString = formatQueryParams(keyword, category, radius.intValue(), sortType, countLimit, offset,
                    coordinate1, coordinate2);
            searchResultObject = (JsonObject) getBaseRequest(RequestType.EBusinessSearch, null, qureyString);
            resultArray = searchResultObject.getJsonArray("businesses");
        }

        // Return result in json list
        return resultArray;
    }

    public JsonObject searchByBusinessId(String businessID) throws Exception {
        if (StringUtil.isBlank(businessID)) {
            return null;
        }
        return (JsonObject) getBaseRequest(RequestType.EBusinessDetail, businessID, null);
    }

    private String formatQueryParams(String keyword, String category, Integer radius, Integer sortType,
            Integer countLimit, Integer offset, ShipCoordinates coordinate1, ShipCoordinates coordinate2) {
        String result = "";

        if (keyword != null)
            result += ("term=" + keyword);
        if (category != null)
            result += (((result.length() != 0) ? "&" : "") + "categories=" + category);

        // if (coordinate1 != null && coordinate2 == null) {
        // request.addQuerystringParameter("ll", coordinate1.toString());
        // } else if (coordinate2 != null) {
        // String searchBounds = coordinate1 + "|" + coordinate2;
        // request.addQuerystringParameter("bounds", searchBounds);
        // }

        if (coordinate1 != null) {
            result += ("&" + "latitude=" + coordinate1.latitude);
            result += ("&" + "longitude=" + coordinate1.longitude);
        }
        if (countLimit != null)
            result += ("&" + "limit=" + countLimit);
        if (radius != null)
            result += ("&" + "radius=" + radius);
        if (sortType != null)
            result += ("&" + "sort_by=" + SortType.fromInt(sortType).getValue());
        if (offset != null)
            result += ("&" + "offset=" + offset);

        return result;
    }

    private Object getBaseRequest(RequestType type, String businessId, String queryParams) throws DataException {
        String apiKey = Config.getInstance().getYelpApiKey();// "dGVzdGluZzp0ZXN0aW5nMTIz";
        String path = "https://" + API_HOST + "/v3";

        if (type == RequestType.EBusinessSearch) {
            path += "/businesses/search?" + queryParams;
        }
        if (type == RequestType.EBusinessDetail) {
            path += "/businesses/" + businessId + "?" + queryParams;
        }
        log.info("Yelp Request: " + path);

        try {
            HttpMethod method = HttpMethod.GET;
            String resp = null;
            if (method == HttpMethod.GET) {
                resp = getResponse(Request.Get(path).addHeader("Authorization", "Bearer " + apiKey)
                        .connectTimeout(60 * 1000).socketTimeout(60 * 1000).execute());
            }
            if (resp != null) {
                JsonObject result = new JsonObject(resp);
                return result;
            }
            throw new DataException(ErrorCode.BAD_GATEWAY, "Yelp no response");
        } catch (ClientProtocolException e) {
            if (e instanceof HttpResponseException) {
                HttpResponseException de = (HttpResponseException) e;
                // if (de.getStatusCode() == 401) {
                // log.error("401 Authorized Failed:" + path + ", token: " + apiToken + ", Params: " + params);
                // throw new DataException(ErrorCode.ServerConfigError, e.getMessage());
                // }
                //
                // int code = ErrorCode.ServiceUnavailable;
                // String message = "";
                // if (type == RequestType.ECapture) {
                // code = ErrorCode.CaptureError;
                // message = "capture error: " + e.getMessage();
                // } else if (type == RequestType.EVoid) {
                // code = ErrorCode.VoidError;
                // message = "void error: " + e.getMessage();
                // } else if (type == RequestType.ERefund) {
                // code = ErrorCode.RefundError;
                // message = "refund error: " + e.getMessage();
                // } else if (type == RequestType.EAuth) {
                // code = ErrorCode.PreAuthError;
                // message = "authorization error: " + e.getMessage();
                // } else {
                // // Search and find, RequestType.EBind hasn't used any more
                // }
                // throw new DataException(code, message);
            }
            throw new DataException(ErrorCode.BAD_GATEWAY, e);
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                ConnectException ce = (ConnectException) e;
                if (ce != null)
                    throw new DataException(ErrorCode.BAD_GATEWAY, "CardConnect service unavailable", e.getMessage());
            }
            throw new DataException(ErrorCode.BAD_GATEWAY, e);
        }
    }

    private String getResponse(Response response) throws HttpResponseException {
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
                if (statusLine != null && statusLine.getStatusCode() < 300 && content != null)
                    return content.asString(Charset.forName("UTF-8"));
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

    private boolean arrayContainsBusiness(JsonArray array, JsonObject business) {
        for (Object item : array) {
            try {
                String id1 = ((JSONObject) item).getString("id");
                String id2 = business.getString("id");
                if (id1.equals(id2)) {
                    return true;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return false;
    }

    private boolean isUnwantedCategory(JsonObject item) {
        if (item == null || !item.containsKey("categories")) {
            return true;
        }
        ArrayList<String> unwantedCategories = new ArrayList<>();
        unwantedCategories.add("hotdogs");
        unwantedCategories.add("foodtrucks");
        unwantedCategories.add("foodstands");

        JsonArray itemCategories = item.getJsonArray("categories");
        try {
            for (Object object : itemCategories) {
                if (unwantedCategories.contains(((JsonObject) object).getString("alias"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return false;
    }
}
