import subprocess
import time
import threading
import xml.etree.ElementTree as ET

ADB = r"D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe"
DEV = "192.168.1.160:42745"
APK = r"D:\CIRCLE TO SEARCH FOR XIAOMI\CircleToSearchForXiaomi\app\build\outputs\apk\debug\app-debug.apk"

def auto_click():
    for _ in range(30):
        time.sleep(0.5)
        # Check if package installer dialog is visible
        try:
            res = subprocess.run([ADB, "-s", DEV, "shell", "uiautomator", "dump", "/data/local/tmp/inst_ui.xml"], capture_output=True, timeout=5)
            xml_data = subprocess.check_output([ADB, "-s", DEV, "shell", "cat", "/data/local/tmp/inst_ui.xml"], timeout=5).decode(errors="ignore")
            if "packageinstaller" in xml_data or "Install" in xml_data or "allow" in xml_data.lower():
                root = ET.fromstring(xml_data)
                for node in root.iter("node"):
                    text = node.attrib.get("text", "")
                    rid = node.attrib.get("resource-id", "")
                    bounds = node.attrib.get("bounds", "")
                    if "install" in text.lower() or "ok" in text.lower() or "allow" in text.lower() or "btn_install" in rid:
                        print(f"Found candidate button: rid={rid}, text={text}, bounds={bounds}")
                        # parse bounds [x1, y1][x2, y2]
                        if bounds and bounds.startswith("["):
                            parts = bounds.replace("][", ",").replace("[", "").replace("]", "").split(",")
                            if len(parts) == 4:
                                cx = (int(parts[0]) + int(parts[2])) // 2
                                cy = (int(parts[1]) + int(parts[3])) // 2
                                print(f"Tapping ({cx}, {cy})...")
                                subprocess.run([ADB, "-s", DEV, "shell", "input", "tap", str(cx), str(cy)])
                                time.sleep(0.5)
                                return
        except Exception as e:
            # print("Polling error:", e)
            pass

subprocess.run([ADB, "connect", DEV], capture_output=True)

t = threading.Thread(target=auto_click)
t.start()

print("Starting streamed install...")
res = subprocess.run([ADB, "-s", DEV, "install", "-r", "-d", APK], capture_output=True, text=True)
print("STDOUT:", res.stdout)
print("STDERR:", res.stderr)
t.join()
