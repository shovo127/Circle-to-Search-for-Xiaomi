# APK Security Findings & Risk Analysis

**Audit Date:** 2026-11-01  
**Standards:** OWASP MASVS (Mobile Application Security Verification Standard), Static Binary Analysis  

---

## 1. Security Analysis: `MiCTS_2.6.apk`

### Findings
1. **Xposed Hook Provider Exposed:**  
   - Component: `io.github.libxposed.service.XposedProvider` exported with authority `com.parallelc.micts.XposedService`.
   - Risk: While protected on unrooted devices, on rooted systems this provider can be manipulated by malicious third-party apps if signature checks are weakened.
2. **Hardcoded GMS Whitelist Assumption:**  
   - Risk: When executed on stock Xiaomi HyperOS, invocation of Google's Omni entry point throws unhandled exceptions or terminates silently due to lack of GMS device whitelisting.

### Conclusion on MiCTS
Safe from trojans/malware (verified via Windows Defender Antivirus Engine), but architecturally fragile for production use on unrooted Xiaomi China ROM devices.

---

## 2. Security Analysis: `NotiGuard-v2.1.apk`

### Findings
1. **Deliberate Target SDK Downgrade (Critical Architecture Finding):**  
   - `targetSdkVersion = 22` (Android 5.1 Lollipop).
   - Analysis: By targeting API 22, the app operates under legacy pre-runtime permission rules, bypassing modern Android permission prompts and restrictions (like background start restrictions and scoped storage).
2. **Broad Package Visibility & Inter-process Privileges:**  
   - Requests `android.permission.QUERY_ALL_PACKAGES` and `moe.shizuku.manager.permission.API_V23`.
   - Exports `rikka.shizuku.ShizukuProvider` with `android.permission.INTERACT_ACROSS_USERS_FULL`.
3. **Persistent Autostart:**  
   - Listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` to maintain a persistent background foreground service.

### Conclusion on NotiGuard
NotiGuard employs aggressive privilege escalation techniques suitable only for specialized notification debugging. **It must be completely quarantined and never bundled with Shovo Circle Search.**

---

## 3. Shovo Circle Search Hardening Measures
In response to the forensic audit, **Shovo Circle Search** has been strictly hardened:
- `targetSdkVersion` set to modern **35**.
- `android:allowBackup="false"` to prevent local data exfiltration.
- Complete removal of `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `INTERNET`, and `RECEIVE_BOOT_COMPLETED`.
- Accessibility service configured with `canRetrieveWindowContent="false"` so screen text and password nodes can never be read.

