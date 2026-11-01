# APK Functional Comparison: Audited Binaries vs. Shovo Circle Search

This document compares the functional capabilities of the audited external APKs against the architectural requirements of **Shovo Circle Search** (`dev.shovo.circlesearch`).

---

## 1. Feature Comparison Matrix

| Functional Dimension | `MiCTS_2.6.apk` | `NotiGuard-v2.1.apk` | **Shovo Circle Search** (Target) |
| :--- | :---: | :---: | :---: |
| **Product Purpose** | Omni CTS Invoker | FCM Push Keeper | **Standalone Circle to Search** |
| **Primary Trigger** | Quick Settings Tile / LSPosed | Background Service | **Bottom Navigation Pill Hold** |
| **Secondary Triggers** | Assistant Shortcut | None | **Back Tap & Digital Assistant** |
| **Zero-Root Operation** | Conditional (Google flag) | Requires Shizuku | **100% Native (Accessibility API)** |
| **Full-Screen Capture** | Delegated to Google GMS | N/A | **Native Android 16 Screenshot** |
| **Freeform Lasso / Rect** | Delegated to Google GMS | N/A | **Interactive Dynamic Canvas** |
| **Multi-Engine Visual Search** | Google Omni only | None | **Google Lens, Bing, Yandex, Chooser** |
| **Offline Cache Purge** | None | None | **5-minute Scoped TTL + Manual** |
| **Haptic Feedback** | System default | None | **Calibrated Multi-stage Ticks** |
| **Target SDK** | SDK 36 | **SDK 22 (Legacy bypass)** | **SDK 35 (Compatible with Android 16)** |
| **Permissions Count** | 1 (`VIBRATE`) | 6 (including Shizuku/QueryAll) | **1 (`VIBRATE`)** |

---

## 2. Capability Extraction Analysis

### What was adopted from `MiCTS`:
- Knowledge of Google App's internal share activities (`LensShareEntryPointActivity` and `com.google.android.apps.photos.SEARCH_IMAGE`).
- Awareness that unrooted Omni invocation fails when Google flag `45631784` is withheld.

### What was rejected from `MiCTS`:
- Reliance on LSPosed / Xposed system hooks (`libxposed`).
- Silent failure without search engine fallback.

### What was adopted from `NotiGuard`:
- **Nothing.** NotiGuard is an unrelated background notification utility for China ROM notification delivery.

### What was rejected from `NotiGuard`:
- Target SDK 22 permission evasion.
- Shizuku privileged daemon dependency.
- Background persistent foreground service.
