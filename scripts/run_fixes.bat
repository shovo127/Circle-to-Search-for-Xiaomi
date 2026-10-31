@echo off
setlocal
set ADB=D:\Debload Phone with a Single Command\platform-tools\adb.exe

echo ============================================
echo  XIAOMI 13 - COMPLETE FIX SUITE
echo ============================================

echo [1/6] Starting ADB server...
"%ADB%" start-server
timeout /t 2 /nobreak >nul

echo [2/6] Connecting to device...
"%ADB%" connect 192.168.1.160:37249 2>nul
timeout /t 1 /nobreak >nul

echo [3/6] Verifying connection...
"%ADB%" -s 192.168.1.160:37249 shell echo CONNECTED
if errorlevel 1 (
    echo Connection failed. Trying mDNS fallback...
    for /f "tokens=3" %%i in ('"%ADB%" mdns services 2^>nul ^| findstr "adb-f802e4be"') do (
        set MDNS_ADDR=%%i
    )
    if defined MDNS_ADDR (
        "%ADB%" connect %MDNS_ADDR% 2>nul
        timeout /t 1 /nobreak >nul
        "%ADB%" -s %MDNS_ADDR% shell echo CONNECTED
    )
)

echo [4/6] Pushing fix script to device...
echo #!/system/bin/sh > "%TEMP%\mifix.sh"
echo echo "=== STANDBY BUCKETS ===" >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.whatsapp active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.whatsapp.w4b active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.google.android.gm active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.discord active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.microsoft.teams active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.instagram.android active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.facebook.katana active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.facebook.orca active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.linkedin.android active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket us.zoom.videomeetings active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.imo.android.imoim active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.google.android.gms active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.google.android.gsf active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.android.vending active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.xiaomi.xmsf active >> "%TEMP%\mifix.sh"
echo am set-standby-bucket com.xiaomi.xmsfkeeper active >> "%TEMP%\mifix.sh"
echo echo "=== DOZE WHITELIST ===" >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.whatsapp >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.whatsapp.w4b >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.google.android.gm >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.discord >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.microsoft.teams >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.instagram.android >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.facebook.katana >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.google.android.gms >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.android.vending >> "%TEMP%\mifix.sh"
echo cmd deviceidle whitelist +com.xiaomi.xmsf >> "%TEMP%\mifix.sh"
echo echo "=== APPOPS ===" >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp WAKE_LOCK allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp 133 allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp.w4b RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp.w4b RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp.w4b WAKE_LOCK allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.whatsapp.w4b 133 allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.google.android.gm RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.google.android.gm RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.discord RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.discord RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.microsoft.teams RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.microsoft.teams RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.instagram.android RUN_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo cmd appops set com.instagram.android RUN_ANY_IN_BACKGROUND allow >> "%TEMP%\mifix.sh"
echo echo "=== GLOBAL SETTINGS ===" >> "%TEMP%\mifix.sh"
echo settings put global heads_up_notifications_enabled 1 >> "%TEMP%\mifix.sh"
echo settings put global notification_badging 1 >> "%TEMP%\mifix.sh"
echo settings put global zen_mode 0 >> "%TEMP%\mifix.sh"
echo settings put global adaptive_battery_management_enabled 0 >> "%TEMP%\mifix.sh"
echo settings put global private_dns_mode hostname >> "%TEMP%\mifix.sh"
echo settings put global private_dns_specifier dns.google >> "%TEMP%\mifix.sh"
echo settings put global window_animation_scale 0.5 >> "%TEMP%\mifix.sh"
echo settings put global transition_animation_scale 0.5 >> "%TEMP%\mifix.sh"
echo settings put global animator_duration_scale 0.5 >> "%TEMP%\mifix.sh"
echo echo "=== DEBLOAT ===" >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.personalassistant >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.hybrid >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.systemAdSolution >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.contentextension >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.bugreport >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.joyose >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.metoknlp >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.yellowpage >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.payment >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 org.mipay.android.manager >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.nextpay >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.cit >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.voiceassist >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.voiceassistoverlay >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.voicetrigger >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.uireporter >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.gamecenter.sdk.service >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.minigame >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.xiaomi.migameservice >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.sohu.inputmethod.sogou.xiaomi >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.rom >> "%TEMP%\mifix.sh"
echo pm uninstall -k --user 0 com.miui.cleanmaster >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.miui.analytics >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.miui.cloudbackup >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.miui.weather2 >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.miui.miwallpaper >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.xiaomi.joyose >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.xiaomi.barrage >> "%TEMP%\mifix.sh"
echo pm disable-user --user 0 com.xiaomi.metoknlp >> "%TEMP%\mifix.sh"
echo echo "=== VERIFICATION ===" >> "%TEMP%\mifix.sh"
echo echo WA_BUCKET=$(am get-standby-bucket com.whatsapp) >> "%TEMP%\mifix.sh"
echo echo WA4B_BUCKET=$(am get-standby-bucket com.whatsapp.w4b) >> "%TEMP%\mifix.sh"
echo echo GMS_BUCKET=$(am get-standby-bucket com.google.android.gms) >> "%TEMP%\mifix.sh"
echo echo DNS=$(settings get global private_dns_specifier) >> "%TEMP%\mifix.sh"
echo echo ANIM=$(settings get global window_animation_scale) >> "%TEMP%\mifix.sh"
echo echo ADAPTIVE_BAT=$(settings get global adaptive_battery_management_enabled) >> "%TEMP%\mifix.sh"
echo pm list packages -d >> "%TEMP%\mifix.sh"
echo echo SCRIPT_COMPLETE >> "%TEMP%\mifix.sh"

"%ADB%" -s 192.168.1.160:37249 push "%TEMP%\mifix.sh" /data/local/tmp/mifix.sh
echo [5/6] Executing fix script on device...
"%ADB%" -s 192.168.1.160:37249 shell sh /data/local/tmp/mifix.sh

echo [6/6] Cleanup...
"%ADB%" -s 192.168.1.160:37249 shell rm /data/local/tmp/mifix.sh
echo ============================================
echo  DONE
echo ============================================

