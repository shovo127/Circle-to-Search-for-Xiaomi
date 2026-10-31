# Circle to Search for Xiaomi (`dev.shovo.circlesearch`)

Comprehensive suite for **Xiaomi 13 (fuxi)** running **HyperOS 3.0 / Android 16**, featuring:
1. **Circle to Search for Xiaomi (`dev.shovo.circlesearch`):** Full Android Studio project providing Samsung-inspired screen selection, circle/rectangle drawing, haptic feedback, and instant Google Lens visual search.
2. **Notification Reliability & PowerKeeper Optimization:** HyperOS 3 MILLET watchdog, Android Doze whitelisting, and FCM socket protection (`cached_apps_freezer` configuration).
3. **HyperOS 3 Complete Debloat & Master Restoration:** Safe, reversible debloat tooling with 1-click restore scripts for all 29 uninstalled packages.
4. **AdGuard Private DNS:** Native system-wide ad sinkholing without battery drain or VPN overhead.

---

## 📱 Supported Target
- **Device:** Xiaomi 13 (`2211133C`, codename `fuxi`)
- **OS:** Xiaomi HyperOS 3.0 (`OS3.0.309.0.WMCCNXM`)
- **Base:** Android 16 (SDK 36)
- **Root Status:** Zero root, no bootloader unlock required.

---

## 📂 Repository Structure

```text
├── CircleToSearchForXiaomi/        # Complete Android Studio Project
│   ├── app/
│   │   ├── src/main/java/dev/shovo/circlesearch/
│   │   │   ├── CircleCanvasView.kt         # Ethereal cyan glowing circle & box drawing
│   │   │   ├── CircleSearchActivity.kt     # Translucent screen overlay & crop trigger
│   │   │   ├── LensSearchHelper.kt         # Crop logic & Google Lens intent dispatcher
│   │   │   ├── MainActivity.kt             # Device detector & diagnostics
│   │   │   ├── NavigationGestureService.kt # Accessibility & bottom-zone hold detector
│   │   │   └── QuickSearchTileService.kt   # Control Center Quick Settings tile
│   │   ├── src/main/res/                   # Layouts, colors, drawables, accessibility XML
│   │   └── AndroidManifest.xml             # Declared permissions & FileProvider
│   ├── build.gradle.kts                    # Root build configuration
│   └── settings.gradle.kts
│
├── platform-tools/                         # Official Android ADB tools
│   ├── adb.exe
│   └── ...
│
├── scripts/                                # Automation & diagnostic tools
│   ├── Xiaomi13_Notification_OneCommand.bat  # 1-click notification & Doze optimizer
│   ├── Xiaomi13_Circle_to_Search_Safe_Setup.bat
│   ├── add_qs_tile.py                       # Control Center tile injector
│   ├── audit_apk.py                         # APK signature & SHA-256 auditor
│   └── ui_dump.py                           # Real-time UI dump & touch automation
│
├── backups/                                # Full device configuration & restoration
│   ├── master_restore_20261031_223959.bat   # 100% reversible restore for all uninstalled packages
│   └── ...
│
├── MiCTS_2.6.apk                           # Audited MiCTS release binary
└── NotiGuard-v2.1.apk                      # Audited NotiGuard release binary
```

---

## 🚀 Quick Start

### 1. Notification Reliability & Zero-Delay Fix
To ensure WhatsApp, Messenger, Gmail, and banking notifications arrive instantaneously on HyperOS 3:
```bat
scripts\Xiaomi13_Notification_OneCommand.bat
```

### 2. Circle to Search Trigger Options
Because Xiaomi China ROM (`WMCCNXM`) hardcodes the bottom navigation handle to XiaoAI in the system partition:
- **Method 1 (Quick Settings Tile):** Swipe down Control Center ➔ Tap **Circle to Search**.
- **Method 2 (Double Back-Tap):** Go to `Settings ➔ Additional settings ➔ Gesture shortcuts ➔ Back tap` and select Open app ➔ **Circle to Search**.
- **Method 3 (Navigation Gesture Service):** Enable the accessibility service from `CircleToSearchForXiaomi` for bottom-pill hold detection.

### 3. Master Restoration
To revert any debloated packages at any time:
```bat
backups\master_restore_20261031_223959.bat
```

---

## 🔒 Security & Privacy
- Zero cloud AI dependencies for screenshot capture or image processing.
- All cropped search bitmaps are cached locally in app cache and shared securely with Google Lens via Android's `FileProvider`.
- All operations preserve Knox/Widevine L1 integrity, official OTA updates, and device warranties.
