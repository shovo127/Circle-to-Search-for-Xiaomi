# Activation Methods Engineering Specification

**Target Hardware:** Xiaomi 13 (`fuxi`)  
**Firmware:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`, Android 16 / API 36)  

---

## 1. Activation Support Matrix

| Method | Implementation Mechanism | Status on Xiaomi 13 (HyperOS 3 China) | User Configuration Required |
| :--- | :--- | :---: | :--- |
| **A. Bottom-Center Hold** | `TYPE_ACCESSIBILITY_OVERLAY` gesture zone around navigation pill | **Fully Supported (Primary)** | Enable Shovo Circle Search Accessibility Service. |
| **B. Back Tap** | Xiaomi Gesture Shortcuts launching Assist / Activity | **System-Assisted (Secondary)** | Configure in HyperOS Settings → Additional Settings → Gestures → Back Tap. |
| **C. Power Button Long Press** | Digital Assistant Role routing (`ACTION_ASSIST`) | **Conditional (System Assistant)** | Configure in Settings → Apps → Default Apps → Digital Assistant (Subject to XiaoAI priority). |
| **D. Quick Settings Tile** | `QuickSearchTileService` (`QS_TILE`) | **Fully Supported (Fallback)** | Add "Shovo Search" tile to Quick Settings panel. |

---

## 2. Technical Details

### A. Bottom-Center Hold (Primary Native Method)
- **Geometry:** 160dp width × 24dp height centered horizontally at the very bottom screen edge (`y = 0`).
- **Hold Duration:** User-configurable from **300 ms to 750 ms** (default: **450 ms**).
- **Jitter Tolerance:** Cancels if upward motion `dy > 3.0 * touchSlop` or lateral motion `dx > 3.5 * touchSlop`.
- **Haptic Feedback:** Dedicated haptic click on trigger activation (configurable on/off).

### B. Back Tap (Secondary Method)
HyperOS provides double and triple tap back gestures on Xiaomi 13 hardware. The system allows binding this action to launch a shortcut or digital assistant. In Shovo Circle Search, selecting Back Tap displays the exact system setup path.

### C. Power Button Long Press
Android allows assigning long press of the power button to the Default Digital Assistant. Because HyperOS China ROM defaults this to Xiao AI (Mi AI), Shovo Circle Search provides explicit deep links to system settings to reassign the assistant role, without making impossible guarantees if the ROM restricts third-party assignment.

