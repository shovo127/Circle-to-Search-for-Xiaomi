@echo off
setlocal

rem Keep all project-owned temporary files and Gradle caches inside this workspace.
set "PROJECT_DIR=%~dp0"
for %%I in ("%PROJECT_DIR%..") do set "WORKSPACE_ROOT=%%~fI"
set "WORK_DIR=%WORKSPACE_ROOT%\.work"
set "TEMP=%WORK_DIR%\tmp"
set "TMP=%WORK_DIR%\tmp"
set "GRADLE_USER_HOME=%WORK_DIR%\gradle-home"

if not exist "%TEMP%" mkdir "%TEMP%"
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
if not defined ANDROID_HOME set "ANDROID_HOME=C:\Users\SHOVO.DESKTOP-GA0MH8U\AppData\Local\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java 17 was not found at "%JAVA_HOME%".
    endlocal & exit /b 1
)
if not exist "%ANDROID_HOME%\platforms\android-36\android.jar" (
    echo ERROR: Android SDK Platform 36 was not found at "%ANDROID_HOME%".
    endlocal & exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%WORKSPACE_ROOT%\platform-tools;%PATH%"
pushd "%PROJECT_DIR%"
call ".\gradlew.bat" --no-daemon %*
set "BUILD_EXIT=%ERRORLEVEL%"
popd

rem Remove only the project-owned temporary tool/cache directory after Gradle exits.
if exist "%WORK_DIR%" rmdir /s /q "%WORK_DIR%"

endlocal & exit /b %BUILD_EXIT%
