# Circle to Search Ecosystem Research Matrix

## 1. Candidate Repositories Audited

| Repository | Origin & Author | Target Android | Core Mechanism | Status Verified |
| :--- | :--- | :--- | :--- | :--- |
| **HyperGesture** | `h3nc4/HyperGesture` | Android 10 - 15 (HyperOS) | `TYPE_ACCESSIBILITY_OVERLAY` bottom pill + touch state machine | Source & Device Verified |
| **MiCTS** | `parallelcc/MiCTS` | Android 9 - 16 | `VoiceInteractionService` (VIS), `CSHelper`, `CSService` | Source Verified (Requires LSPosed/root for Omni spoofing on China ROM) |
| **CTSLauncher** | `chenxiaolong/CTSLauncher` | Android 14 - 15 | Assistant Intent & Quick Settings Tile | Source Verified (Pass-through to Google App, fails if unwhitelisted) |
| **AKS CircleToSearch** | `AKS-Labs/CircleToSearch` | Android 10 - 15+ | `AccessibilityService` screenshot + Multi-engine visual search + offline OCR | Source Verified |
| **DirectLens** | `Bankairim/DirectLens` | Android 11+ | Direct Google Lens entry point via `ACTION_SEND` / `SEARCH_IMAGE` | Source Verified |
| **QuickLens** | `quantavil/QuickLens` | Android 11+ | Hybrid Accessibility + VoiceInteraction, Room search history | Source Verified |
| **Native Project** | `shovo127/Circle-to-Search-for-Xiaomi` | Android 16 (API 36) | Calibrated `TYPE_ACCESSIBILITY_OVERLAY` + Native Android 16 Screenshot + Multi-engine Lens helper | Source & Xiaomi 13 Verified |

---

## 2. Technical Findings Summary

1. **Native Google Omni Invocation on HyperOS China ROM**:
   - Google restricts native Omni Circle to Search using server-side flag `45631784` under package `com.google.android.apps.search.omnient.device`.
   - On unrooted Xiaomi 13 China ROM, attempting to trigger Omni directly results in logcat error: `Omni invocation failed: not enabled`.
   - **Conclusion**: Relying solely on Google's closed Omni service is fundamentally broken on unrooted HyperOS China ROM. An independent, high-performance visual search and lens pipeline is strictly required.

2. **HyperOS Gesture Overlays**:
   - Standard `TYPE_APPLICATION_OVERLAY` (`SYSTEM_ALERT_WINDOW`) lacks gesture synchronization and disrupts full-screen navigation gestures.
   - `TYPE_ACCESSIBILITY_OVERLAY` attached via an active `AccessibilityService` has system-level priority, allowing precise placement over the gesture bar without triggering MIUI permission security warnings during runtime touches.

3. **Android 16 Screenshot Capabilities**:
   - `AccessibilityService.takeScreenshot(Display.DEFAULT_DISPLAY, ...)` is the only zero-consent, low-latency method on API 36 that works without MediaProjection notification prompts.
   - Requires explicit lifecycle cleanup (`hardwareBuffer.close()`) to avoid GPU out-of-memory kernel panics.

