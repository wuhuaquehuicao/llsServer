#!/bin/bash
WORKSPACE=$(cd "$(dirname "$0")"; pwd)
cd $WORKSPACE
echo $WORKSPACE

CONTAINER_HOME=~/Containers

echo "#GENERATE BY build.sh" > $WORKSPACE/.env
echo "CONTAINER_HOME=${CONTAINER_HOME}" > $WORKSPACE/.env
echo "MYSQL_IMAGE=mysql:5.7.17" >> $WORKSPACE/.env
echo "PHP_IMAGE=php:5.6.29-fpm-alpine" >> $WORKSPACE/.env
echo "OPENRESTY_IMAGE=openresty/openresty:alpine" >> $WORKSPACE/.env
echo "OPENJDK_IMAGE=openjdk:8u121-alpine" >> $WORKSPACE/.env
echo "TOMCAT_IMAGE=tomcat:8.5.5-alpine" >> $WORKSPACE/.env
echo "JENKINS_IMAGE=jenkins:2.60.3-alpine" >> $WORKSPACE/.env
echo "REDIS_IMAGE=redis:3.2.8-alpine" >> $WORKSPACE/.env
echo "RABBITMQ_IMAGE=rabbitmq:3.6.9-management-alpine" >> $WORKSPACE/.env

mkdir -p ${CONTAINER_HOME}/openresty/lualib
mkdir -p ${CONTAINER_HOME}/openresty/lua
mkdir -p ${CONTAINER_HOME}/openresty/logs
mkdir -p ${CONTAINER_HOME}/html
mkdir -p ${CONTAINER_HOME}/mysql
mkdir -p ${CONTAINER_HOME}/php/conf
mkdir -p ${CONTAINER_HOME}/php/extensions
mkdir -p ${CONTAINER_HOME}/projects/felyxservlet
mkdir -p ${CONTAINER_HOME}/tomcat/conf
mkdir -p ${CONTAINER_HOME}/tomcat/logs
mkdir -p ${CONTAINER_HOME}/tomcat/webapps
mkdir -p ${CONTAINER_HOME}/tomcat/ROOT/
mkdir -p ${CONTAINER_HOME}/tomcat/logs/portal
mkdir -p ${CONTAINER_HOME}/redis
mkdir -p ${CONTAINER_HOME}/rabbitmq

if [ "`uname`" = "Darwin" ]; then
LOCAL_IP=$(ifconfig | grep broadcast |awk -F: '{print $1}' |awk '{print $2}')
PUBLIC_IP=${LOCAL_IP}
echo PUBLIC_IP=${LOCAL_IP} >> $WORKSPACE/.env
echo LOCAL_IP=${LOCAL_IP} >> $WORKSPACE/.env
else
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
LOCAL_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)
echo PUBLIC_IP=${PUBLIC_IP} >> $WORKSPACE/.env
echo LOCAL_IP=${LOCAL_IP} >> $WORKSPACE/.env
fi

if [ ! -f ${CONTAINER_HOME}/redis/redis.conf ]; then
echo 'bind 0.0.0.0
protected-mode no
port 6379
tcp-backlog 511
timeout 0
tcp-keepalive 300
daemonize no
supervised no
pidfile /var/run/redis_6379.pid
loglevel notice
logfile ""
databases 16
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir ./
slave-serve-stale-data yes
slave-read-only yes
repl-diskless-sync no
repl-diskless-sync-delay 5
repl-disable-tcp-nodelay no
slave-priority 100
appendonly no
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-load-truncated yes
lua-time-limit 5000
slowlog-log-slower-than 10000
slowlog-max-len 128
latency-monitor-threshold 0
notify-keyspace-events ""
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
list-compress-depth 0
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
hll-sparse-max-bytes 3000
activerehashing yes
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit slave 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60
hz 10
aof-rewrite-incremental-fsync yes
' > ${CONTAINER_HOME}/redis/redis.conf
fi

if [ ! -f ${CONTAINER_HOME}/php/conf/php.ini ]; then
echo '[PHP]
post_max_size = 10M
date.timezone = "Asia/Hong_Kong"
;date.timezone = "America/Los_Angeles"
' > ${CONTAINER_HOME}/php/conf/php.ini
fi

if [ ! -f ${CONTAINER_HOME}/php/conf/php-fpm.conf ]; then
echo '[global]
error_log = /proc/self/fd/2
daemonize = no
;  include=etc/php-fpm.d/*.conf

[www]
user = www-data
group = www-data
listen = 0.0.0.0:9000
; listen = [::]:9000
pm = dynamic
pm.max_children = 50
pm.start_servers = 10
pm.min_spare_servers = 10
pm.max_spare_servers = 30
; if we send this to /proc/self/fd/1, it never appears
access.log = /proc/self/fd/2
clear_env = no
; Ensure worker stdout and stderr are sent to the main error log.
catch_workers_output = yes
' > ${CONTAINER_HOME}/php/conf/php-fpm.conf
fi

if [ ! -f ${CONTAINER_HOME}/tomcat/conf/portal.xml ]; then
echo '<?xml version="1.0" encoding="UTF-8"?>
  <Context crossContext="true">
    <WatchedResource>WEB-INF/web.xml</WatchedResource>
   <Resource name="jdbc/pos2" auth="Container"
        type="javax.sql.DataSource"
        maxTotal="20" 
        maxIdle="10"
        maxWaitMillis="10000"
        username="root" 
        password="Justekpwd" 
        validationQuery="select 1"
        poolPreparedStatements="true"
        driverClassName="com.mysql.jdbc.Driver"
        url="jdbc:mysql://mysql/pos2?useSSL=false" />
        
   <Resource name="jdbc/pos2_portal" auth="Container"
        type="javax.sql.DataSource"
        maxTotal="20" 
        maxIdle="10"
        maxWaitMillis="10000"
        username="root" 
        password="Justekpwd" 
        validationQuery="select 1"
        poolPreparedStatements="true"
        driverClassName="com.mysql.jdbc.Driver"
        url="jdbc:mysql://mysql/pos2_portal?useSSL=false"/>
                
    <Environment name="apiserver.host" type="java.lang.String" value="http://p1000" />
    <Environment name="hiccup.event.manager.key" type="java.lang.String" value="a134a37d-0fdd-4fa2-b6d1-e217cc69cb51" />
</Context>
' > ${CONTAINER_HOME}/tomcat/conf/portal.xml
fi

if [ ! -f ${CONTAINER_HOME}/tomcat/conf/server.xml ]; then
echo '<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>
  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
    <Engine name="Catalina" defaultHost="localhost">
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
        <Valve className="org.apache.catalina.valves.RemoteIpValve" emoteIpHeader="X-Forwarded-For" 
               protocolHeader="X-Forwarded-Proto" protocolHeaderHttpsValue="https" />
        <!--<Context docBase="portal" path="/" reloadable="true"/>-->
      </Host>
    </Engine>
  </Service>
</Server>
' > ${CONTAINER_HOME}/tomcat/conf/server.xml
fi

if [ ! -f ${CONTAINER_HOME}/projects/felyxservlet/run.sh ]; then
echo '#!/bin/sh
sysctl -p /etc/sysctl.d/00-alpine.conf
id=1000

if [ -n "$1" ]; then
	if [ $1 -ge 1000 -a $1 -le 9999 ]; then
		id=$1
        echo "set id to ${id}"
    else 
        echo "invalid id $1"
        exit 1
	fi
else
    echo "default id ${id}"
fi

for file in $(ls ./*.jar -t)
do
JAR=$file
break
done
echo $JAR

conf_file=
if [[ -f "p.conf" ]]; then
    conf_file="-c p.conf"
fi

JAVA_OPTS="-Djava.awt.headless=true -Xms512m -Xmx1024m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseTLAB -XX:NewSize=128m -XX:MaxNewSize=128m -XX:MaxTenuringThreshold=0 -XX:SurvivorRatio=1024 -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=60 -XX:+DisableExplicitGC"
exec /usr/bin/java ${JAVA_OPTS} -jar $JAR -n p${id} -id ${id} ${conf_file}
' > ${CONTAINER_HOME}/projects/felyxservlet/run.sh
fi

if [ ! -f ${CONTAINER_HOME}/openresty/lua/init.lua ]; then
echo 'function string:split(sep)  
    local sep, fields = sep or ",", {}  
    local pattern = string.format("([^%s]+)", sep)  
    self:gsub(pattern, function(c) fields[#fields+1] = c end)  
    return fields  
end 
' > ${CONTAINER_HOME}/openresty/lua/init.lua
fi

if [ ! -f ${CONTAINER_HOME}/openresty/lua/init_worker.lua ]; then
echo 'local delay = 10  -- in seconds
local new_timer = ngx.timer.at
local shared = ngx.shared
local json = require "cjson"
local check

check = function(premature)
    if not premature then
        local di = require("dockerinfo");
        local result=di.getInfo();
        if result then
            local ok, err = shared.shareData:set("hosts", json.encode(result))
            if not ok then
                ngx.log(ngx.INFO,"set hosts error:", err);
            end
        else
            ngx.log(ngx.INFO,"no hosts");
        end
        local ok, err = new_timer(delay, check)
        if not ok then
            ngx.log(ngx.ERR, "failed to create timer: ", err)
            return
        end
    end
end

if 0 == ngx.worker.id() then
    local ok, err = new_timer(1, check)
    if not ok then
        ngx.log(ngx.ERR, "failed to create timer: ", err)
        return
    end
end
' > ${CONTAINER_HOME}/openresty/lua/init_worker.lua
fi

if [ ! -f ${CONTAINER_HOME}/openresty/lua/dockerinfo.lua ]; then
echo 'module(..., package.seeall);
local json = require "cjson"
local http = require "resty.http"

function getInfo()    
    local httpc = http.new()
    httpc:connect("unix:///var/run/docker.sock")
    local res, err = httpc:request{
        method = "GET",
        path = "/v1.24/containers/json",
        headers = {
            ["Host"] = "localhost"
        }
    }
    if not res then
        ngx.log(ngx.DEBUG,"failed to request: ", err)
        return nil
    end

    local result={}
    if res.status >= 200 and res.status < 300 then
        local reader = res.body_reader
        local body = ""
        repeat
            local chunk, err = reader(8192)
            if err then
                ngx.log(ngx.ERR, err)
                break
            end

            if chunk then
                body=body..chunk
            end
        until not chunk
        --ngx.log(ngx.INFO,body);
        docker = json.decode(body)
        if docker then
            for k, v in pairs(docker) do
                if v and v["Names"] then
                    local name = string.sub(v["Names"][1],2);
                    local net = v["NetworkSettings"]["Networks"]
                    local ip=""
                    for knet, vnet in pairs(net) do
                        ip=vnet["IPAddress"]
                    end
                    result[name]={}
                    result[name]["ip"]=ip
                    result[name]["state"]=v["State"]
                    result[name]["status"]=v["Status"]
                end 
            end  
        end
    else
        log(ngx.DEBUG,"Query returned a non-200 response: " .. res.status)
    end
    return result
end
' > ${CONTAINER_HOME}/openresty/lua/dockerinfo.lua
fi

if [ ! -f ${CONTAINER_HOME}/openresty/htpasswd ]; then
printf "admin:$(openssl passwd -1 Justekpwd)\n" > ${CONTAINER_HOME}/openresty/htpasswd
fi

if [ ! -f ${CONTAINER_HOME}/openresty/nginx.conf ]; then
echo 'user  root;
worker_processes  1;
error_log  /usr/local/openresty/nginx/logs/error.log warn;
events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;
    #gzip  on;
    server_tokens off;
    client_max_body_size 8M;

    lua_package_path "/usr/local/openresty/nginx/lua/?.lua;;";
    lua_shared_dict shareData 1m;
    init_by_lua_file lua/init.lua;
    init_worker_by_lua_file lua/init_worker.lua;

    map $http_upgrade $connection_upgrade {
        default upgrade;
        ""      close;
    }

    upstream backend{  
        server 0.0.0.0;  
        balancer_by_lua_block {  
            local balancer = require "ngx.balancer"   
            local json = require "cjson"
            local containers = ngx.var.container_name:split(",")
            local ips={}
            local i=1
            local ip = nil
            local hosts = json.decode(ngx.shared.shareData:get("hosts"));
            if hosts then
                for k, v in pairs(containers) do
                    local hostInfo = hosts[v]
                    if hostInfo then
                        ips[i] = hostInfo["ip"]
                        i = i+1
                    end
                end
                if table.getn(ips) > 0 then
                    --local key = ngx.var.remote_addr.."-"..ngx.var.server_port  
                    --local hash = ngx.crc32_long(key)
                    --hash = (hash % table.getn(ips)) + 1  
                    local hash = (os.time() % table.getn(ips)) + 1  
                    ip = ips[hash]
                end
                if not ip then
                    ngx.log(ngx.ERR, "no ip for : ", ngx.var.container_name)  
                    return ngx.exit(404)  
                end
                ngx.log(ngx.INFO, "proxy_pass ip:",ip," port:",ngx.var.container_port)  
                local ok, err = balancer.set_current_peer(ip, ngx.var.container_port)  
                if not ok then  
                    ngx.log(ngx.ERR, "failed to set the current peer: ", err)  
                    return ngx.exit(500)  
                end  
            else
                ngx.log(ngx.ERR, "no hosts")  
                return ngx.exit(404)  
            end
        }
    }

    server {
        listen 80;
        listen 443 ssl;

        ssl_certificate /usr/local/openresty/nginx/server.crt;
        ssl_certificate_key /usr/local/openresty/nginx/server.key;

        lua_code_cache on;
        server_name  localhost;
        if ($server_port = 80) { 
            return 301 https://$server_name$request_uri; 
        } 
        if ($scheme = http) { 
            return 301 https://$server_name$request_uri; 
        } 
        proxy_redirect http:// $scheme://;
        port_in_redirect on;
        error_page 497 https://$server_name$request_uri; 

        error_page 500 502 503 504  /50x.html;
        location = /50x.html {
            root   /var/www/html;
        }

        location / {
            root   /var/www/html;
            index  index.html index.htm index.php;
        }

        location ^~ /docker/ {
            default_type text/html;
            content_by_lua_block {
                local di = require("dockerinfo");
                local result = di.getInfo();
                if result then
                    for k, v in pairs(result) do
                        ngx.say(k.." ip:"..v["ip"].." state:"..v["state"].." status:"..v["status"].."<br>")
                    end
                end
            }
        }

        location ^~ /api/ {
            set $container_name "p1000,p1002";
            set $container_port 8081;
            proxy_pass http://backend/api/;
            proxy_set_header Host $host:$server_port;
            #proxy_set_header X-Real-IP $remote_addr;
            #proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; 
	        proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
        }

        location ^~ /portal/ {
            set $container_name "tomcat";
            set $container_port 8080;
            proxy_pass http://backend/portal/;
            sendfile off;
            proxy_set_header Host $host:$server_port;
            #proxy_set_header X-Real-IP $remote_addr;
            #proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; 
            proxy_set_header X-Forwarded-Proto  $scheme;
        }

        location ^~ /jenkins/ {
            set $container_name "jenkins";
            set $container_port 8080;
            proxy_pass http://backend/jenkins/;
            sendfile off;
            proxy_set_header Host $host:$server_port;
            #proxy_set_header X-Real-IP $remote_addr;
            #proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; 
            proxy_max_temp_file_size 0;
        }

#        location ~* /rabbit/api/(.*?)/(.*) {
#            proxy_pass http://rabbit:15672/api/$1/%2F/$2;
#        }

#        location ~* /rabbit/(.*) {
#            rewrite ^/rabbit/(.*)$ /$1 break;
#            proxy_pass http://rabbit:15672;
#        }

        location /log/ {
            auth_basic "Log";
            #printf "admin:$(openssl passwd -1 Justekpwd)\n" >> ./htpasswd
            #/usr/local/openresty/nginx/conf/htpasswd
            auth_basic_user_file htpasswd; 
            root   /var/www/html;
            index  index.html index.htm index.php;
        }

        location ~ \.php$ {
            root /var/www/html;
            set $container_name "php";
            set $container_port 9000;
            fastcgi_pass   backend;
            fastcgi_index  index.php;
            fastcgi_param  SCRIPT_FILENAME  $document_root$fastcgi_script_name;
            include        fastcgi_params;
        }

        location ~ /\.ht {
            deny  all;
        }
    }
}' > ${CONTAINER_HOME}/openresty/nginx.conf
fi

if [ ! -f "${CONTAINER_HOME}/openresty/server.crt" ]; then
openssl genrsa -des3 -out ${CONTAINER_HOME}/openresty/server.key.org 1024
openssl req -new -key ${CONTAINER_HOME}/openresty/server.key.org -out ${CONTAINER_HOME}/openresty/server.csr
openssl rsa -in ${CONTAINER_HOME}/openresty/server.key.org -out ${CONTAINER_HOME}/openresty/server.key
openssl x509 -req -days 3650 -in ${CONTAINER_HOME}/openresty/server.csr -signkey ${CONTAINER_HOME}/openresty/server.key -out ${CONTAINER_HOME}/openresty/server.crt
fi 

echo 'version: "3"
services:
  mysql:
    image: ${MYSQL_IMAGE}
    container_name: mysql
    volumes:
      - ${CONTAINER_HOME}/mysql:/var/lib/mysql
    environment:
      - MYSQL_ROOT_PASSWORD=Justekpwd
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
    ports:
      - "8086:3306"

  php:
    image: ${PHP_IMAGE}
    container_name: php
    volumes:
      - ${CONTAINER_HOME}/html:/var/www/html 
      - ${CONTAINER_HOME}/php/conf/php-fpm.conf:/usr/local/etc/php-fpm.conf 
      - ${CONTAINER_HOME}/php/conf:/usr/local/etc/php/conf.d 
      - ${CONTAINER_HOME}/php/extensions:/usr/local/lib/php/extensions/no-debug-non-zts-20131226

  tomcat:
    image: ${TOMCAT_IMAGE}
    container_name: tomcat
    volumes:
      - ${CONTAINER_HOME}/tomcat/conf/portal.xml:/usr/local/tomcat/conf/Catalina/localhost/portal.xml
      - ${CONTAINER_HOME}/tomcat/conf/server.xml:/usr/local/tomcat/conf/server.xml
      - ${CONTAINER_HOME}/tomcat/webapps:/usr/local/tomcat/webapps
      - ${CONTAINER_HOME}/tomcat/logs:/usr/local/tomcat/logs
      - ${CONTAINER_HOME}/tomcat/ROOT/:/usr/local/tomcat/webapps/ROOT

  jenkins:
    image: ${JENKINS_IMAGE}
    container_name: jenkins
    environment:
      - JAVA_OPTS=-Xmx1024m
      - JENKINS_OPTS=--prefix=/jenkins
    volumes:
      - ${CONTAINER_HOME}/jenkins:/var/jenkins_home
      - ${CONTAINER_HOME}/projects:/var/jenkins_home/projects
      - ${CONTAINER_HOME}/tomcat/webapps:/var/jenkins_home/tomcat
      - /var/run/docker.sock:/var/jenkins_home/docker.sock

  openresty:
    image: ${OPENRESTY_IMAGE}
    container_name: openresty
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ${CONTAINER_HOME}/openresty/nginx.conf:/usr/local/openresty/nginx/conf/nginx.conf
      - ${CONTAINER_HOME}/openresty/lua:/usr/local/openresty/nginx/lua
      - ${CONTAINER_HOME}/openresty/lualib:/usr/local/openresty/site/lualib
      - ${CONTAINER_HOME}/openresty/server.crt:/usr/local/openresty/nginx/server.crt
      - ${CONTAINER_HOME}/openresty/server.key:/usr/local/openresty/nginx/server.key
      - ${CONTAINER_HOME}/openresty/logs:/usr/local/openresty/nginx/logs
      - ${CONTAINER_HOME}/openresty/htpasswd:/usr/local/openresty/nginx/conf/htpasswd
      - ${CONTAINER_HOME}/html:/var/www/html 

  p1000:
    privileged: true
    image: ${OPENJDK_IMAGE}
    container_name: p1000
    working_dir: /root/projects
    command: sh run.sh 1000

    logging:
      driver: "json-file"
      options:
        max-size: "200k"
        max-file: "10"

    sysctls:
      - net.core.somaxconn=262144
      - net.ipv4.ip_local_port_range=1024 65000

    ulimits:
      nproc: 65535
      nofile:
        soft: 40000
        hard: 40000

    volumes:
      - ${CONTAINER_HOME}/projects/felyxservlet:/root/projects

  p1002:
    privileged: true
    image: ${OPENJDK_IMAGE}
    container_name: p1002
    working_dir: /root/projects
    command: sh run.sh 1002

    logging:
      driver: "json-file"
      options:
        max-size: "200k"
        max-file: "10"

    sysctls:
      - net.core.somaxconn=262144
      - net.ipv4.ip_local_port_range=1024 65000

    ulimits:
      nproc: 65535
      nofile:
        soft: 40000
        hard: 40000

    volumes:
      - ${CONTAINER_HOME}/projects/felyxservlet:/root/projects

  redis:
    privileged: true
    image: ${REDIS_IMAGE}
    container_name: redis
    command: sh -c "echo never > /sys/kernel/mm/transparent_hugepage/enabled && redis-server /data/redis.conf"
    sysctls:
      - net.core.somaxconn=262144
    ports:
      - "6379:6379"

    ulimits:
      nproc: 65535
      nofile:
        soft: 40000
        hard: 40000

    volumes:
      - ${CONTAINER_HOME}/redis:/data
      - ${CONTAINER_HOME}/redis/redis.conf:/data/redis.conf

  rabbitmq:
    image: ${RABBITMQ_IMAGE}
    container_name: rabbit
    command: sh -c "docker-entrypoint.sh rabbitmq-server && rabbitmqctl set_permissions -p / admin \".*\" \".*\" \".*\""
    working_dir: /var/lib/rabbitmq
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=Justekpwd
    ports:
      - "5672:5672"
      - "15672:15672"
      - "15674:15674"
    volumes:
      - ${CONTAINER_HOME}/rabbitmq:/var/lib/rabbitmq
' > $WORKSPACE/docker-compose.yml

docker-compose config

exit 1
#docker-compose scale jenkins=2
#docker-compose build jenkins
#docker-compose up --no-deps -d jenkins
#docker-compose up -d mysql php openresty
#docker-compose up -d 
#docker-compose stop
#docker-compose rm
docker-compose ps

if [ ! -f "${CONTAINER_HOME}/openresty/lualib/resty/http.lua" ]; then
docker exec -it openresty apk add --no-cache perl
docker exec -it openresty apk add --no-cache curl
docker exec -it openresty opm get agentzh/lua-resty-http
fi

#docker exec -it rabbit rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
#docker exec -it -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/docker.sock

docker exec -it php sh -c 'rm -rf /usr/lib/php5/modules'
docker exec -it php sh -c 'rm -rf /etc/php5/conf.d'
docker exec -it php sh -c 'mkdir /usr/lib/php5/'
docker exec -it php sh -c 'mkdir /etc/php5'
docker exec -it php sh -c 'ln -s /usr/local/lib/php/extensions/no-debug-non-zts-20131226 /usr/lib/php5/modules'
docker exec -it php sh -c 'ln -s /usr/local/etc/php/conf.d /etc/php5/conf.d'
docker exec -it php sh -c 'chown -R www-data:www-data /var/www/html'
if [ ! -f "${CONTAINER_HOME}/php/extensions/pdo_mysql.so" ]; then
docker exec -it php apk add --no-cache php5-pdo_mysql
fi
