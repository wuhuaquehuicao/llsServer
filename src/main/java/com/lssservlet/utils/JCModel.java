package com.lssservlet.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.lssservlet.datamodel.ADSDbKey;

/**
 * Created by ramon on 5/23/17.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JCModel {
    ADSDbKey.Type type();
}