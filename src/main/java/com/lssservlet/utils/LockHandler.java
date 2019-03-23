package com.lssservlet.utils;

public interface LockHandler<T> {

    void accept(T t, Object... args) throws DataException;
}
