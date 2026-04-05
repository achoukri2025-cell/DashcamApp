#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

DEFAULT_JVM_OPTS="-Xmx2048m -Dfile.encoding=UTF-8"

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

org_gradle_wrapper_GradleWrapperMain="org.gradle.wrapper.GradleWrapperMain"

set -- "$@"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
