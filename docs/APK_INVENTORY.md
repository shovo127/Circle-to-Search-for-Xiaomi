# APK Forensics: Complete Workspace Inventory

**Audit Date:** 2026-11-01  
**Target Hardware:** Xiaomi 13 (`2211133C`, `fuxi`)  
**Firmware:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`, Android 16 / API 36)  
**Auditor:** Lead Platform Security & Reverse Engineering Specialist  

---

## 1. Discovery Scope
A recursive discovery search was performed across the development workspace:
- `D:\CIRCLE TO SEARCH FOR XIAOMI`
- `D:\Debload Phone with a Single Command`
*(Windows Recycle Bin was strictly excluded and not accessed).*

---

## 2. Inventory Table

| File Name | Exact File Path | Size (Bytes) | SHA-256 Digest | Status & Provenance |
| :--- | :--- | :--- | :--- | :--- |
| **MiCTS_2.6.apk** | `D:\CIRCLE TO SEARCH FOR XIAOMI\MiCTS_2.6.apk` | 4,733,750 | `4680D24112FBF0D7FF5BBC055B760F2D67173D7D5879EC5411540D5CAA7A97B8` | External binary from parallelcc/MiCTS release v2.6. |
| **NotiGuard-v2.1.apk** | `D:\CIRCLE TO SEARCH FOR XIAOMI\NotiGuard-v2.1.apk` | 10,849,167 | `F2F1968719BEEC07ECEB7EB61AA53A5AF59A4C22C3D975040CFB260176124072` | External binary (notification/FCM keeper utility). |
| **app-debug.apk** | `D:\CIRCLE TO SEARCH FOR XIAOMI\CircleToSearchForXiaomi\app\build\outputs\apk\debug\app-debug.apk` | ~4.5 MB | *(Dynamic build artifact)* | Locally compiled Shovo Circle Search debug binary. |
| **platform-tools.zip** | `D:\CIRCLE TO SEARCH FOR XIAOMI\platform-tools.zip` | 8,044,989 | *(Archive)* | Official Google Android SDK platform tools (adb, fastboot). |

---

## 3. Directory Audit Notes
- **`D:\Debload Phone with a Single Command`**: Audited via recursive inspection. Confirmed empty of any APK or archive files.
- **Excluded Items**: Gradle wrapper jar (`gradle-wrapper.jar`) in build tooling verified as authentic Gradle 8.10.2 binary.
- **Handling Protocol**: No APK was installed, executed, or repackaged into Shovo Circle Search. All binaries remain in static analysis quarantine.

