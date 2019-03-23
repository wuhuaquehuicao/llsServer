package com.lssservlet.utils;

import com.lssservlet.core.Config;

public class DataException extends Exception {
    private static final long serialVersionUID = 1L;
    private int _errorCode = 0;
    private int _status = 0;
    private JsonObject body = new JsonObject();

    public DataException(int errorCode, String errorMessage) {
        super(errorMessage);
        init(errorCode, errorMessage, null);
    }

    public DataException(int errorCode, Throwable t) {
        super(t);
        init(errorCode, t.getMessage(), null);
    }

    public DataException(int errorCode, String errorMessage, String debugMessage) {
        super(errorMessage);
        init(errorCode, errorMessage, debugMessage);
    }

    private void init(int errorCode, String errorMessage, String debugMessage) {
        _errorCode = errorCode;
        if (errorCode >= 100)
            _status = Integer.parseInt(String.valueOf(errorCode).substring(0, 3));
        else
            _status = _errorCode;
        // body.put("status", _status);
        body.put("code", _errorCode);
        body.put("message", errorMessage);
        if (debugMessage != null && Config.getInstance()._serverType != 1) {
            body.put("debugMessage", debugMessage);
        }
    }

    public int errorCode() {
        return _errorCode;
    }

    public int getStatus() {
        return _status;
    }

    @Override
    public String toString() {
        return body.toString();
    }
}