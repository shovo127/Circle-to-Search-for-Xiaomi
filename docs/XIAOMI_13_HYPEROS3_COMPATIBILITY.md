# Xiaomi 13 (HyperOS 3 China ROM) Device Compatibility & Optimization Report

**Target Hardware:** Xiaomi 13  
**Model Number:** `2211133C`  
**Device Codename:** `fuxi`  
**Firmware Target:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`)  
**Android Platform:** Android 16 (API 36)  
**Display:** 6.36-inch Flat AMOLED, 1080 × 2400 pixels (~414 ppi, 440 dpi nominal density, 120Hz refresh rate, 240Hz touch sampling rate, 20:9 aspect ratio)  
**Cutout & Geometry:** Centered punch-hole camera at top (`y = 48px`, radius ~14dp), rounded display corners (radius ~40dp).

---

## 1. Xiaomi 13 Physical Touch & Pill Calibration

On Xiaomi 13 running HyperOS 3 full-screen gesture navigation:
- **Display Resolution:** 1080 px width × 2400 px height.
- **Density Bucket:** 2.75× (`xxhdpi`).
- **Pill Overlay Dimensions:** Calibrated to **160dp width × 24dp height** (`440px × 66px`).
- **Placement:** Positioned at `y = 0` (screen bottom), centered horizontally.
- **Free Lateral Margins:** 
  - Left edge margin: `320px` (~116dp).
  - Right edge margin: `320px` (~116dp).
  - Both lateral margins are 100% free and unintercepted, allowing native HyperOS Back gestures (edge inwards swipes) and lateral app switches to operate with zero delay.

---

## 2. Upward Gesture Navigation Delegation (Home & Recents)

When a user initiates an upward navigation swipe from inside the bottom pill area:
1. `NavigationGestureService` detects upward displacement `dy > 3.0 * touchSlop` and cancels the hold timer.
2. Upon finger release (`ACTION_UP`):
   - **Quick Swipe Up (`elapsed < 280ms`):** Dispatches native `performGlobalAction(GLOBAL_ACTION_HOME)`.
   - **Swipe Up & Pause (`elapsed >= 280ms`, `dy > 5.0 * touchSlop`):** Dispatches native `performGlobalAction(GLOBAL_ACTION_RECENTS)`.
   - **Stationary Hold (`elapsed >= holdThresholdMs`, no displacement):** Dispatches haptic click and activates **Shovo Circle Search**.
   - **Micro-tap (< hold threshold, no displacement):** Resets state cleanly without synthetic navigation.

This prevents navigation dead-zones in the center of the navigation indicator.

---

## 3. Landscape & Fullscreen Media Optimization

1. **Auto-Orientation Synchronization:**  
   When capturing full-screen media (e.g. landscape video in YouTube or games), `CircleSearchActivity` inspects the captured bitmap dimensions:
   - If `bmp.width > bmp.height`: Requests `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE`.
   - If `bmp.width <= bmp.height`: Requests `ActivityInfo.SCREEN_ORIENTATION_PORTRAIT`.
   - `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"` declared in manifest prevents view destruction and squashed images during orientation shifts.
2. **Cutout & Punch Hole Insets:**  
   `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` renders edge-to-edge under the Xiaomi 13 centered punch hole without letterboxing or coordinate shifts.
3. **Micro-Tap Crop Auto-Expansion:**  
   If a user taps or draws a tiny dot (< 48dp / 130px), `CircleCanvasView` automatically expands the bounding box to a minimum searchable dimension (140px × 140px) centered on the touch point. This avoids Google Lens/Bing "Image too small" analysis failures.

---

## 4. HyperOS 3 China ROM System Constraints & Solutions

1. **HyperOS AppsFilter Interaction Blocking:**  
   - Declared explicit `<queries>` for `com.google.android.googlequicksearchbox`, `com.google.android.apps.photos`, `com.microsoft.bing`, and `com.yandex.searchapp`.
2. **Google Omni Hardware Whitelist (Flag `45631784`):**  
   - Google blocks native Omni popups on unrooted Xiaomi China ROMs.
   - Handled via `LensSearchHelper` multi-strategy dispatch (`LensShareEntryPointActivity` with `putParcelableArrayListExtra`, `SEARCH_IMAGE`, and `ACTION_SEND`).
3. **MIUI Battery Saver & Process Persistence:**  
   - Accessibility services bound by Android `system_server` receive `PROC_STATE_PERSISTENT`, exempting them from Xiaomi's background freeze.
