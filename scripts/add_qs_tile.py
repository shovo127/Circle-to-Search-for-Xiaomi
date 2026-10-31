import subprocess

ADB = r'D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools\adb.exe'
DEV = '192.168.1.160:42745'
subprocess.run([ADB, 'connect', DEV], capture_output=True)

curr_tiles = subprocess.check_output([ADB, '-s', DEV, 'shell', 'settings', 'get', 'secure', 'sysui_qs_tiles']).decode().strip()
micts_tile = 'custom(com.parallelc.micts/.LaunchTileService)'
if micts_tile not in curr_tiles:
    new_tiles = curr_tiles + ',' + micts_tile
    subprocess.run([ADB, '-s', DEV, 'shell', 'settings', 'put', 'secure', 'sysui_qs_tiles', f"'{new_tiles}'"])

check = subprocess.check_output([ADB, '-s', DEV, 'shell', 'settings', 'get', 'secure', 'sysui_qs_tiles']).decode().strip()
print('Verified tiles contains micts:', 'com.parallelc.micts' in check)

