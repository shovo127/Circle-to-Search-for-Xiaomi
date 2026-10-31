import subprocess
import re

ADB = r"D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe"
DEV = "192.168.1.160:42745"
APK_ON_DEVICE = "/data/local/tmp/app.apk"

subprocess.run([ADB, "connect", DEV], check=True)
subprocess.run([ADB, "-s", DEV, "push", r"D:\CIRCLE TO SEARCH FOR XIAOMI\CircleToSearchForXiaomi\app\build\outputs\apk\debug\app-debug.apk", APK_ON_DEVICE], check=True)
subprocess.run([ADB, "-s", DEV, "shell", "chmod", "777", APK_ON_DEVICE], check=True)

# Create session
out = subprocess.check_output([ADB, "-s", DEV, "shell", "pm", "install-create", "-r", "-d", "-t", "-i", "com.android.shell"]).decode()
print("Create output:", out)
m = re.search(r"\[(\d+)\]", out)
if not m:
    print("Could not parse session ID")
    exit(1)
session_id = m.group(1)
print(f"Session ID: {session_id}")

# Write APK into session
write_out = subprocess.check_output([ADB, "-s", DEV, "shell", "pm", "install-write", session_id, "base", APK_ON_DEVICE]).decode()
print("Write output:", write_out)

# Commit session
commit_out = subprocess.run([ADB, "-s", DEV, "shell", "pm", "install-commit", session_id], capture_output=True, text=True)
print("Commit stdout:", commit_out.stdout)
print("Commit stderr:", commit_out.stderr)

