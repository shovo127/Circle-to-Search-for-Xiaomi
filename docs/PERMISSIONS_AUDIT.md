# Permissions Audit Report

## 1. Declared Permissions

| Permission | Protection Level | Purpose & Necessity | Minimum SDK |
| :--- | :--- | :--- | :---: |
| `android.permission.VIBRATE` | Normal | Haptic pulses during bottom-hold threshold and circle drawing | 1 |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Signature | Required to register `NavigationGestureService` in Android framework | 4 |
| `android.permission.BIND_QUICK_SETTINGS_TILE` | Signature | Required for `QuickSearchTileService` in notification shade | 24 |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Reconnect service after device reboot | 1 |

## 2. Unnecessary Permissions Explicitly Removed / Avoided
- `SYSTEM_ALERT_WINDOW`: Not needed because `TYPE_ACCESSIBILITY_OVERLAY` is granted by virtue of accessibility service binding.
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`: Not needed; all crops use app-internal `cacheDir`.
- `FOREGROUND_SERVICE`: Not needed; the accessibility service is managed as a persistent system service.
- `INTERNET`: Visual search is delegated to external installed apps (Google App / Browser), so the core app requires no direct network socket permissions for screenshot capture or gesture recognition.

