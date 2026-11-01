# Circle to Search for Xiaomi

Minimal native Google Circle to Search trigger for Xiaomi 13 / HyperOS 3 / Android 16.

The app is designed to be launched by **Double Back Tap**. Its launcher activity is transparent: when HyperOS launches the app, it immediately invokes Google's native Circle to Search through Android's Voice Interaction Manager (VIS) entry point and exits. It does not show a dashboard, create a second screenshot overlay, install an accessibility touch overlay, or replace the Power-button menu.

## Configure once

1. Make sure the Google app is installed and updated.
2. Set **Google** as the phone's default Digital Assistant. The app checks this and opens assistant settings if Google is not selected.
3. Open HyperOS **Settings → Additional settings → Gesture shortcuts → Back tap** (the exact menu label can vary by firmware).
4. Set **Press back twice / Double tap** to **Open app → Circle to Search for Xiaomi**, if that action is offered by this firmware.
5. Double-tap the back of the phone. Google should display its native Circle to Search surface; circle or lasso the content there.

## Important compatibility boundary

The app uses the same public-source VIS invocation route documented by the upstream MiCTS project. Rootless success depends on Google allowing Circle to Search on the device. If Google rejects the invocation (Omni invocation failed: not enabled / the app reports that Google rejected it), a normal third-party APK cannot change Google's device-eligibility flag or whitelist. That is a Google-side restriction, not a gesture or screenshot bug.

Back Tap itself is owned by HyperOS. The app can expose a launchable activity, but it cannot add an unsupported Open app action to a firmware Back Tap menu that does not provide one.

## Build

- Application ID: dev.shovo.circlesearch
- Version: 1.1.0 (versionCode 4)
- Minimum Android: 9 (API 28)
- Target / compile SDK: 36
- No app-side accessibility service, overlay permission, screenshot permission, Quick Settings tile, Power Assistant service, or long-press Power handling.
