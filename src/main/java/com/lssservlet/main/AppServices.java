package com.lssservlet.main;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.db.JCPersistence;
import com.lssservlet.rest.RestApplication;
import com.lssservlet.servlet.ContextAttributeListener;
import com.lssservlet.servlet.ContextListener;
import com.lssservlet.servlet.JCServlet;
import com.lssservlet.undertow.UserAuthentication;
import com.lssservlet.undertow.UserPrincipal;
import com.lssservlet.utils.TaskManager;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import io.undertow.Undertow;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ExceptionHandler;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

public class AppServices implements ExceptionHandler {
    public static AppServices _shareInstance = null;
    private static Logger log = null;
    private UndertowJaxrsServer _server;
    private boolean _httpServices;

    public AppServices() {
        _shareInstance = this;
    }

    public static class TestClass {

        public String v1;
        public String v2;
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print this usage information");
        options.addOption("s", "serverType", true, "Server type");
        options.addOption("p", "port", true, "Port");
        options.addOption("c", "conf", true, "Config file");
        options.addOption("d", "directory", true, "Config directory");
        options.addOption("id", "serverId", true, "Server Id");
        options.addOption("n", "name", true, "Server Name");
        options.addOption("r", "run", true, "Main class");
        // Parse the program arguments
        CommandLine commandLine = parser.parse(options, args);
        Config.getInstance().setCommandLine(commandLine);

        if (commandLine.hasOption("h")) {
            System.out.println("Help Message:");
            System.out.println("-s local | -s test | -s staging | -s production");
            System.out.println("-p port");
            System.out.println("-c conf");
            System.out.println("-id server id");
            System.out.println("-n server name");
            System.out.println("-r main class");
            System.exit(0);
        }
        JCPersistence.getInstance();
        AppServices services = new AppServices();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                services.stop();
            }
        });
        if (commandLine.hasOption("r")) {
            Method mainMethod = null;
            String className = commandLine.getOptionValue("r");
            if (className != null && className.length() > 0) {
                Class<?> cn = null;
                try {
                    cn = Class.forName(className);
                } catch (Exception e) {
                    if (!className.startsWith("com.lssservlet.external.")) {
                        try {
                            cn = Class.forName("com.lssservlet.external." + className);
                        } catch (Exception e1) {

                        }
                    }
                }
                if (cn != null) {
                    Method[] methods = cn.getMethods();
                    for (Method m : methods) {
                        if (m.getName().equalsIgnoreCase("main")) {
                            mainMethod = m;
                            break;
                        }
                    }
                } else {
                    System.out.println("not found " + className);
                    System.exit(0);
                }
            }
            if (mainMethod != null) {
                services.launchServer(false);
                try {
                    mainMethod.invoke(null, new Object[] { args });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            } else {
                System.out.println("no main method in " + className);
                System.exit(0);
            }
        } else
            services.launchServer(true);
    }

    public static AppServices getInstance() {
        return _shareInstance;
    }

    public static Boolean isHttpStarted() {
        return RestApplication.getInstance() != null;
    }

    public void stop() {
        log.warn("===============================STOP===============================");
        if (_server != null) {
            _server.stop();
            _server = null;
        }
        TaskManager.stop();
        DataManager.getInstance().stop();
        JCPersistence.getInstance().close();
        Config.getInstance().stop();
    }

    public void launchServer(boolean httpServices) {
        if (Config.getInstance().getHomePath() == null) {
            System.out.println("no found config file, exit");
            return;
        }
        _httpServices = httpServices;
        start();
        if (httpServices) {
            ResteasyDeployment deployment = getResteasyDeployment(Config.getInstance().getPort());
            if (deployment != null) {
                _server = new UndertowJaxrsServer();

                DeploymentInfo di = _server.undertowDeployment(deployment, "");
                di.setClassLoader(AppServices.class.getClassLoader());
                di.setContextPath("/api");
                di.setDeploymentName("Rest");

                // http://localhost:8080/api/pos
                di.addServlets(Servlets.servlet("lssservlet", JCServlet.class).addMapping("lssservlet"));

                di.addListeners(Servlets.listener(ContextListener.class),
                        Servlets.listener(ContextAttributeListener.class));
                di.addFirstAuthenticationMechanism("token", new UserAuthentication());

                di.setExceptionHandler(this);
                Set<Class<?>> endpoints = AppServices.getInstance().getWebSocketEndpoint();
                if (endpoints.size() > 0) {
                    WebSocketDeploymentInfo wsInfo = new WebSocketDeploymentInfo();
                    for (Class<?> ep : endpoints) {
                        wsInfo.addEndpoint(ep);
                    }
                    XnioWorker worker = null;
                    final Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
                    try {
                        worker = xnio.createWorker(OptionMap.builder()
                                .set(org.xnio.Options.CONNECTION_HIGH_WATER, 1000000)
                                .set(org.xnio.Options.CONNECTION_LOW_WATER, 1000000)
                                .set(org.xnio.Options.WORKER_TASK_CORE_THREADS, Config.getInstance().getWorkerThreads())
                                .set(org.xnio.Options.WORKER_TASK_MAX_THREADS, Config.getInstance().getWorkerThreads())
                                .set(org.xnio.Options.WORKER_IO_THREADS, Config.getInstance().getIoThreads())
                                .set(org.xnio.Options.TCP_NODELAY, true).set(org.xnio.Options.CORK, true).getMap());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    wsInfo.setWorker(worker);
                    di.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);
                }
                _server.deploy(di);
                _server.start(getBuilder(Config.getInstance().getHomePath(), Config.getInstance().getPort()));
            }
        }
    }

    private void start() {
        log = LogManager.getLogger(AppServices.class);
        try {
            DataManager.getInstance().start();
        } catch (Exception e) {
            log.error("DataManager start error", e);
            System.exit(0);
            return;
        }
        log.info("==============================={}===============================", Version._VER);
        if (_httpServices) {
            log.info("port:{}, type:{}, name:{}, ioThreads:{}, workerThreads:{}, path:{}",
                    Config.getInstance().getPort(), Config.getInstance()._serverType,
                    Config.getInstance().getServerName(), Config.getInstance().getIoThreads(),
                    Config.getInstance().getWorkerThreads(), Config.getInstance().getHomePath());
            log.info("==============================={}===============================", "SERVER IS READY");
        } else {
            log.info("NO HTTP services, type:{}, name:{}, path:{}", Config.getInstance()._serverType,
                    Config.getInstance().getServerName(), Config.getInstance().getHomePath());
            log.info("==============================={}===============================", "Data READY");
        }
    }

    public boolean isEnableHttpServices() {
        return _httpServices;
    }

    private Undertow.Builder getBuilder(String webroot, int port) {
        // http://undertow.io/undertow-docs/undertow-docs-1.3.0/index.html#bootstrapping-undertow
        // http://krasig.blogspot.sg/2015/01/wildfly-performane-tuning-part-1.html
        // https://github.com/undertow-io/undertow/blob/master/core/src/main/java/io/undertow/Undertow.java
        final Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
        XnioWorker worker = null;
        try {
            worker = xnio.createWorker(OptionMap.builder().set(org.xnio.Options.CONNECTION_HIGH_WATER, 1000000)
                    .set(org.xnio.Options.CONNECTION_LOW_WATER, 1000000)
                    .set(org.xnio.Options.WORKER_TASK_CORE_THREADS, Config.getInstance().getWorkerThreads())
                    .set(org.xnio.Options.WORKER_TASK_MAX_THREADS, Config.getInstance().getWorkerThreads())
                    .set(org.xnio.Options.WORKER_IO_THREADS, Config.getInstance().getIoThreads())
                    .set(org.xnio.Options.TCP_NODELAY, true).set(org.xnio.Options.CORK, true).getMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Undertow.Builder serverBuilder = null;
        if (Config.getInstance().getSSLKeyStore() != null) {
            SSLContext sslContext = createSSLContext(loadKeyStore(Config.getInstance().getSSLKeyStore()), null);
            if (sslContext != null)
                serverBuilder = Undertow.builder().addHttpsListener(port, "0.0.0.0", sslContext);
        }
        if (serverBuilder == null)
            serverBuilder = Undertow.builder().addHttpListener(port, "0.0.0.0");
        if (worker != null)
            serverBuilder.setWorker(worker);
        // final ResourceHandler resourceHandler = new ResourceHandler(new
        // FileResourceManager(new File(webroot), 100));
        // resourceHandler.setWelcomeFiles("index.html").setDirectoryListingEnabled(true);
        // serverBuilder.setHandler(resourceHandler);
        return serverBuilder;
    }

    private Set<Class<?>> getWebSocketEndpoint() {
        ScanResult scanResult = new FastClasspathScanner("com.lssservlet.ws").scan();
        List<String> pathAnnotated = scanResult
                .getNamesOfClassesWithAnnotation(javax.websocket.server.ServerEndpoint.class);
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (String c : pathAnnotated) {
            try {
                result.add(Class.forName(c));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private ResteasyDeployment getResteasyDeployment(int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            server.close();
        } catch (java.net.BindException e) {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            long pid = Long.parseLong(processName.split("@")[0]);
            System.out.println("kill old process");
            String command = "kill -9 `ps -ef | grep java | grep \"com.lssservlet.main\" | grep -v grep | grep -v "
                    + pid + "| awk '{print $2}'`";
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command }, null, null);
                int exitValue = process.waitFor();
                if (exitValue == 0) {
                    Thread.sleep(1000);
                    try {
                        ServerSocket server = new ServerSocket(port);
                        server.close();
                    } catch (java.net.BindException e1) {
                        return null;
                    }
                } else {
                    System.out.println("kill old process error:" + exitValue);
                    return null;
                }
            } catch (IOException | InterruptedException e2) {
                e2.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(com.lssservlet.rest.RestApplication.class.getName());
        // deployment.setAsyncJobServiceEnabled(true); // ?asynch=true
        ScanResult scanResult = new FastClasspathScanner("com.lssservlet").scan();
        List<String> pathAnnotated = scanResult.getNamesOfClassesWithAnnotation(javax.ws.rs.Path.class);
        for (String c : pathAnnotated) {
            deployment.getResourceClasses().add(c);
        }

        List<String> providerAnnotated = scanResult.getNamesOfClassesWithAnnotation(javax.ws.rs.ext.Provider.class);
        for (String c : providerAnnotated) {
            deployment.getProviderClasses().add(c);
        }
        deployment.setAsyncJobServiceBasePath("/jobs/" + Config.getInstance().getServerId());
        return deployment;
    }

    private KeyStore loadKeyStore(String storeLoc) {
        try {
            String storePw = "";
            InputStream stream = Files.newInputStream(Paths.get(storeLoc));
            if (stream == null) {
                throw new IllegalArgumentException("Could not load keystore");
            }
            try (InputStream is = stream) {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(is, storePw.toCharArray());
                return loadedKeystore;
            }
        } catch (Exception e) {

        }
        return null;
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) {
        try {
            KeyManager[] keyManagers = null;
            if (keyStore != null) {
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, "".toCharArray());
                keyManagers = keyManagerFactory.getKeyManagers();
            }
            TrustManager[] trustManagers = null;
            if (trustStore != null) {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return sslContext;
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public boolean handleThrowable(HttpServerExchange exchange, ServletRequest request, ServletResponse response,
            Throwable throwable) {
        UserPrincipal user = null;
        ADSToken token = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        SecurityContext sc = exchange.getSecurityContext();
        if (sc != null && sc.getAuthenticatedAccount() != null)
            user = (UserPrincipal) sc.getAuthenticatedAccount().getPrincipal();
        if (user != null) {
            token = user.getToken();
        }
        String uri = exchange.getRequestPath();
        if (user != null)
            log.error("handler internal error: " + httpRequest.getMethod() + " - " + uri
                    + ((token != null) ? token.getDisplayInfo() : "") + " " + " ip:" + user.getClientIp() + " ver:"
                    + user.getVersion(), throwable);
        else
            log.error("handler internal error:" + httpRequest.getMethod() + " - " + uri, throwable);
        return false;
    }
}
