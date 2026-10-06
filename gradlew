#!/data/data/com.termux/files/usr/bin/sh
# Gradle wrapper script for Termux / POSIX environments

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" )

# Setup Java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

# Fallback to system gradle if wrapper jar not downloaded yet
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    else
        echo "Gradle wrapper jar not found and 'gradle' command not in PATH." >&2
        exit 1
    fi
fi

exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
