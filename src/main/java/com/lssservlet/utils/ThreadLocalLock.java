package com.lssservlet.utils;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.datamodel.ADSData;

public class ThreadLocalLock {
    protected static final Logger log = LogManager.getLogger(ThreadLocalLock.class);

    public static final ThreadLocal<HashMap<String, ADSData>> _threadLocal = new ThreadLocal<HashMap<String, ADSData>>();

    static public void put(String cacheKey, ADSData data) {
        HashMap<String, ADSData> threadData = _threadLocal.get();
        if (threadData == null) {
            threadData = new HashMap<String, ADSData>();
            _threadLocal.set(threadData);
        }
        threadData.put(cacheKey, data);
    }

    static public ADSData get(String cacheKey) {
        HashMap<String, ADSData> threadData = _threadLocal.get();
        if (threadData != null) {
            return threadData.get(cacheKey);
        }
        return null;
    }

    static public void remove(String cacheKey) {
        HashMap<String, ADSData> threadData = _threadLocal.get();
        if (threadData != null) {
            threadData.remove(cacheKey);
        }
    }

    static public void clear() {
        HashMap<String, ADSData> threadData = _threadLocal.get();
        if (threadData != null) {
            threadData.clear();
        }
        _threadLocal.set(null);
    }

    static public boolean isLock() {
        return (_threadLocal.get() != null);
    }
}
