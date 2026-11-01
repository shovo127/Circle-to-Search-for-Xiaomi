# Technical Requirements Document (TRD)

## 1. Technical Architecture & Component Model
- **Language**: Kotlin 2.0+ (strict nullability, coroutines).
- **Target SDK**: Android 16 (API 36).
- **Min SDK**: Android 8.0 (API 26).
- **Architecture Pattern**: Clean Architecture with Service-Overlay decoupling and unidirectional state flow.

## 2. Core Subsystems

### A. Navigation Gesture Daemon (`NavigationGestureService`)
- Extends `android.accessibilityservice.AccessibilityService`.
- Capability requirements: `canTakeScreenshot="true"`, `canRetrieveWindowContent="true"`.
- Uses `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` to register a 180dp x 32dp pill centered at bottom (`y=0`).
- Manages gesture state machine:
  - Down: start `holdRunnable` delayed by `holdThresholdMs`.
  - Move: calculate `dy = downY - event.rawY`. If `dy > touchSlop * 3.5f`, mark swipe detected, cancel hold timer, delegate navigation.
  - Up/Cancel: if hold triggered, do nothing. If swipe detected, invoke `performGlobalAction(GLOBAL_ACTION_HOME)` or `RECENTS`.

### B. Capture & Crop Subsystem (`LensSearchHelper`)
- Receives `ScreenshotResult` from `takeScreenshot()`.
- Unwraps `HardwareBuffer`, clones to software `ARGB_8888` bitmap, and executes mandatory `hardwareBuffer.close()`.
- Calculates canvas-to-bitmap scale ratios:
  `scaleX = bitmap.width / canvas.width`, `scaleY = bitmap.height / canvas.height`.
- Crops exact sub-rectangle using `Bitmap.createBitmap`.
- Compresses to JPEG (95% quality) into app internal cache `cacheDir/crops/`.
- Issues temporary grant URI via `androidx.core.content.FileProvider`.

### C. Visual Search Dispatcher
- Dispatches implicit and explicit intents with `FLAG_GRANT_READ_URI_PERMISSION` and `ClipData`.
- Fallbacks gracefully to secondary entry point or browser visual search.

