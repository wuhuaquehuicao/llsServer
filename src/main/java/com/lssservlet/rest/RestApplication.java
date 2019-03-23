package com.lssservlet.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.lssservlet.core.Config;

import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/api")
public class RestApplication extends Application {
    public static RestApplication _shareInstance = null;
    private static final boolean ALLOW_SCAN = true;

    public RestApplication() {
        BeanConfig sc = new BeanConfig();
        sc.setSchemes(new String[] { "http" });
        sc.setTitle("JC API document");
        sc.setVersion("1.0.0");
        sc.setHost(Config.getInstance().getAPIHost());
        sc.setBasePath("/api");
        sc.setResourcePackage("com.lssservlet.api");

        if (ALLOW_SCAN) {
            sc.setScan(true);
        }
    }

    public static RestApplication getInstance() {
        if (_shareInstance == null) {
            synchronized (RestApplication.class) {
                if (_shareInstance == null) {
                    _shareInstance = new RestApplication();
                }
            }
        }
        return _shareInstance;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

        return resources;
    }
}
