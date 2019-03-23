#!/bin/bash

clusterId=1

if [ -n "$1" ]; then
	if [ $1 = "1" ]; then
		clusterId=1
	elif [ $1 = "2" ]; then
		clusterId=2
    elif [ $1 = "3" ]; then
		clusterId=3
    elif [ $1 = "4" ]; then
		clusterId=4
    elif [ $1 = "5" ]; then
		clusterId=5
    elif [ $1 = "6" ]; then
		clusterId=6
    elif [ $1 = "7" ]; then
		clusterId=7
    elif [ $1 = "8" ]; then
		clusterId=8
    elif [ $1 = "9" ]; then
		clusterId=9
    else 
        echo "unknown clusterId $1"
        exit 1
	fi
else
    echo "default clusterId 1"
fi

exec java -Djava.awt.headless=true -Xms512m -Xmx512m \
-XX:+UseParNewGC  \
-XX:+UseConcMarkSweepGC  \
-XX:+UseTLAB  \
-XX:NewSize=128m  \
-XX:MaxNewSize=128m  \
-XX:MaxTenuringThreshold=0  \
-XX:SurvivorRatio=1024  \
-XX:+UseCMSInitiatingOccupancyOnly  \
-XX:CMSInitiatingOccupancyFraction=60 \
-XX:+DisableExplicitGC \
-jar ./target/felyxservlet-*.jar -p 909${clusterId} -c ./src/main/webapp/WEB-INF/conf/setting.conf -n local${clusterId} -id 100${clusterId}