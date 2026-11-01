# APK Static Forensic Analysis Report

**Audit Target:** Discovered external APK binaries (`MiCTS_2.6.apk` and `NotiGuard-v2.1.apk`)  
**Inspection Tools:** `aapt` (Build Tools 35.0.0), `apksigner`, `dexdump`, Microsoft Windows Defender Antivirus Engine  
**Execution Status:** Static analysis only; 0 execution, 0 device installation.

---

## 1. Binary: `MiCTS_2.6.apk`

- **File Path:** `D:\CIRCLE TO SEARCH FOR XIAOMI\MiCTS_2.6.apk`
- **File Size:** 4,733,750 bytes
- **SHA-256:** `4680D24112FBF0D7FF5BBC055B760F2D67173D7D5879EC5411540D5CAA7A97B8`
- **Package Name:** `com.parallelc.micts`
- **Application Label:** `MiCTS`
- **Version Code:** `66` | **Version Name:** `2.6`
- **Min SDK:** `28` (Android 9 Pie)
- **Target SDK:** `36` (Android 16 Vanilla Ice Cream)
- **Compile SDK:** `36`

### Signing Certificate & Signatures
- **Signature Scheme:** v2 verified; v1/v3/v4 false.
- **Signer DN:** `ST=BJ, L=CN, O=Parallelc, OU=Parallelc, CN=Parallelc`
- **Certificate SHA-256:** `21eddc00ff8b47127fc6a639d39c2a0d52b39faaf7a817e012a1d2306d3a991f`
- **Key Algorithm:** RSA 2048-bit

### Permissions Requested
- `android.permission.VIBRATE`
- `com.parallelc.micts.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

### Exported Components & Providers
- **Exported Activity:** `com.parallelc.micts.ui.activity.MainActivity` (Launcher)
- **Exported Activity:** `com.parallelc.micts.ui.activity.SettingsActivity` (Filters for Xposed module settings & QS preferences)
- **Exported Service:** `com.parallelc.micts.LaunchTileService` (`android.permission.BIND_QUICK_SETTINGS_TILE`)
- **Exported Provider:** `io.github.libxposed.service.XposedProvider` (`com.parallelc.micts.XposedService`)
- **Native ABI Libraries:** `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

### Security & Functional Assessment
- Contains Xposed/LSPosed hook infrastructure (`libxposed`).
- On unrooted stock HyperOS, relies on `LaunchTileService` to dispatch an Omni assistant intent. If Google GMS server-side flag `45631784` is false, Google ignores the intent.
- Offline Antivirus Scan: Clean (Windows Defender Engine scan reported 0 threats).

---

## 2. Binary: `NotiGuard-v2.1.apk`

- **File Path:** `D:\CIRCLE TO SEARCH FOR XIAOMI\NotiGuard-v2.1.apk`
- **File Size:** 10,849,167 bytes
- **SHA-256:** `F2F1968719BEEC07ECEB7EB61AA53A5AF59A4C22C3D975040CFB260176124072`
- **Package Name:** `com.local.notiguard`
- **Application Label:** `NotiGuard`
- **Version Code:** `3` | **Version Name:** `2.1`
- **Min SDK:** `26` (Android 8.0 Oreo)
- **Target SDK:** `22` (Android 5.1 Lollipop - deliberate legacy target to bypass modern runtime permission constraints!)
- **Compile SDK:** `35`

### Signing Certificate & Signatures
- **Signature Scheme:** v2 verified.
- **Signer DN:** `CN=liujang, O=NotiGuard`
- **Certificate SHA-256:** `628effdeeb7d4ed9b5f06d5aed2c49372073456ce99bc678171025c5854b8f29`
- **Key Algorithm:** RSA 4096-bit

### Permissions Requested
- `android.permission.QUERY_ALL_PACKAGES`
- `moe.shizuku.manager.permission.API_V23`
- `android.permission.WRITE_SETTINGS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECEIVE_BOOT_COMPLETED`

### Exported Components & Providers
- **Exported Provider:** `rikka.shizuku.ShizukuProvider` (`com.local.notiguard.shizuku`) with `INTERACT_ACROSS_USERS_FULL`
- **Service:** `com.local.notiguard.fcmguard.FcmGuardService`
- **Receiver:** `com.local.notiguard.fcmguard.FcmGuardBootReceiver` (BOOT_COMPLETED)

### Security & Functional Assessment
- Target SDK 22 combined with Shizuku privileged IPC (`moe.shizuku.privileged.api`) and `QUERY_ALL_PACKAGES`.
- Sole purpose: Background FCM notification keeper for China ROM devices.
- **Completely irrelevant to Circle to Search.**
- Must NOT be bundled or integrated into Shovo Circle Search.
