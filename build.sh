#!/bin/bash
echo "build start"

MAX_LOG_COUNT=1

if [ -d "/usr/lib/jvm" ]; then
for file in $(find /usr/lib/jvm/java-8*/bin -name "java")
do
	JAVA_HOME=$(dirname $file)
	JAVA_HOME=$(dirname $JAVA_HOME)
	break
done 
fi

if [ -d "/Library/Java/JavaVirtualMachines" ]; then
for file in $(find /Library/Java/JavaVirtualMachines/jdk1.8.*/Contents/Home/bin -name "java")
do
	JAVA_HOME=$(dirname $file)
	JAVA_HOME=$(dirname $JAVA_HOME)
	break
done  
fi

export JAVA_HOME

if [ -z $WORKSPACE ]; then 
	WORKSPACE=$(cd "$(dirname "$0")"; pwd)
fi

echo $WORKSPACE

BUILD_NUMBER_FILE=$WORKSPACE/.build_number.txt

if [ -z $BUILD_NUMBER ]; then 
	if [ -f "$BUILD_NUMBER_FILE" ]; then
		BUILD_NUMBER=`cat $BUILD_NUMBER_FILE`
	else
		BUILD_NUMBER=0
	fi 
	let BUILD_NUMBER+=1
fi

echo $BUILD_NUMBER > $BUILD_NUMBER_FILE

LAST_SUBMIT_FILE=$WORKSPACE/.last_submit.txt
if [ -f "$LAST_SUBMIT_FILE" ]; then
	LAST_SUBMIT=`cat $LAST_SUBMIT_FILE`
else
	LAST_SUBMIT=0
fi 

VERSION_NUMBER=`git log HEAD --oneline | wc -l | tr -d ' '`

let LOG_COUNT=$VERSION_NUMBER-$LAST_SUBMIT
if [ $LOG_COUNT -gt $MAX_LOG_COUNT ]; then
	LOG_COUNT=$MAX_LOG_COUNT
fi
if [ $LOG_COUNT -lt 1 ]; then
	LOG_COUNT=1
fi

`git log HEAD --oneline --grep "Merge branch " --invert-grep -$LOG_COUNT > $WORKSPACE/src/main/webapp/WEB-INF/conf/changes.txt`
echo $VERSION_NUMBER > $LAST_SUBMIT_FILE

APP=`mvn help:evaluate -Dexpression=project.artifactId|grep "^[^\[]"`
OLD_VERSION=`mvn help:evaluate -Dexpression=project.version|grep "^[^\[]"`

TIME_INFO=`TZ="Asia/Hong_Kong" date "+%m%d"`
COMMIT_HASH=`git log HEAD -1 --pretty=format:%H`
VESION_NAME=2.2.${VERSION_NUMBER}.${BUILD_NUMBER}.${TIME_INFO}
mvn versions:set -DnewVersion=${VESION_NAME}

rm -rf $WORKSPACE/src/main/webapp/WEB-INF/logs

VERSION_FILE=$WORKSPACE/src/main/java/com/lssservlet/main/Version.java
echo "package com.lssservlet.main;" > $VERSION_FILE
echo "" >> $VERSION_FILE
echo "public class Version {" >> $VERSION_FILE
echo "    public static String _VER = \"$VESION_NAME\";" >> $VERSION_FILE
echo "    public static String _COMMIT_HASH = \"$COMMIT_HASH\";" >> $VERSION_FILE
echo "}" >> $VERSION_FILE

mvn clean package -Dmaven.test.skip=true -Dapp.version=$VESION_NAME -Dapp.hash=$COMMIT_HASH

# revert version
mvn versions:set -DnewVersion=$OLD_VERSION
mvn dependency:tree
git checkout -- $VERSION_FILE


if [ -d "~/Containers/projects/lssservlet" ]; then
cp $WORKSPACE/target/lssservlet*.jar ~/Containers/projects/lssservlet/
fi
