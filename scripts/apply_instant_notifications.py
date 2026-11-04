import subprocess
import time

ADB = r"D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe"
DEV = "192.168.1.160:42109"

def run_adb(args):
    cmd = [ADB, "-s", DEV] + args
    res = subprocess.run(cmd, capture_output=True, text=True, errors="ignore")
    return res.stdout.strip()

print("[1/7] Connecting to device...")
subprocess.run([ADB, "connect", DEV], capture_output=True)

model = run_adb(["shell", "getprop", "ro.product.model"])
build = run_adb(["shell", "getprop", "ro.build.version.incremental"])
print(f"Connected: {DEV} | Model: {model} | Build: {build}")

if model != "2211133C":
    print("Error: Target device mismatch!")
    exit(1)

# Priority Instant Notification Apps
priority_apps = [
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.google.android.gm",       # Mail
    "com.whatsapp",                 # WhatsApp
    "com.whatsapp.w4b",             # WhatsApp Business
    "com.imo.android.imoim",        # IMO
    "com.facebook.orca",            # Messenger
    "com.instagram.android"         # Instagram
]

# Apps to RESTRICT from background execution (Bkash, Nagad, Pathao, Foodpanda, Uber)
restrict_apps = [
    "com.bKash.customerapp",
    "com.konasl.nagad",
    "com.pathao.user",
    "com.global.foodpanda.android",
    "com.ubercab"
]

print("\n[2/7] Disabling cached_apps_freezer (Preventing socket freeze)...")
run_adb(["shell", "settings", "put", "global", "cached_apps_freezer", "disabled"])
run_adb(["shell", "device_config", "put", "activity_manager_native_boot", "use_freezer", "false"])
freezer = run_adb(["shell", "settings", "get", "global", "cached_apps_freezer"])
print(f"cached_apps_freezer status: {freezer}")

print("\n[3/7] Restricting background-heavy non-essential apps...")
for pkg in restrict_apps:
    installed = run_adb(["shell", "pm", "path", pkg])
    if installed:
        print(f" - Restricting {pkg}")
        run_adb(["shell", "cmd", "deviceidle", "whitelist", f"-{pkg}"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "RUN_IN_BACKGROUND", "ignore"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", "ignore"])

print("\n[4/7] Applying instant delivery permissions to priority apps...")
for pkg in priority_apps:
    installed = run_adb(["shell", "pm", "path", pkg])
    if installed:
        print(f" + Whitelisting {pkg}")
        run_adb(["shell", "cmd", "deviceidle", "whitelist", f"+{pkg}"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "RUN_IN_BACKGROUND", "allow"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", "allow"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "WAKE_LOCK", "allow"])
        run_adb(["shell", "cmd", "appops", "set", pkg, "10008", "allow"])  # Autostart
        run_adb(["shell", "cmd", "appops", "set", pkg, "10021", "allow"])  # Secondary Autostart

print("\n[5/7] Configuring MILLET_NO_RESTRICT_APP for HyperOS PowerKeeper...")
millet_val = ",".join(priority_apps)
run_adb(["shell", "settings", "put", "system", "MILLET_NO_RESTRICT_APP", f"'{millet_val}'"])
cur_millet = run_adb(["shell", "settings", "get", "system", "MILLET_NO_RESTRICT_APP"])
print(f"MILLET_NO_RESTRICT_APP is now: {cur_millet}")

print("\n[6/7] Deploying and starting MILLET watchdog daemon...")
watchdog_script = f"""#!/system/bin/sh
LOG="/data/local/tmp/watchdog.log"
TARGET_APPS="{millet_val}"

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
"""

# Push script to device
script_local = r"D:\CIRCLE TO SEARCH FOR XIAOMI\scripts\millet_watchdog.sh"
with open(script_local, "w", newline="\n") as f:
    f.write(watchdog_script)

run_adb(["push", script_local, "/data/local/tmp/millet_watchdog.sh"])
run_adb(["shell", "chmod", "755", "/data/local/tmp/millet_watchdog.sh"])

# Kill any old watchdog and start fresh
run_adb(["shell", "pkill", "-f", "millet_watchdog.sh"])
run_adb(["shell", "nohup", "sh", "/data/local/tmp/millet_watchdog.sh", ">", "/dev/null", "2>&1", "&"])
time.sleep(1)

ps = run_adb(["shell", "ps", "-ef"])
found_pid = False
for line in ps.splitlines():
    if "millet_watchdog.sh" in line:
        print(f"Watchdog daemon active: {line}")
        found_pid = True
if not found_pid:
    print("Warning: Watchdog process could not be confirmed via ps.")

print("\n[7/7] Triggering FCM Heartbeat and Reconnect...")
run_adb(["shell", "am", "broadcast", "-a", "com.google.android.intent.action.MCS_HEARTBEAT", "-p", "com.google.android.gms"])

# Reconnect WhatsApp and GMS
run_adb(["shell", "am", "broadcast", "-a", "android.net.conn.CONNECTIVITY_CHANGE", "-p", "com.whatsapp"])
run_adb(["shell", "am", "broadcast", "-a", "android.net.conn.CONNECTIVITY_CHANGE", "-p", "com.whatsapp.w4b"])
run_adb(["shell", "am", "broadcast", "-a", "android.net.conn.CONNECTIVITY_CHANGE", "-p", "com.facebook.orca"])

print("\n=== VERIFICATION SUMMARY ===")
print("1. Private DNS:")
print("  ", run_adb(["shell", "settings", "get", "global", "private_dns_specifier"]))
print("2. Cached Apps Freezer:")
print("  ", run_adb(["shell", "settings", "get", "global", "cached_apps_freezer"]))
print("3. MILLET_NO_RESTRICT_APP:")
print("  ", run_adb(["shell", "settings", "get", "system", "MILLET_NO_RESTRICT_APP"]))
print("4. Watchdog Log:")
print(run_adb(["shell", "tail", "-n", "4", "/data/local/tmp/watchdog.log"]))
print("\nInstant notification configuration completed successfully!")
