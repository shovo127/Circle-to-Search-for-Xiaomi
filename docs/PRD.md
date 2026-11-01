# Product Requirements Document (PRD)

## 1. Executive Summary
Circle to Search for Xiaomi brings a Samsung/Pixel-grade contextual search experience to the Xiaomi 13 (HyperOS 3 China ROM / Android 16) without requiring root, bootloader unlocking, or custom ROMs.

## 2. Target Persona & User Journey
- **User**: Xiaomi 13 user navigating any application (Chrome, YouTube, Gallery, Social Media, Documents).
- **Core Need**: Immediately select visual elements on screen and query Google Lens or visual search engines without taking manual screenshots, cropping in gallery, or switching apps.

## 3. Product Scope & Functional Requirements
1. **Activation (3 Modes)**:
   - *Primary*: Press and hold bottom navigation indicator for 450 ms.
   - *Secondary*: Back Tap (via Assistant integration).
   - *Tertiary*: Long-press Power button (via Digital Assistant role).
2. **Overlay Capture**:
   - Zero-latency capture using Android 16 Accessibility Screenshot API.
   - Immediate freeze-frame translucent overlay over active app.
3. **Selection UX**:
   - Freeform circle/lasso drawing or box selection.
   - Glowing cyan visual feedback with haptic pulses.
   - Bounding box auto-calculation and boundary enforcement.
4. **Visual Search Engines**:
   - Priority 1: Google Lens (via Google App).
   - Priority 2: Fallback multi-engine visual search (Bing / Yandex).
   - Offline text/OCR and QR code extraction.
5. **Dismissal & Lifecycle**:
   - Back button or tap outside selection immediately dismisses overlay without breaking the underlying application's state.

## 4. Non-Functional Requirements
- **Performance**: Activation to overlay display under 200 ms.
- **Battery**: Zero idle background CPU usage; touch listeners active only on the 495x88px navigation pill.
- **Privacy**: No external telemetry, no remote screenshot storage; temporary crop files auto-deleted after 5 minutes.

