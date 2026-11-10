#!/bin/bash
cd "$(dirname "$0")"
mkdir -p build
find src -name "*.java" | xargs javac -encoding UTF-8 -d build -cp "lib/*"
cp src/styles.css build/
java --module-path lib/javafx-base-17.0.10-linux.jar:lib/javafx-graphics-17.0.10-linux.jar:lib/javafx-controls-17.0.10-linux.jar --add-modules javafx.controls -cp "build:lib/sqlite-jdbc.jar" com.zad.ZadApp
