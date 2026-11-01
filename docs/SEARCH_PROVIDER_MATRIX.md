# Search Provider Functionality Matrix

**Target Application:** Shovo Circle Search (`dev.shovo.circlesearch`)  
**Architecture Policy:** Honest Provider Protocol — Never fake visual search with blank browser landing pages.

---

## 1. Provider Capabilities & Dispatch Contract

| Provider | Target Package | Supported Image Dispatch Flow | Missing App Behavior |
| :--- | :--- | :--- | :--- |
| **Google Lens** | `com.google.android.googlequicksearchbox` | `LensShareEntryPointActivity` / `SEARCH_IMAGE` / `ACTION_SEND` | Falls back to System App Chooser. |
| **Microsoft Bing** | `com.microsoft.bing` | `ACTION_SEND` with `FLAG_GRANT_READ_URI_PERMISSION` | Displays `EngineNotInstalled` prompt. *(No fake web landing)*. |
| **Yandex Search** | `com.yandex.searchapp` | `ACTION_SEND` with `FLAG_GRANT_READ_URI_PERMISSION` | Displays `EngineNotInstalled` prompt. *(No fake web landing)*. |
| **System Chooser** | System Framework | `Intent.createChooser(ACTION_SEND)` | Dispatches image to any installed OCR / visual search app. |

---

## 2. Dispatch Result State Machine
Every visual search dispatch in `LensSearchHelper` returns an explicit sealed result:
```kotlin
sealed class DispatchResult {
    data class Success(val providerName: String) : DispatchResult()
    data class EngineNotInstalled(val engineName: String, val targetPackage: String) : DispatchResult()
    data class Failed(val reason: String) : DispatchResult()
}
```
If an engine is selected whose application is not installed on the Xiaomi 13, the app displays a clear notification explaining that the application is required to process direct visual search, guiding the user to install the app or use Google Lens / System Chooser.
