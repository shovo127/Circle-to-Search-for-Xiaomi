@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Xiaomi 13 - Safe Circle to Search Setup

set "ADB=D:\Debload Phone with a Single Command\platform-tools\adb.exe"
set "DEVICE_IP=192.168.1.160:42745"
set "APK=D:\Debload Phone with a Single Command\MiCTS_2.6.apk"
set "EXPECTED_SHA=4680d24112fbf0d7ff5bbc055b760f2d67173d7d5879ec5411540d5caa7a97b8"

echo ============================================================
echo  Xiaomi 13 - Safe Circle to Search setup
echo  No root / no bootloader unlock / no device spoofing
echo ============================================================
echo.

"%ADB%" connect %DEVICE_IP%

echo [1/3] Setting Google as assistant and enabling screen context...
"%ADB%" -s "%DEVICE_IP%" shell cmd role add-role-holder --user 0 android.app.role.ASSISTANT com.google.android.googlequicksearchbox
"%ADB%" -s "%DEVICE_IP%" shell settings put secure assist_structure_enabled 1
"%ADB%" -s "%DEVICE_IP%" shell settings put secure assist_screenshot_enabled 1

echo [2/3] Setting Doze and AppOps permissions for Google App...
"%ADB%" -s "%DEVICE_IP%" shell cmd deviceidle whitelist +com.google.android.googlequicksearchbox
"%ADB%" -s "%DEVICE_IP%" shell cmd appops set com.google.android.googlequicksearchbox RUN_IN_BACKGROUND allow
"%ADB%" -s "%DEVICE_IP%" shell cmd appops set com.google.android.googlequicksearchbox RUN_ANY_IN_BACKGROUND allow
"%ADB%" -s "%DEVICE_IP%" shell cmd appops set com.google.android.googlequicksearchbox WAKE_LOCK allow
"%ADB%" -s "%DEVICE_IP%" shell cmd appops set com.google.android.googlequicksearchbox 10008 allow
"%ADB%" -s "%DEVICE_IP%" shell cmd appops set com.google.android.googlequicksearchbox 10021 allow

echo [3/3] Verification:
"%ADB%" -s "%DEVICE_IP%" shell cmd role get-role-holders --user 0 android.app.role.ASSISTANT
"%ADB%" -s "%DEVICE_IP%" shell settings get secure assist_structure_enabled
"%ADB%" -s "%DEVICE_IP%" shell settings get secure assist_screenshot_enabled

echo.
echo ============================================================
echo Setup completed. Tap 'Install' on your phone screen to complete MiCTS installation.
echo ============================================================
pause

