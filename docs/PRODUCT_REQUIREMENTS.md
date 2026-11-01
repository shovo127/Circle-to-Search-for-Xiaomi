# Product Requirements Document (PRD)

**Product Name:** Circle to Search for Xiaomi  
**Package:** `dev.shovo.circlesearch`  
**Target Hardware:** Xiaomi 13 (`2211133C`, codename `fuxi`)  
**Firmware:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`, Android 16 / API 36)  
**Author & Product Owner:** Shovo (`@shovo127`)  

---

## 1. Product Vision
Deliver a standalone, production-grade, zero-root Circle to Search application specifically engineered for Xiaomi 13 on HyperOS 3 China ROM. The application provides an original branded experience, respects user privacy and system stability, and activates smoothly over any third-party app without breaking native full-screen gesture navigation.

---

## 2. Core Functional Requirements
1. **Primary Activation (Bottom-Center Hold):**
   - Press and hold the bottom navigation indicator for 450 ms (configurable 300–750 ms).
   - Zero interference with HyperOS Back, Home, and Recents swipes.
   - Distinct haptic feedback upon trigger activation.
2. **Secondary Activations:**
   - Back Tap shortcut integration via HyperOS system settings.
   - Long-press Power button via Android Digital Assistant role.
   - Quick Settings tile fallback.
3. **Screen Capture & Visual Selection:**
   - Zero-prompt screenshot capture via `AccessibilityService.takeScreenshot()`.
   - Freeform lasso drawing and rectangular bounding box.
   - Dynamic coordinate mapping between display resolution and captured bitmap.
4. **Multi-Engine Visual Search:**
   - Native Google Lens image sharing.
   - Real Microsoft Bing and Yandex Search image dispatch with honest installation status.
   - Universal system application chooser.
5. **Private Storage & Memory Lifecycle:**
   - Temporary crops stored in private app cache (`cache/crops/`).
   - Automatic cache expiration (5 minutes TTL) and manual one-tap purge.
   - Bitmap memory recycled immediately upon session completion.

