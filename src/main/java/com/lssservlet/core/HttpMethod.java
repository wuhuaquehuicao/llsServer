package com.lssservlet.core;

public enum HttpMethod {
    GET, PUT, POST, DELETE;

    static public HttpMethod fromString(String method) {
        if (method.equals(PUT.toString()))
            return PUT;
        return GET;
    }
}
