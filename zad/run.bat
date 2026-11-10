@echo off
cd /d "%~dp0"
if not exist build mkdir build
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d build -cp "lib\*" @sources.txt
if errorlevel 1 (echo BUILD FAILED & pause & exit /b 1)
del sources.txt
copy /y src\styles.css build\ >nul
java --module-path "lib\javafx-base-17.0.10-win.jar;lib\javafx-graphics-17.0.10-win.jar;lib\javafx-controls-17.0.10-win.jar" --add-modules javafx.controls -cp "build;lib\sqlite-jdbc.jar" com.zad.ZadApp
