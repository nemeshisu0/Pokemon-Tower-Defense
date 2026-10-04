#!/usr/bin/env bash
set -e

# Detect Java environment with JDK
if [ -z "$JAVA_HOME" ]; then
    if [ -x "/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon/bin/java" ]; then
        export JAVA_HOME="/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon"
    fi
fi

MVN_BIN="/home/nemesy/.local/bin/mvn"
if ! command -v "$MVN_BIN" &> /dev/null; then
    MVN_BIN="mvn"
fi

echo "Building project with Maven..."
"$MVN_BIN" clean package -DskipTests
echo "Build completed successfully! Fat JAR generated in target/pokemon-tower-defense-1.0.0-SNAPSHOT.jar"
