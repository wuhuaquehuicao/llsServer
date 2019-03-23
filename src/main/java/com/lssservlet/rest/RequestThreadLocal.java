package com.lssservlet.rest;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestThreadLocal {
    protected static final Logger log = LogManager.getLogger(RequestThreadLocal.class);

    public enum ThreadDataType {
        EUserData, EUri;
    }

    public static final ThreadLocal<HashMap<String, Object>> threadLocal = new ThreadLocal<HashMap<String, Object>>();

    static public void put(ThreadDataType type, Object data) {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData == null) {
            threadData = new HashMap<String, Object>();
            threadLocal.set(threadData);
        }
        threadData.put(type.toString(), data);
    }

    static public Object get(ThreadDataType type) {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData != null) {
            return threadData.get(type.toString());
        }
        return null;
    }

    static public void putCacheKey(String key, Object data) {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData == null) {
            threadData = new HashMap<String, Object>();
            threadLocal.set(threadData);
        }
        threadData.put(key, data);
    }

    static public Object getCacheKey(String key) {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData != null) {
            return threadData.get(key);
        }
        return null;
    }

    static public void removeCacheKey(String key) {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData != null) {
            threadData.remove(key);
        }
    }

    static public void clear() {
        HashMap<String, Object> threadData = threadLocal.get();
        if (threadData != null) {
            threadData.clear();
        }
        threadLocal.set(null);
    }
}
