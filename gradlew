#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0
#
# Gradle 래퍼 Unix 쉘 스크립트
# gradle/wrapper/gradle-wrapper.properties 의 distributionUrl 에서
# 지정된 Gradle 버전을 자동으로 다운로드하고 실행한다.
#

# 오류 발생 시 즉시 종료
set -e

# 현재 OS 및 JVM 관련 설정
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# 플랫폼별 JVM 실행파일 결정
if [ "$(uname)" = "Darwin" ]; then
    darwin=true
else
    darwin=false
fi

# JVM 옵션 설정
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
JAVACMD="java"

# JAVA_HOME 설정이 있으면 그것을 사용
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
fi

# 스크립트 위치 탐색
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done
SAVED=$(pwd)
cd "$(dirname "$PRG")" >/dev/null
APP_HOME=$(pwd -P)
cd "$SAVED" >/dev/null

# 클래스패스 설정
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# 래퍼 실행
exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
