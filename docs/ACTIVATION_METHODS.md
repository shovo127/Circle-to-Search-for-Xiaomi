# Activation Methods Engineering Specification

**Target Hardware:** Xiaomi 13 (`fuxi`)  
**Firmware:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`, Android 16 / API 36)  

---

## 1. Activation Support Matrix

| Method | Implementation Mechanism | Status on Xiaomi 13 (HyperOS 3 China) | User Configuration Required |
| :--- | :--- | :---: | :--- |
| **A. Bottom-Center Hold** | `TYPE_ACCESSIBILITY_OVERLAY` gesture zone around navigation pill | **Fully Supported (Primary • Native)** | Enable Shovo Circle Search Accessibility Service. |
| **B. Back Tap** | Xiaomi Gesture Shortcuts launching Digital Assistant / Assist Activity | **System-Assisted (Secondary)** | Configure in HyperOS Settings → Additional Settings → Gestures → Back Tap. |
| **C. Power Button Long Press** | Official AOSP `VoiceInteractionService` (`onHandleScreenshot`) | **Fully Supported (Digital Assistant)** | Configure in Settings → Apps → Default Apps → Digital Assistant. |
| **D. Quick Settings Tile** | `QuickSearchTileService` (`QS_TILE`) | **Fully Supported (Fallback)** | Add "Shovo Search" tile to Quick Settings panel. |

---

## 2. Technical Details

### A. Bottom-Center Hold (Primary Native Method)
- **Geometry:** 160dp width × 24dp height centered horizontally at the very bottom screen edge (`y = 0`).
- **Hold Duration:** User-configurable from **300 ms to 750 ms** (default: **450 ms**).
- **Jitter Tolerance:** Cancels if upward motion `dy > 3.0 * touchSlop` or lateral motion `dx > 3.5 * touchSlop`.
- **Haptic Feedback:** Dedicated haptic click on trigger activation (configurable on/off).
- **Home/Recents Delegation:** If an upward swipe occurs from inside the pill, the service automatically delegates `performGlobalAction(GLOBAL_ACTION_HOME)` or `GLOBAL_ACTION_RECENTS`, preventing navigation dead zones.

### B. Back Tap (Secondary Method)
HyperOS provides double and triple tap back gestures on Xiaomi 13 hardware. The system allows binding this action to launch a shortcut or digital assistant. In Shovo Circle Search, selecting Back Tap displays the exact system setup path.

### C. Power Button Long Press (Official AOSP VoiceInteractionService)
By declaring a full `VoiceInteractionService` (`ShovoVoiceInteractionService`) and `VoiceInteractionSessionService` (`ShovoVoiceSessionService`) with `res/xml/voice_interaction_service.xml`, Shovo Circle Search is registered as an official Android Digital Assistant. When selected as Default Assistant:
- Long pressing the Power button natively triggers `ShovoVoiceSession`.
- The Android system captures the physical display buffer and delivers it to `onHandleScreenshot(Bitmap screenshot)` with zero accessibility latency.
- The session forwards the bitmap directly to `SearchSessionManager` and presents `CircleSearchActivity`.
