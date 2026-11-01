# Feature-by-Feature Comparative Evaluation

| Evaluation Criteria | HyperGesture | MiCTS | CTSLauncher | AKS-Labs CTS | Circle to Search for Xiaomi |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Xiaomi HyperOS Compatibility** | High (5/5) | Low (2/5 unrooted) | Low (1/5) | Medium (3/5) | **High (5/5)** |
| **Android 16 (API 36) Support** | Medium (3/5) | Medium (3/5) | Low (2/5) | High (4/5) | **High (5/5)** |
| **Bottom Navigation Bar Hold** | Yes (Custom) | LSPosed only | No | No | **Yes (Native Overlay)** |
| **Navigation Swipes Preserved** | Yes (5/5) | N/A | N/A | N/A | **Yes (5/5)** |
| **Native Zero-Consent Capture** | No | No | No | Yes (AS API) | **Yes (Accessibility)** |
| **Freeform Circle Selection** | No | No | No | Yes | **Yes (Cyan Glow HUD)** |
| **Multi-Engine Visual Search** | No | No | No | Yes (Google/Bing/Yandex) | **Yes (Google/Bing/Yandex)** |
| **Offline OCR / Text Extraction** | No | No | No | Yes | **Yes (ML Kit / Offline)** |
| **QR & Barcode Recognition** | No | No | No | Yes | **Yes (Automatic)** |
| **Three Activation Methods** | No | Yes (Root) | No | Yes | **Yes (Configurable)** |
| **No-Root Requirement** | Yes | No (For Omni) | Yes | Yes | **Yes (100% Non-Root)** |
| **Privacy & Storage Cleanup** | High | Medium | High | High | **High (Auto-Purge 5m)** |
| **Licensing Compliance** | GPL-3.0 | GPL-3.0 | GPL-3.0 | GPL-3.0 | **GPL-3.0 Compliant** |

---

## Evaluation Rationale

1. **Gesture Accuracy**: HyperGesture's state machine is the only implementation that successfully coexists with Xiaomi HyperOS 3 full-screen navigation without requiring root or custom launchers.
2. **Search Independence**: AKS-Labs demonstrated that Google Lens alone is vulnerable to OEM firmware lockouts. Implementing a multi-engine fallback (Google Lens -> Bing Visual -> Yandex Images) guarantees that the user always gets search results regardless of Google account or ROM restrictions.
3. **Capture Reliability**: Android 16 requires strict lifecycle adherence (`HardwareBuffer.close()`) to avoid graphical crashes. Our implementation wraps the buffer transfer safely.

