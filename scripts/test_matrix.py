import subprocess
import time
import re

ADB = r"D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe"
DEV = "192.168.1.160:42745"

def run_adb(args):
    cmd = [ADB, "-s", DEV] + args
    res = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
    return res.stdout.strip()

subprocess.run([ADB, "connect", DEV], capture_output=True)

apps = [
    ("Chrome", ["shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "https://en.wikipedia.org/wiki/Xiaomi", "com.android.chrome"]),
    ("YouTube", ["shell", "am", "start", "-n", "com.google.android.youtube/com.google.android.apps.youtube.app.watchwhile.WatchWhileActivity"]),
    ("Gallery", ["shell", "am", "start", "-n", "com.miui.gallery/.activity.HomePageActivity"]),
    ("Launcher", ["shell", "input", "keyevent", "3"])
]

results = []
print("Starting 20-cycle validation test...")

for cycle in range(1, 21):
    app_name, launch_cmd = apps[(cycle - 1) % len(apps)]
    run_adb(launch_cmd)
    time.sleep(1.2)
    
    # Bottom hold
    t0 = time.time()
    run_adb(["shell", "input", "swipe", "540", "2380", "540", "2380", "480"])
    time.sleep(0.9)
    focus = run_adb(["shell", "dumpsys", "window"])
    elapsed = int((time.time() - t0) * 1000)
    
    is_success = "CircleSearchActivity" in focus
    results.append({
        "cycle": cycle,
        "app": app_name,
        "success": is_success,
        "duration_ms": elapsed
    })
    print(f"Cycle {cycle:02d} | App: {app_name:<8} | Triggered: {is_success} | Time: {elapsed}ms")
    
    # Dismiss for next cycle
    run_adb(["shell", "input", "keyevent", "4"])
    time.sleep(0.5)

success_count = sum(1 for r in results if r["success"])
print(f"\nFinal Summary: {success_count}/20 successful activations ({success_count/20*100:.1f}%)")

