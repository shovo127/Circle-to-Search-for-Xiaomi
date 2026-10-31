@echo off
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
set "ANDROID_HOME=C:\Users\SHOVO.DESKTOP-GA0MH8U\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;C:\Gradle\gradle-8.10.2\bin;D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools;%PATH%"
cd /d "%~dp0"
call C:\Gradle\gradle-8.10.2\bin\gradle.bat %*

