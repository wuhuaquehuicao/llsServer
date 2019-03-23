package com.lssservlet.rest;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import com.lssservlet.undertow.UserPrincipal;

/**
 * Created by ramon on 17/4/10.
 */
public class RestSecurityContext implements SecurityContext {
    private UserPrincipal _user;

    public RestSecurityContext(UserPrincipal user) {
        _user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        if (_user == null)
            return null;
        return _user;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (_user == null)
            return false;
        return _user.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
