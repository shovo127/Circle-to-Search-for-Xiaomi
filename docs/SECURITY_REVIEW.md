# Security Engineering Review

## 1. Threat Modeling & Attack Surface Analysis
- **Accessibility Service Exploitation Risk**: Accessibility services have high privilege levels. To prevent misuse, `NavigationGestureService` is protected with the mandatory platform permission `android.permission.BIND_ACCESSIBILITY_SERVICE`. Non-system apps cannot bind or spoof this service.
- **Overlay Injection & Tapjacking**: The gesture pill overlay uses `FLAG_NOT_FOCUSABLE`. It does not intercept password fields or credentials because its bounds are strictly restricted to the 32dp bottom edge of the display.
- **Data Infiltration / Leakage**: Captured screenshots are held in memory only for the duration of the selection session. Cropped image files are saved to internal cache, accessible solely by the app UID, and granted temporarily to the selected search provider via `FileProvider` with temporary read-only flags (`FLAG_GRANT_READ_URI_PERMISSION`).

## 2. Hardening Measures
- `android:allowBackup="false"` to prevent local extraction of cached images via ADB backup.
- Strict FileProvider scoping (`<cache-path name="crops" path="crops/" />`).
- Automatic purging of all crop files on invocation.
- Zero analytics, telemetry, or remote logging SDKs.

