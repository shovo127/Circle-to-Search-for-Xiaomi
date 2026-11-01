# Architecture Decision Records (ADR)

## ADR 01: Touch Interception Architecture
- **Decision**: Use `TYPE_ACCESSIBILITY_OVERLAY` for the bottom-center gesture detection zone instead of `TYPE_APPLICATION_OVERLAY` (`SYSTEM_ALERT_WINDOW`).
- **Alternatives Considered**:
  1. `SYSTEM_ALERT_WINDOW`: Fails to receive touches reliably without stealing full-screen input, triggers constant Xiaomi floating window warnings, and blocks underlying navigation bar gestures.
  2. `VoiceInteractionService` alone: Fails on HyperOS China ROM unless set as default assistant, which conflicts with XiaoAI system functions.
- **Evidence**: Verified in `h3nc4/HyperGesture`. HyperOS grants `TYPE_ACCESSIBILITY_OVERLAY` direct input pipeline access without popup permission prompts.
- **Risk Mitigation**: The touch zone is calibrated strictly to the navigation handle area (`180dp x 32dp`), leaving the left and right edges open for system back gestures.

---

## ADR 02: Screen Capture Pipeline on Android 16 (API 36)
- **Decision**: Employ `AccessibilityService.takeScreenshot(Display.DEFAULT_DISPLAY, ...)` as the primary capture engine.
- **Alternatives Considered**:
  1. `MediaProjection API`: Requires Android 14+ user consent dialog on every invocation and foreground service type `mediaProjection`, destroying the seamless "hold and search" interaction.
  2. Shell `screencap`: Requires ADB / root, unavailable to standalone non-root applications.
- **Evidence**: Android 16 accessibility service capability `canTakeScreenshot="true"` captures the raw hardware buffer with ~40ms latency.
- **Risk Mitigation**: Buffer is copied to software ARGB_8888 bitmap immediately and `hardwareBuffer.close()` is executed in a mandatory `finally` block to prevent GPU VRAM leaks.

---

## ADR 03: Visual Search & Fallback Strategy
- **Decision**: Implement a cascading visual search dispatcher:
  1. Priority 1: Google Lens direct intent via `com.google.android.apps.photos.SEARCH_IMAGE` on `com.google.android.googlequicksearchbox`.
  2. Priority 2: Google Lens Share Entry Activity `com.google.android.apps.search.lens.LensShareEntryPointActivity` with `ArrayList<Uri>` parcelable extra.
  3. Priority 3: Multi-engine web search (Bing Visual Search / Yandex Images) via browser dispatch if Google App is disabled or blocked.
- **Alternatives Considered**: Direct Google Omni intent (`com.google.android.apps.search.omnient`). Rejected because Google blocks non-Pixel/Samsung hardware via flag `45631784`.
- **Evidence**: Audited across `MiCTS`, `CTSLauncher`, and device dumpsys.
- **Risk Mitigation**: All URIs are strictly shared via `androidx.core.content.FileProvider` with temporary read-only permissions and automatic deletion after 5 minutes.

