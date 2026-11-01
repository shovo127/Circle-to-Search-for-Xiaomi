# Google Circle to Search & Lens Integration Architecture

**Component:** `dev.shovo.circlesearch.LensSearchHelper`  
**Compatibility Layer:** Multi-strategy Google App & Google Photos Intent Dispatcher  

---

## 1. Native Omni Entry Point Investigation
- **The Omni Mechanism:** Google's proprietary Circle to Search implementation is backed by internal GMS flag `45631784` and `com.google.android.googlequicksearchbox.omni.entry_point`.
- **Hardware Whitelist Reality:** On non-Pixel and non-Samsung devices (such as Xiaomi 13 China ROM), GMS rejects unrooted Omni Binder invocations (`Omni invocation failed: not enabled`).
- **Zero-Root Engineering Decision:** Rather than requiring LSPosed hooks to spoof Pixel device profiles, Shovo Circle Search implements a **guarded compatibility adapter** targeting Google Lens's high-speed image processing pipelines.

---

## 2. Dispatch Pipeline Strategies

```mermaid
flowchart TD
    IMG["Cropped Bitmap (JPEG, 95% Quality)"] --> SAVE["Private Scoped Cache (cache/crops/)"]
    SAVE --> URI["FileProvider URI (dev.shovo.circlesearch.fileprovider)"]
    URI --> S1{"Strategy 1: LensShareEntryPointActivity"}
    S1 -->|Resolved| GOOG1["Google Lens UI"]
    S1 -->|Failed| S2{"Strategy 2: SEARCH_IMAGE Action"}
    S2 -->|Resolved| GOOG2["Google App Visual Search"]
    S2 -->|Failed| S3{"Strategy 3: Google App Send Intent"}
    S3 -->|Resolved| GOOG3["Google App Image Handler"]
    S3 -->|Failed| S4["Strategy 4: System App Chooser"]
```

---

## 3. Intent Security & Permissions
- All Google intents include `FLAG_GRANT_READ_URI_PERMISSION`.
- Exact clip data attached via `ClipData.newRawUri("image", imageUri)` ensuring Android 16 Scoped Storage compliance.
- Android Manifest declares `<queries>` for `com.google.android.googlequicksearchbox` and `com.google.android.apps.photos` to bypass HyperOS package filtering.
