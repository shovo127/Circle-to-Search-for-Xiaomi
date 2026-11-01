# Data Schema and Storage Specification (Schema)

## 1. Preferences Schema (`SharedPreferences`: `cts_prefs`)

| Key Name | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `pref_activation_method` | `String` | `"BOTTOM_HOLD"` | Selected activation trigger (`"BOTTOM_HOLD"`, `"BACK_TAP"`, `"POWER_LONG"`) |
| `pref_hold_threshold_ms` | `Long` | `450L` | Duration in milliseconds required to trigger bottom hold (Range: 300 - 750 ms) |
| `pref_haptic_feedback` | `Boolean` | `true` | Whether haptic pulses occur during hold and circle selection |
| `pref_search_engine` | `String` | `"GOOGLE_LENS"` | Preferred search provider (`"GOOGLE_LENS"`, `"BING"`, `"YANDEX"`) |
| `pref_auto_clean_crops` | `Boolean` | `true` | Automatically delete crop files older than 5 minutes |

---

## 2. File Storage Structure
- **Root Directory**: `Context.cacheDir`
- **Sub-directory**: `crops/`
  - File naming pattern: `search_crop_<timestamp>.jpg`
  - File permissions: Private (`MODE_PRIVATE`), shared only through FileProvider authority `dev.shovo.circlesearch.fileprovider`.
  - Retention policy: Purged on every save operation for items older than 300,000 ms.

