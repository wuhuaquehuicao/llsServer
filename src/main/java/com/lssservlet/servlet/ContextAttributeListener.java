package com.lssservlet.servlet;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

public class ContextAttributeListener implements ServletContextAttributeListener {

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        // getServletContext().setAttribute("t1", "v1");
        // System.out.println("attributeAdd ");
        // System.out.println(event.getName() + ":" + event.getValue());
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        // System.out.println("attributeRemoved");
        // System.out.println(event.getName() + ":" + event.getValue());
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        // System.out.println("attributeRepaced");
        // System.out.println(event.getName() + ":" + event.getValue());
    }

}
