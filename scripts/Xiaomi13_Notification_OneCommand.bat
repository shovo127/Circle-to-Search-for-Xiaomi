@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Xiaomi 13 HyperOS 3 - Notification Reliability Setup

echo ============================================================
echo  Xiaomi 13 (fuxi) - Notification Reliability Setup
echo  Safe ADB configuration - no root, no app install, no debloat
echo ============================================================
echo.

set "ADB=adb"
where adb >nul 2>&1
if errorlevel 1 (
  if exist "D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe" (
    set "ADB=D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe"
  ) else if exist "%~dp0platform-tools\adb.exe" (
    set "ADB=%~dp0platform-tools\adb.exe"
  ) else (
    echo [ERROR] ADB was not found in PATH.
    echo Install Android platform-tools or run this BAT from its folder.
    pause
    exit /b 1
  )
)

set "DEVICE="
for /f "skip=1 tokens=1,2" %%A in ('"%ADB%" devices') do (
  if "%%B"=="device" if not defined DEVICE set "DEVICE=%%A"
)

if not defined DEVICE (
  echo [INFO] No authorized ADB device found. Checking wireless connection...
  "%ADB%" connect 192.168.1.160:37249 >nul 2>&1
  for /f "skip=1 tokens=1,2" %%A in ('"%ADB%" devices') do (
    if "%%B"=="device" if not defined DEVICE set "DEVICE=%%A"
  )
)

if not defined DEVICE (
  echo [INFO] No authorized ADB device found.
  echo Connect your Xiaomi 13 by USB with USB debugging enabled,
  echo or connect Wireless debugging first, then run this file again.
  echo.
  "%ADB%" devices
  pause
  exit /b 1
)

echo [OK] Connected device: %DEVICE%
echo [INFO] Checking device model...
"%ADB%" -s "%DEVICE%" shell getprop ro.product.model
"%ADB%" -s "%DEVICE%" shell getprop ro.build.version.incremental
echo.

echo [1/5] Saving current relevant settings...
if not exist "%~dp0notification_setup_backup" mkdir "%~dp0notification_setup_backup"
"%ADB%" -s "%DEVICE%" shell settings get global private_dns_mode > "%~dp0notification_setup_backup\private_dns_mode_before.txt"
"%ADB%" -s "%DEVICE%" shell settings get global private_dns_specifier > "%~dp0notification_setup_backup\private_dns_host_before.txt"
"%ADB%" -s "%DEVICE%" shell settings get global cached_apps_freezer > "%~dp0notification_setup_backup\cached_apps_freezer_before.txt"
"%ADB%" -s "%DEVICE%" shell settings get system MILLET_NO_RESTRICT_APP > "%~dp0notification_setup_backup\millet_list_before.txt"

echo [2/5] Preserving AdGuard Private DNS...
"%ADB%" -s "%DEVICE%" shell settings put global private_dns_mode hostname
"%ADB%" -s "%DEVICE%" shell settings put global private_dns_specifier dns.adguard.com

echo [3/5] Keeping Android cached-app freezer disabled...
"%ADB%" -s "%DEVICE%" shell settings put global cached_apps_freezer disabled
"%ADB%" -s "%DEVICE%" shell device_config put activity_manager_native_boot use_freezer false

echo [4/5] Applying background allowances to installed messaging and Google push apps...
for %%P in (
  com.google.android.gms
  com.google.android.gsf
  com.google.android.gm
  com.whatsapp
  com.whatsapp.w4b
  org.telegram.messenger
  com.discord
  com.microsoft.teams
  com.instagram.android
  com.facebook.katana
  com.facebook.orca
  com.linkedin.android
  us.zoom.videomeetings
  com.imo.android.imoim
  com.bKash.customerapp
  com.konasl.nagad
  com.pathao.user
  com.global.foodpanda.android
  com.ubercab
) do (
  set "PKGPATH="
  for /f "delims=" %%R in ('"%ADB%" -s "%DEVICE%" shell pm path %%P 2^>nul') do set "PKGPATH=%%R"
  if defined PKGPATH (
    echo   - Configuring %%P
    "%ADB%" -s "%DEVICE%" shell cmd deviceidle whitelist +%%P
    "%ADB%" -s "%DEVICE%" shell cmd appops set %%P RUN_IN_BACKGROUND allow
    "%ADB%" -s "%DEVICE%" shell cmd appops set %%P RUN_ANY_IN_BACKGROUND allow
    "%ADB%" -s "%DEVICE%" shell cmd appops set %%P WAKE_LOCK allow
    rem Xiaomi HyperOS app-op IDs previously confirmed on this device:
    "%ADB%" -s "%DEVICE%" shell cmd appops set %%P 10008 allow
    "%ADB%" -s "%DEVICE%" shell cmd appops set %%P 10021 allow
  )
)

echo [5/5] Verifying applied settings...
echo.
echo --- Private DNS ---
"%ADB%" -s "%DEVICE%" shell settings get global private_dns_mode
"%ADB%" -s "%DEVICE%" shell settings get global private_dns_specifier
echo --- Cached apps freezer ---
"%ADB%" -s "%DEVICE%" shell settings get global cached_apps_freezer
echo --- Doze whitelist ---
"%ADB%" -s "%DEVICE%" shell dumpsys deviceidle whitelist
echo.
echo ============================================================
echo  COMPLETED: ADB settings were applied where supported.
echo  Reboot the phone once, then test notifications with screen
echo  off and Wi-Fi/mobile data separately.
echo.
echo  IMPORTANT:
echo  - This cannot force an app/server to send an FCM message.
echo  - Xiaomi Background autostart and app-level Notifications
echo    may still need checking in HyperOS Settings.
echo  - Some Xiaomi app-op IDs may be unsupported on other builds.
echo  - Do not disable Google Play services or Xiaomi Security.
echo ============================================================
pause
