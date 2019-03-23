package com.lssservlet.undertow;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSToken;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;

public class UserAuthentication implements AuthenticationMechanism {
    protected static final Logger log = LogManager.getLogger(UserAuthentication.class);

    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = null;
        if (request != null) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.length() == 0) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.length() == 0)
                ip = request.getRemoteAddr();
        }
        if (ip == null)
            ip = "";
        return ip;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        final ServletRequestContext servletRequestContext = exchange
                .getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest request = (HttpServletRequest) servletRequestContext.getServletRequest();
        // HttpServletResponse response = (HttpServletResponse) servletRequestContext.getServletResponse();
        String ip = getClientIpAddr(request);
        String path = exchange.getRequestPath().toLowerCase();
        UserPrincipal user = new UserPrincipal(path, exchange.getRequestHeaders(), exchange.getQueryParameters(), ip);
        if (user != null) {
            securityContext.authenticationComplete(new Account() {
                private static final long serialVersionUID = 1L;

                @Override
                public Principal getPrincipal() {
                    return user;
                }

                @Override
                public Set<String> getRoles() {
                    UserPrincipal p = (UserPrincipal) getPrincipal();
                    return p.getRoles();
                }
            }, "token", false);

            ADSToken token = user.getToken();
            if (token != null && token.getUser() != null) {
                DataManager.getInstance().updateToken(token);// ???
            }
        }

        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        return ChallengeResult.NOT_SENT;
    }

}
