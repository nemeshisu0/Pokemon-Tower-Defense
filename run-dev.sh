#!/usr/bin/env bash
set -e

# Detect Java environment with JDK
if [ -z "$JAVA_HOME" ]; then
    if [ -x "/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon/bin/java" ]; then
        export JAVA_HOME="/home/nemesy/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon"
    fi
fi

JAR_FILE="releases/pokemon-tower-defense-dev.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR Sviluppatore non trovato in releases/. Compilazione in corso dal branch developer..."
    CURR_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    git checkout developer
    ./build.sh
    mkdir -p releases
    cp target/pokemon-tower-defense-1.0.0-SNAPSHOT.jar releases/pokemon-tower-defense-dev.jar
    git checkout "$CURR_BRANCH"
fi

echo "🤖 Avvio Pokemon Tower Defense (Versione Sviluppatore / Bot AI)..."
"$JAVA_HOME/bin/java" -jar "$JAR_FILE" "$@"
