package com.lssservlet.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;

import com.lssservlet.exception.ErrorCode;
import com.lssservlet.main.AppServices;
import com.lssservlet.main.Version;
import com.lssservlet.utils.DBUtil;
import com.lssservlet.utils.DESUtil;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.JsonArray;
import com.lssservlet.utils.JsonObject;

public class Config {
    public static Config _shareInstance = null;

    public String _defaultAccessToken = "coupadmin";
    private int _requestTimeout = 60 * 1000; // 1 min
    public int _serverType = 3;

    private String _version = Version._VER;

    private Long _serverId = null;
    private String _serverName = null;
    private String _serverUrl = null;

    private DataSource _dataSource = null;
    private DataSource _replicaDataSource = null;

    private String _launchUrl = null;
    private CommandLine _commandLine = null;
    private String _rootPath = null;
    private Properties _properties = null;

    private String _lastChanges = null;
    private Integer _ioThreads = null;
    private Integer _workerThreads = null;
    private int _port = 0;

    // private Long _tokenExpireTime = null; // 25 minutes
    private String _mailForgotpasswordTemplate = null;

    private String _redisRootKey = "felyx";
    private JsonObject _redisMasterConfig;

    private Long _3pRequestTimeout = 7200 * 1000L; // 2 hours
    private int _3pThreadPoolSize = 6;

    private String _appAPIHost = "localhost:80";

    private String _yifteeAPIHost = "";

    private String _payeezyKeyId = "";
    private String _payeezyHmacKey = "";

    private String _yelpApiKey = null;
    private String _googleApiKey = null;
    private String _facebookAppSecret = null;
    private String _facebookAccessToken = null;

    private String _awsAccessKey = null; // "AKIAINUFXZQS5VMJZXFA";
    private String _awsSecretKey = null; // "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";
    private String _awsBucket = null; // "ads-resource";
    private String _awsExportBucketKey = null;
    private String _awsCloudfrontBaseUrl = null;

    private Long _tokenExpiry = null;

    private Long _statiscInterval = null;
    private Long _adsUpdateInterval = null;
    private String _clientLatestVersion = null;
    private String _clientDownloadUrl = null;
    private Long _deviceStatusDuration = 600000l;

    private String _clientLatestVersionInternal = null;
    private String _clientDownloadUrlInternal = null;

    private JsonObject _clientModels;

    private Config() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public void addClassPath(String path) {
        try {
            File f = new File(path);
            URL u = f.toURI().toURL();
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> urlClass = URLClassLoader.class;
            Method method = urlClass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[] { u });
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static Config getInstance() {
        if (_shareInstance == null) {
            synchronized (Config.class) {
                if (_shareInstance == null) {
                    _shareInstance = new Config();
                }
            }
        }
        return _shareInstance;
    }

    public void setCommandLine(CommandLine cl) throws DataException {
        _commandLine = cl;
        reloadConfig();
    }

    public void stop() {
        if (_dataSource != null) {
            BasicDataSource bds = (BasicDataSource) _dataSource;
            try {
                bds.close();
            } catch (Exception e) {

            }
        }
        if (_replicaDataSource != null) {
            BasicDataSource bds = (BasicDataSource) _replicaDataSource;
            try {
                bds.close();
            } catch (Exception e) {

            }
        }
    }

    public String getChanges() {
        if (_lastChanges == null) {
            synchronized (Config.class) {
                if (_lastChanges == null) {
                    try {
                        File file = new File("WEB-INF/conf/changes.txt");
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        String result = "", s = "";
                        while ((s = br.readLine()) != null) {
                            result += (s + "\r\n");
                        }
                        br.close();
                        result = result.replace("https://bitbucket.org/justek-us", "");
                        _lastChanges = result;
                    } catch (Exception e) {
                    }
                    if (_lastChanges == null) {
                        try {
                            byte[] c = Files.readAllBytes(Paths.get(_rootPath + "/conf/changes.txt"));
                            String result = new String(c, Charset.forName("UTF-8"));
                            result = result.replace("https://bitbucket.org/justek-us", "");
                            _lastChanges = result;
                        } catch (Exception e) {
                        }
                    }
                    if (_lastChanges == null)
                        _lastChanges = "";
                }
            }
        }
        return _lastChanges;
    }

    public String decrypt(DESUtil desUtils, int secure, String key, String value) throws DataException {
        try {
            if (desUtils != null && secure == 1 && value != null && value.length() > 0) {
                return desUtils.base64Decrypt(value);
            }
        } catch (Exception e) {
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "fail to decrypt: " + key + "=" + value);
        }

        return value;
    }

    public void reloadConfig() throws DataException {
        String configFile = null, configPath = "conf";
        if (_commandLine != null && _commandLine.hasOption("d")) {
            configPath = _commandLine.getOptionValue("d");
        }
        String root = System.getProperty("app.server.home");
        if (root != null) {
            if (Files.exists(Paths.get(root + "/" + configPath))) {
                _rootPath = root;
            }
        }
        if (_rootPath == null) {
            URL url = AppServices.class.getProtectionDomain().getCodeSource().getLocation();
            String filePath = null;
            try {
                filePath = URLDecoder.decode(url.getPath(), "utf-8");
            } catch (Exception e) {
                System.out.println("get server path error: " + e.getMessage());
                return;
            }
            if (filePath.endsWith(".jar")) {
                filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            }
            String parentPath = null;
            String[] paths = new String[] { filePath, "src/main/webapp/WEB-INF", "WEB-INF", "classes/WEB-INF" };
            for (String p : paths) {
                Path tp = Paths.get(p);
                if (Files.exists(tp) && Files.exists(Paths.get(tp + "/" + configPath))) {
                    _rootPath = tp.toAbsolutePath().toString();
                    parentPath = tp.getParent().toAbsolutePath().toString();
                    break;
                }
            }
            if (_rootPath != null) {
                addClassPath(_rootPath);
                if (parentPath != null)
                    addClassPath(parentPath);
            }
        }
        if (_rootPath == null) {
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Not Found root path");
        }
        System.setProperty("app.server.home", _rootPath);
        if (_commandLine != null && _commandLine.hasOption("c")) {
            configFile = _commandLine.getOptionValue("c");
        }
        if (configFile == null) {
            if (_commandLine != null && _commandLine.hasOption("s")) {
                String type = _commandLine.getOptionValue("s");
                if (type != null) {
                    Path tp = Paths.get(_rootPath + "/" + configPath + "/setting-" + type + ".conf");
                    if (Files.exists(tp)) {
                        configFile = tp.toAbsolutePath().toString();
                    }
                }
            }
        }
        if (configFile == null) {
            configFile = _rootPath + "/" + configPath + "/setting.conf";
        }

        if (configFile != null) {
            if (_properties != null) {
                _properties.forEach((k, v) -> {
                    String key = (String) k;
                    System.clearProperty(key);
                });
                _properties.clear();
            }
            _properties = new Properties();
            try {
                _properties.load(new FileInputStream(configFile));
            } catch (FileNotFoundException e) {
                _properties = null;
                e.printStackTrace();
            } catch (IOException e) {
                _properties = null;
                e.printStackTrace();
            }
        }
        if (_properties == null)
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Not Found config file");

        if (_commandLine != null && _commandLine.hasOption("id")) {
            String serverIdStr = _commandLine.getOptionValue("id");
            if (serverIdStr != null) {
                _serverId = Long.parseLong(serverIdStr);
                _properties.setProperty("app.server.id", String.valueOf(_serverId));
                System.setProperty("app.server.id", String.valueOf(_serverId));
            }
        }

        if (_commandLine != null && _commandLine.hasOption("n")) {
            _serverName = _commandLine.getOptionValue("n");
            _properties.setProperty("app.server.name", _serverName);
            System.setProperty("app.server.name", _serverName);
        }

        if (_commandLine != null && _commandLine.hasOption("p")) {
            String portStr = _commandLine.getOptionValue("p");
            if (portStr != null) {
                _properties.setProperty("app.server.port", portStr);
                System.setProperty("app.server.port", portStr);
            }
        }

        if (_properties.getProperty("app.server.id") == null)
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Not Server Id");
        if (_properties.getProperty("app.server.name") == null)
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Not Server Name");
        if (_properties.getProperty("app.server.type") == null)
            throw new DataException(ErrorCode.INTERNAL_SERVER_ERROR, "Not Server Type");
        if (_properties != null && _properties.size() > 0) {
            _properties.forEach((k, v) -> {
                String key = (String) k;
                String value = (String) v;
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            });
            if (System.getProperty("app.lssservlet.log.path") == null) {
                System.setProperty("app.lssservlet.log.path",
                        _rootPath + "/logs-" + _properties.getProperty("app.server.id"));
                _properties.setProperty("app.lssservlet.log.path", System.getProperty("app.lssservlet.log.path"));
            }

            System.setProperty("app.lssservlet.build.number", _version);
            _properties.setProperty("app.lssservlet.build.number", System.getProperty("app.lssservlet.build.number"));

            _dataSource = null;
            _replicaDataSource = null;

            DBUtil dbUtil = new DBUtil(getDataSource(), false);
            JsonArray configArray = dbUtil.query("t_config", new JsonObject().put("flag", 0), 0);
            for (int i = 0; i < configArray.size(); i++) {
                JsonObject c = configArray.getJsonObject(i);
                String key = c.getString("id");
                Object value = c.getValue("value");
                if (System.getProperty(key) == null) {
                    String content = value.toString();
                    _properties.setProperty(key, content);
                    System.setProperty(key, content);
                }
            }

            if (System.getProperty(
                    org.apache.logging.log4j.core.config.ConfigurationFactory.CONFIGURATION_FILE_PROPERTY) == null)
                System.setProperty(
                        org.apache.logging.log4j.core.config.ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                        _rootPath + "/" + configPath + "/log4j2.xml");
            System.out.println("log config:" + System.getProperty(
                    org.apache.logging.log4j.core.config.ConfigurationFactory.CONFIGURATION_FILE_PROPERTY));
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

            ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                    .setConfigLocation(URI.create(System.getProperty(
                            org.apache.logging.log4j.core.config.ConfigurationFactory.CONFIGURATION_FILE_PROPERTY)));
            ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).reconfigure();

            String value = _properties.getProperty("app.server.port");
            if (value != null && value.length() > 0)
                _port = Integer.parseInt(value);

            value = _properties.getProperty("app.lssservlet.access.token");
            if (value != null && value.length() > 0) {
                _defaultAccessToken = value;
            }

            value = _properties.getProperty("app.lssservlet.request.timeout");
            if (value != null && value.length() > 0) {
                _requestTimeout = Integer.parseInt(value) * 1000;
            }

            _serverId = Long.parseLong(_properties.getProperty("app.server.id"));
            _serverName = _properties.getProperty("app.server.name");
            _serverUrl = _properties.getProperty("app.server.url");

            if (_serverId.longValue() < 1000l || _serverId.longValue() > 9999l)
                throw new IllegalArgumentException("invalid server id [1000-9999]: " + _serverId.toString());

            if (_serverName == null || _serverName.length() == 0)
                throw new IllegalArgumentException("invalid server name");

            String serverTypeN = _properties.getProperty("app.server.type");
            if (serverTypeN != null && serverTypeN.length() > 0) {
                _serverType = Integer.parseInt(serverTypeN);
            }

            value = _properties.getProperty("app.redis.rootkey");
            if (value != null && value.length() > 0) {
                _redisRootKey = value;
            }

            _mailForgotpasswordTemplate = _properties.getProperty("mail.forgotpassword.template");
            _launchUrl = _properties.getProperty("app.lssservlet.launchurl");

            value = _properties.getProperty("redis.master.config");
            if (value != null && value.length() > 0) {
                _redisMasterConfig = new JsonObject(value);
            }

            value = _properties.getProperty("app.client.models");
            if (value != null && value.length() > 0) {
                _clientModels = new JsonObject(value);
            }

            int ioThreads = Runtime.getRuntime().availableProcessors();
            value = _properties.getProperty("server.undertow.io-threads");
            if (value != null && value.length() > 0)
                _ioThreads = Integer.parseInt(value);
            else
                _ioThreads = ioThreads * 2;

            value = _properties.getProperty("server.undertow.worker-threads");
            if (value != null && value.length() > 0)
                _workerThreads = Integer.parseInt(value);
            else
                _workerThreads = ioThreads * 16;

            value = _properties.getProperty("third.party.request.timeout");
            if (value != null && value.length() > 0) {
                _3pRequestTimeout = Long.parseLong(value) * 1000;
            }

            value = _properties.getProperty("third.party.threadpool.size");
            if (value != null && value.length() > 0) {
                _3pThreadPoolSize = Integer.parseInt(value);
            }

            value = _properties.getProperty("app.api.host");
            if (value != null && value.length() > 0) {
                _appAPIHost = value;
            }

            value = _properties.getProperty("yiftee.api.host");
            if (value != null && value.length() > 0) {
                _yifteeAPIHost = value;
            }

            value = _properties.getProperty("app.payeezy.api.key");
            if (value != null && value.length() > 0) {
                _payeezyKeyId = value;
            }

            value = _properties.getProperty("app.payeezy.hmac.key");
            if (value != null && value.length() > 0) {
                _payeezyHmacKey = value;
            }

            value = _properties.getProperty("app.yelp.api.key");
            if (value != null && value.length() > 0) {
                _yelpApiKey = value;
            }

            value = _properties.getProperty("app.google.api.key");
            if (value != null && value.length() > 0) {
                _googleApiKey = value;
            }

            value = _properties.getProperty("app.facebook.app.secret");
            if (value != null && value.length() > 0) {
                _facebookAppSecret = value;
            }

            value = _properties.getProperty("app.facebook.access.token");
            if (value != null && value.length() > 0) {
                _facebookAccessToken = value;
            }

            value = _properties.getProperty("app.aws.accesskey");
            if (value != null && value.length() > 0) {
                _awsAccessKey = value;
            }

            value = _properties.getProperty("app.aws.secretkey");
            if (value != null && value.length() > 0) {
                _awsSecretKey = value;
            }

            value = _properties.getProperty("app.aws.bucket");
            if (value != null && value.length() > 0) {
                _awsBucket = value;
            }

            value = _properties.getProperty("app.aws.bucket.exportbucketkey");
            if (value != null && value.length() > 0) {
                _awsExportBucketKey = value;
            }

            value = _properties.getProperty("app.aws.cloudfront.baseurl");
            if (value != null && value.length() > 0) {
                _awsCloudfrontBaseUrl = value;
            }

            value = _properties.getProperty("app.statics.interval");
            if (value != null && value.length() > 0) {
                _statiscInterval = Long.parseLong(value);
            }

            value = _properties.getProperty("app.ads.update.interval");
            if (value != null && value.length() > 0) {
                _adsUpdateInterval = Long.parseLong(value);
            }

            value = _properties.getProperty("app.device.status.duration");
            if (value != null && value.length() > 0) {
                _deviceStatusDuration = Long.parseLong(value);
            }

            value = _properties.getProperty("app.token.expiry");
            if (value != null && value.length() > 0) {
                _tokenExpiry = Long.parseLong(value);
            }

            value = _properties.getProperty("app.client.version");
            if (value != null && value.length() > 0) {
                _clientLatestVersion = value;
            }

            value = _properties.getProperty("app.client.download.url");
            if (value != null && value.length() > 0) {
                _clientDownloadUrl = value;
            }

            value = _properties.getProperty("app.client.version.internal");
            if (value != null && value.length() > 0) {
                _clientLatestVersionInternal = value;
            }

            value = _properties.getProperty("app.client.download.url.internal");
            if (value != null && value.length() > 0) {
                _clientDownloadUrlInternal = value;
            }
        }
    }

    public String getFileContent(String path) {
        try {
            byte[] c = Files.readAllBytes(Paths.get(_rootPath + path));
            return new String(c, Charset.forName("UTF-8"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public Integer getIoThreads() {
        return _ioThreads;
    }

    public Integer getWorkerThreads() {
        return _workerThreads;
    }

    public String getSSLKeyStore() {
        return null;
    }

    public String getLogTimeZone() {
        return _properties.getProperty("app.lssservlet.log.timezone");
    }

    public int getPort() {
        if (_port > 0)
            return _port;
        if (_properties.containsKey("app.server.port"))
            return Integer.parseInt(_properties.getProperty("app.server.port"));
        else
            return 8081;
    }

    public String getHomePath() {
        return _rootPath;
    }

    public long getServerId() {
        return _serverId;
    }

    public String getFontPath() {
        if (_properties == null)
            return null;
        return _properties.getProperty("app.font.path");
    }

    public String getFontName() {
        if (_properties == null)
            return null;
        return _properties.getProperty("app.font.name");
    }

    public String getLaunchUrl() {
        return _launchUrl;
    }

    public String getServerName() {
        return _serverName != null ? _serverName : "";
    }

    public String getServerUrl() {
        return _serverUrl != null ? _serverUrl : "";
    }

    public String getEmailForgotPasswordTemplate() {
        return _mailForgotpasswordTemplate;
    }

    public int getRequestTimeout() {
        return _requestTimeout;
    }

    public String getRedisRootKey() {
        return _redisRootKey;
    }

    public JsonObject getRedisMasterConfig() {
        return _redisMasterConfig;
    }

    public JsonObject getClientModels() {
        return _clientModels;
    }

    public String getDefaultAccessToken() {
        return _defaultAccessToken;
    }

    public String getLeafAPIUrl() {
        if (_properties == null)
            return null;
        return _properties.getProperty("leaf.api.url");
    }

    public String getAPIHost() {
        return _appAPIHost;
    }

    public String getYifteeAPIHost() {
        return _yifteeAPIHost;
    }

    public Long get3pRequestTimeout() {
        return _3pRequestTimeout;
    }

    public Integer get3pThreadPoolSize() {
        return _3pThreadPoolSize;
    }

    public String getPayeezyAPIKey() {
        return _payeezyKeyId;
    }

    public String getPayeezyHmacKey() {
        return _payeezyHmacKey;
    }

    public String getGoogleApiKey() {
        return _googleApiKey;
    }

    public String getYelpApiKey() {
        return _yelpApiKey;
    }

    public String getFacebookAppSecrect() {
        return _facebookAppSecret;
    }

    public String getFacebookAccessToken() {
        return _facebookAccessToken;
    }

    public String getAwsAccessKey() {
        return _awsAccessKey;
    }

    public String getAwsSecretKey() {
        return _awsSecretKey;
    }

    public String getAwsBucket() {
        return _awsBucket;
    }

    public String getAwsExportBucketKey() {
        return _awsExportBucketKey;
    }

    public String getAwsCloudFrontBaseUrl() {
        return _awsCloudfrontBaseUrl;
    }

    public Properties getEmailProperties(String category) {
        Properties email = new Properties();
        _properties.forEach((k, v) -> {
            String key = (String) k;
            if (key.startsWith("mail.")) {
                String value = (String) v;
                email.setProperty(key, value);
            }
        });
        return email;
    }

    public DataSource getDataSource() {
        if (_dataSource == null && _properties != null) {
            synchronized (_properties) {
                if (_dataSource == null && _properties != null) {
                    BasicDataSource bds = new BasicDataSource();
                    bds.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    bds.setUsername(_properties.getProperty("mysql.username"));
                    bds.setPassword(_properties.getProperty("mysql.password"));
                    bds.setUrl(_properties.getProperty("mysql.host"));
                    bds.setInitialSize(1);
                    bds.setMaxTotal(Integer.parseInt(_properties.getProperty("mysql.maxPoolSize")));
                    bds.setMaxWaitMillis(1000);
                    bds.setMinIdle(2);
                    bds.setValidationQuery("select now()");
                    _dataSource = (DataSource) bds;
                }
            }
        }
        return _dataSource;
    }

    public DataSource getReplicaDataSource() {
        if (_replicaDataSource == null && _properties != null) {
            if (_properties.getProperty("replica.host") != null) {
                synchronized (_properties) {
                    if (_replicaDataSource == null && _properties != null
                            && _properties.getProperty("replica.host") != null) {
                        BasicDataSource bds = new BasicDataSource();
                        bds.setDriverClassName("com.mysql.cj.jdbc.Driver");
                        bds.setUsername(_properties.getProperty("replica.username"));
                        bds.setPassword(_properties.getProperty("replica.password"));
                        bds.setUrl(_properties.getProperty("replica.host"));
                        bds.setInitialSize(1);
                        bds.setMaxTotal(Integer.parseInt(_properties.getProperty("replica.maxPoolSize")));
                        bds.setMaxWaitMillis(1000);
                        bds.setMinIdle(2);
                        bds.setValidationQuery("select now()");
                        _replicaDataSource = (DataSource) bds;
                    }
                }
            } else {
                _replicaDataSource = getDataSource();
            }
        }
        return _replicaDataSource;
    }

    public Long getStaticsInterval() {
        return _statiscInterval;
    }

    public Long getAdsUpdateInterval() {
        return _adsUpdateInterval;
    }

    public Long getDeviceStatusDuration() {
        return _deviceStatusDuration;
    }

    public Long getTokenExpiry() {
        return _tokenExpiry;
    }

    public String getLatestClientVersion() {
        return _clientLatestVersion;
    }

    public String getClientDownloadUrl() {
        return _clientDownloadUrl;
    }

    public String getLatestClientVersionInternal() {
        return _clientLatestVersionInternal;
    }

    public String getClientDownloadUrlInternal() {
        return _clientDownloadUrlInternal;
    }
}
