package com.bookie.infra;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class JettyBootstrap {

    private final Server server;
    private final AnnotationConfigWebApplicationContext context;

    private JettyBootstrap(Server server, AnnotationConfigWebApplicationContext context) {
        this.server = server;
        this.context = context;
    }

    public static JettyBootstrap start() throws Exception {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(TestAppConfig.class, TestWebMvcConfig.class);

        DispatcherServlet servlet = new DispatcherServlet(context);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.addServlet(new ServletHolder(servlet), "/*");

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();

        return new JettyBootstrap(server, context);
    }

    public int getPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    public <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    public void stop() throws Exception {
        server.stop();
        context.close();
    }
}
