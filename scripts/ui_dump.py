import subprocess
import xml.etree.ElementTree as ET

ADB = r'D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe'
DEV = '192.168.1.160:42745'

subprocess.run([ADB, 'connect', DEV], capture_output=True)
subprocess.run([ADB, '-s', DEV, 'shell', 'uiautomator', 'dump', '/data/local/tmp/ui.xml'], capture_output=True)
xml = subprocess.check_output([ADB, '-s', DEV, 'shell', 'cat', '/data/local/tmp/ui.xml']).decode(errors='ignore')

root = ET.fromstring(xml)
for node in root.iter('node'):
    text = node.attrib.get('text', '')
    desc = node.attrib.get('content-desc', '')
    rid = node.attrib.get('resource-id', '')
    bounds = node.attrib.get('bounds', '')
    if text or desc:
        print(f'{rid} | text="{text}" | desc="{desc}" | bounds={bounds}')

