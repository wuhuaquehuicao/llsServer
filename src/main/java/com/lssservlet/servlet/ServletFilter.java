package com.lssservlet.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ServletFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // io.undertow.websockets.jsr.JsrWebSocketFilter

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // HttpServletRequest req = (HttpServletRequest) request;
        // HttpServletResponse resp = (HttpServletResponse) response;
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

}
