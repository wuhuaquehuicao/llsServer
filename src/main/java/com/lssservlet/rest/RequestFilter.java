package com.lssservlet.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.lssservlet.exception.ErrorCode;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.DataException;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class RequestFilter implements ContainerRequestFilter {
    protected static final Logger log = LogManager.getLogger(RequestFilter.class);

    private boolean checkUpgrade(ContainerRequestContext requestContext, UserPrincipal user) {
        boolean ret = false;
        if (user != null) {
            // TODO: handle upgrade case
            ret = true;
        }
        return ret;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UserPrincipal user = null;
        String body = null;
        if (requestContext instanceof org.jboss.resteasy.core.interception.PostMatchContainerRequestContext) {
            PostMatchContainerRequestContext context = (PostMatchContainerRequestContext) requestContext;
            HttpRequest request = context.getHttpRequest();
            if (request instanceof org.jboss.resteasy.plugins.server.servlet.Servlet3AsyncHttpRequest) {
                SecurityContext sc = requestContext.getSecurityContext();
                user = (UserPrincipal) sc.getUserPrincipal();
                ResteasyProviderFactory.pushContext(HttpRequest.class, request);
                InputStream in = request.getInputStream();
                if (in.available() > 0) {
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    byte[] data = new byte[1024];
                    int count = -1;
                    while ((count = in.read(data, 0, 1024)) != -1)
                        outStream.write(data, 0, count);
                    data = null;
                    body = new String(outStream.toByteArray(), "UTF-8");
                    request.setInputStream(new ByteArrayInputStream(outStream.toByteArray()));
                }
                if (user != null)
                    user.setRequestBody(body);
            } else {
                logRequest(requestContext, body);
                requestContext.abortWith(
                        responseWithDataException(new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!")));
                return;
            }
        }
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext
                .getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();

        boolean ret = checkUpgrade(requestContext, user);

        // Access allowed for all
        if (method.isAnnotationPresent(PermitAll.class)) {
            logRequest(requestContext, body);
            return;
        }
        if (method.isAnnotationPresent(DenyAll.class)) {
            logRequest(requestContext, body);
            requestContext.abortWith(
                    responseWithDataException(new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!")));
            return;
        }

        Boolean match = false;
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
            match = rolesSet.size() > 0 ? false : true;
            if (user != null) {
                Set<String> roles = user.getRoles();
                for (String role : rolesSet) {
                    match = roles.contains(role);
                    if (match)
                        break;
                }
            }
            if (!match) {
                logRequest(requestContext, body);
                if (user != null && user.getRoles().size() > 0)
                    requestContext
                            .abortWith(responseWithDataException(new DataException(ErrorCode.FORBIDDEN, "Forbid")));
                else
                    requestContext.abortWith(responseWithDataException(
                            new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!")));
                return;
            }
        }

        if (!match) {
            requestContext.abortWith(
                    responseWithDataException(new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!")));
        }

        if (ret) {
            logRequest(requestContext, body);
            return;
        }

        logRequest(requestContext, body);
        requestContext.abortWith(
                responseWithDataException(new DataException(ErrorCode.UNAUTHORIZED, "Authentication failed!")));
    }

    private Response responseWithDataException(DataException dataException) {
        return Response.status(dataException.getStatus()).entity(dataException).build();
    }

    private void logRequest(ContainerRequestContext requestContext, String body) {
        // can't log password...
        try {
            String uri = requestContext.getUriInfo().getRequestUri().toString();
            UserPrincipal user = (UserPrincipal) requestContext.getSecurityContext().getUserPrincipal();
            String filterBody = user.getRequestFilterBody();
            log.info("request: {} - {} {} ip:{} ver:{} request:{} {}", requestContext.getMethod(), uri, "",
                    user.getClientIp(), user.getVersion(), user.getRequestId(),
                    (filterBody == null) ? "" : (" params:\r\n" + filterBody));
        } catch (Exception e) {
            log.error("logRequest error", e);
        }
    }
}
