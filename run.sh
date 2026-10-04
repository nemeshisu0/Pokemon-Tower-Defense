#!/usr/bin/env bash
set -e

# Detect Java environment with JDK
if [ -z "$JAVA_HOME" ]; then
    if [ -x "/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon/bin/java" ]; then
        export JAVA_HOME="/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon"
    fi
fi

JAR_FILE="target/pokemon-tower-defense-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR not found. Building project first..."
    ./build.sh
fi

echo "Starting Pokemon Tower Defense..."
"$JAVA_HOME/bin/java" -jar "$JAR_FILE" "$@"
