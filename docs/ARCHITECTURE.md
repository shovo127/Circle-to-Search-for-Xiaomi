# System Architecture: Circle to Search for Xiaomi

**Target Platform:** Xiaomi 13 (HyperOS 3 / Android 16)  
**Package:** `dev.shovo.circlesearch`  

---

## 1. Architectural Layers

```mermaid
flowchart TD
    subgraph Trigger["Layer 1: Dual Activation Pipeline"]
        P1["NavigationGestureService (TYPE_ACCESSIBILITY_OVERLAY - Bottom Hold)"]
        P2["ShovoVoiceInteractionService (Official AOSP Assistant - Power Button / Back Tap)"]
        P3["QuickSearchTileService (Quick Settings Tile)"]
    end

    subgraph Capture["Layer 2: Zero-Prompt Display Capture"]
        P1 -->|takeScreenshot| ACC["AccessibilityService.takeScreenshot()"]
        P2 -->|onHandleScreenshot| ASSIST["AOSP System Capture (VoiceInteractionSession)"]
        P3 --> P1
        ACC --> BMP["HardwareBuffer -> ARGB_8888 Bitmap"]
        ASSIST --> BMP
        BMP --> SSM["SearchSessionManager (Request-Scoped Session)"]
    end

    subgraph UI["Layer 3: Interactive Selection Overlay"]
        SSM --> ACT["CircleSearchActivity (Auto-Orientation & Cutout Sync)"]
        ACT --> CAN["CircleCanvasView (Dual-Tone Glowing Lasso & Auto-Expansion)"]
        CAN -->|Touch Up| CROP["Crash-Proof Bitmap Crop Engine"]
    end

    subgraph Dispatch["Layer 4: Visual Search Dispatch"]
        CROP --> FP["FileProvider (Private Cache with 5m TTL)"]
        FP --> DISP["LensSearchHelper"]
        DISP --> GMS["Google Lens (Direct Entry / Search Action / Standalone)"]
        DISP --> BING["Microsoft Bing App (Honest Intent Check)"]
        DISP --> YAN["Yandex Search App (Honest Intent Check)"]
        DISP --> CHOOSE["System App Chooser Fallback"]
    end
```

---

## 2. Key Subsystems
1. **`ShovoVoiceInteractionService` & `ShovoVoiceSession` (Official Assistant):** Registers Circle to Search for Xiaomi as an official Android Digital Assistant, delivering zero-latency system screenshots via `onHandleScreenshot` without accessibility service permissions.
2. **`NavigationGestureService` (Accessibility Overlay):** Manages the calibrated 160dp × 24dp transparent pill at screen bottom. Detects stationary long press and delegates upward swipes to Home and Recents.
3. **`SearchSessionManager`:** Thread-safe singleton providing unique session IDs, strict bitmap ownership, and automatic memory cleanup.
4. **`CircleSearchActivity` & `CircleCanvasView`:** Edge-to-edge transparent activity with dynamic landscape/portrait synchronization, cutout avoidance, Material 3 close button, and dual-tone glowing stroke.
5. **`LensSearchHelper`:** Crops selected bounds with memory bounds safety, writes JPEG to private cache, and dispatches to Google Lens, Bing, Yandex, or System Chooser.
