# Xiaomi 13 (HyperOS 3 China ROM) Device Compatibility Report

## 1. Target Hardware & Firmware Specifications
- **Device**: Xiaomi 13
- **Model Number**: `2211133C`
- **Device Codename**: `fuxi`
- **Firmware**: HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`)
- **Android Version**: Android 16 (API 36)
- **Display**: 6.36-inch OLED, 1080 x 2400 pixels, ~414 ppi (nominal density ~440 dpi)
- **Navigation Mode**: Full screen gesture indicator enabled (white bottom pill: ~160dp x 20dp)

---

## 2. Activation Method Support Matrix

| Method | Target Status | Technical Mechanism & HyperOS Behavior | Setup Instructions / Truthful Caveat |
| :--- | :---: | :--- | :--- |
| **Method A: Bottom-Center Long Press** | **Fully Supported (Native)** | Uses `TYPE_ACCESSIBILITY_OVERLAY` calibrated to bottom center (`180dp x 32dp`). Intercepts long press (`450ms`) without triggering system UI conflicts. | Primary method. User enables Accessibility Service once in Settings. Works in all apps. |
| **Method B: Back Tap (Double/Triple)** | **Conditional (System-Assisted)** | Xiaomi HyperOS China ROM exposes `back_double_tap` and `back_triple_tap` in `Settings -> Additional settings -> Gesture shortcuts -> Back tap`. However, HyperOS restricts targets to predefined system tools or default digital assistant. | Supported if the user sets `Circle to Search` as default assistant or maps assistant to Back Tap. App exposes `android.intent.action.ASSIST`. |
| **Method C: Power Button Long Press** | **Conditional (System-Assisted)** | HyperOS China ROM reserves Power Long Press for `XiaoAI` (default) or `Power menu`. Changing assistant requires setting `Settings -> Apps -> Default apps -> Digital assistant app`. | App implements `android.intent.action.ASSIST` filter. Clear setup wizard guides user without spoofing or breaking emergency power functions. |

---

## 3. HyperOS 3 Specific Constraints & Mitigations

1. **Xiaomi AppsFilter Interaction Blocking**:
   - HyperOS implements strict package visibility. Without declaring `<queries>` in `AndroidManifest.xml` for `com.google.android.googlequicksearchbox` and `com.google.android.apps.photos`, inter-app intent dispatching is blocked with logcat error `AppsFilter: interaction BLOCKED`.
   - *Mitigation*: Declared explicit package and action queries in manifest.

2. **GreezeManager (Process Freeze)**:
   - Xiaomi's background freezer puts inactive services to sleep.
   - *Mitigation*: `NavigationGestureService` is bound directly by `system_server` (uid 1000) as an active accessibility service, granting it `PROC_STATE_PERSISTENT` and exempting it from background freezer freezes.

3. **Secure Window Screenshot Handling**:
   - DRM protected or `FLAG_SECURE` banking apps will return error code in `takeScreenshot`.
   - *Mitigation*: Graceful error callback with non-blocking toast disclosure, preventing crash.

