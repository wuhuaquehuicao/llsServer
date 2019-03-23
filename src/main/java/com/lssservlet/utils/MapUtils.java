package com.lssservlet.utils;

import java.util.Map;

public class MapUtils {
    public static void addToMap(Map<String, Object> map, String key, Object value) {
        if (map != null && key != null && key.length() > 0 && value != null) {
            map.put(key, value);
        }
    }

    public static void addToMap(Map<String, Object> map, String key, String value) {
        if (map != null && key != null && key.length() > 0 && value != null && value.length() > 0) {
            map.put(key, value);
        }
    }
}
