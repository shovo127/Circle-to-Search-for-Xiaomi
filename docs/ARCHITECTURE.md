# System Architecture: Shovo Circle Search

**Target Platform:** Xiaomi 13 (HyperOS 3 / Android 16)  
**Package:** `dev.shovo.circlesearch`  

---

## 1. Architectural Layers

```mermaid
flowchart TD
    subgraph Trigger["Layer 1: Activation & Input"]
        P1["NavigationGestureService (TYPE_ACCESSIBILITY_OVERLAY)"]
        P2["Back Tap Shortcut / ACTION_ASSIST"]
        P3["QuickSearchTileService"]
    end

    subgraph Session["Layer 2: Session & Capture Lifecycle"]
        P1 & P2 & P3 --> SSM["SearchSessionManager"]
        SSM --> ACC["AccessibilityService.takeScreenshot()"]
        ACC -->|HardwareBuffer| BMP["ARGB_8888 Bitmap (Session Scoped)"]
    end

    subgraph UI["Layer 3: Interactive Selection Overlay"]
        BMP --> ACT["CircleSearchActivity"]
        ACT --> CAN["CircleCanvasView (Freeform Lasso & Box)"]
        CAN -->|Touch Up| CROP["Bitmap Crop Engine"]
    end

    subgraph Dispatch["Layer 4: Visual Search Dispatch"]
        CROP --> FP["FileProvider (cache/crops/)"]
        FP --> DISP["LensSearchHelper"]
        DISP --> GMS["Google Lens"]
        DISP --> BING["Microsoft Bing App"]
        DISP --> YAN["Yandex Search App"]
        DISP --> CHOOSE["System App Chooser"]
    end
```

---

## 2. Key Subsystems
1. **`NavigationGestureService`:** Manages the transparent overlay pill (160dp × 24dp) at `y = 0`. Detects stationary hold while letting swipe navigation pass without synthetic injection.
2. **`SearchSessionManager`:** Thread-safe singleton providing unique session IDs, strict bitmap ownership, and automatic memory cleanup.
3. **`CircleSearchActivity` & `CircleCanvasView`:** Edge-to-edge transparent activity displaying the captured frame, supporting dynamic multi-touch gestures, display cutouts, and haptic feedback.
4. **`LensSearchHelper`:** Crops selected bounds, writes JPEG to private cache, generates FileProvider URI with temporary read permissions, and dispatches to the selected visual engine.
