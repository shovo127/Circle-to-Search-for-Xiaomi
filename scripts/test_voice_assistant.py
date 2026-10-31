import subprocess

ADB = r'D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe'
DEV = '192.168.1.160:42745'
subprocess.run([ADB, 'connect', DEV], capture_output=True)

orig = subprocess.check_output([ADB, '-s', DEV, 'shell', 'settings', 'get', 'secure', 'entity_config_key_voice_assistant']).decode().strip()
print('Original:', orig)

test_val = orig.replace('com.miui.voiceassist', 'com.google.android.googlequicksearchbox').replace('com.xiaomi.voiceassistant.VoiceService', 'com.google.android.voiceinteraction.GsaVoiceInteractionService')
print('Testing:', test_val)
subprocess.run([ADB, '-s', DEV, 'shell', 'settings', 'put', 'secure', 'entity_config_key_voice_assistant', f"'{test_val}'"])

check = subprocess.check_output([ADB, '-s', DEV, 'shell', 'settings', 'get', 'secure', 'entity_config_key_voice_assistant']).decode().strip()
print('Current on device:', check)

