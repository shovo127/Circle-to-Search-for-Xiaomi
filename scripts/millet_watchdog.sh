#!/system/bin/sh
LOG="/data/local/tmp/watchdog.log"
TARGET_APPS="com.google.android.gms,com.google.android.gsf,com.google.android.gm,com.whatsapp,com.whatsapp.w4b,com.imo.android.imoim,com.facebook.orca,com.instagram.android"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Watchdog started (PID $$)" >> "$LOG"

while true; do
    CURRENT=$(settings get system MILLET_NO_RESTRICT_APP 2>/dev/null)
    case "$CURRENT" in
        *com.google.android.gms*com.google.android.gm*com.instagram.android*)
            ;;
        *)
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] MILLET list reset detected! Re-injecting priority apps..." >> "$LOG"
            settings put system MILLET_NO_RESTRICT_APP "$TARGET_APPS"
            settings put global cached_apps_freezer disabled
            device_config put activity_manager_native_boot use_freezer false
            am broadcast -a com.google.android.intent.action.MCS_HEARTBEAT -p com.google.android.gms >/dev/null 2>&1
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Repaired successfully." >> "$LOG"
            ;;
    esac
    sleep 15
done
