package com.lssservlet.core;

import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonObject;

public interface PostParams {
    public JsonObject toPostData(String merchant_id) throws DataException;

    public JsonObject getExtraData(String merchant_id);
}
