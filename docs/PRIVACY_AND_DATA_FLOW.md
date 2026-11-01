# Privacy Architecture and Data Flow Analysis

## 1. Zero Cloud Dependency for Selection
The application never transmits user screenshots to any cloud service, third-party analytics provider, or external server. All screenshot capture, display, coordinate mapping, and image cropping are executed 100% on-device in local memory.

---

## 2. Data Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant NavigationService as NavigationGestureService
    participant OverlayActivity as CircleSearchActivity
    participant Helper as LensSearchHelper
    participant TargetApp as Google Lens / External App

    User->>NavigationService: Hold Bottom Navigation Pill (450ms)
    NavigationService->>NavigationService: takeScreenshot(Display.DEFAULT_DISPLAY)
    NavigationService->>OverlayActivity: Pass in-memory Bitmap
    OverlayActivity->>User: Display frozen screen overlay
    User->>OverlayActivity: Draws circle around content
    OverlayActivity->>Helper: Crop bounding box
    Helper->>Helper: Save JPEG to internal cacheDir/crops/
    Helper->>TargetApp: Grant temporary READ_URI_PERMISSION & launch Intent
    TargetApp->>User: Display visual search results
    Helper->>Helper: Purge crops older than 5 minutes
```

---

## 3. Data Retention Policy
- Screenshots in memory: Recycled immediately when `CircleSearchActivity` is destroyed.
- Cropped files on disk: Saved to `Context.cacheDir/crops/`. Evaluated on every run and deleted if timestamp > 300 seconds.
- User search queries: Never logged or stored.

