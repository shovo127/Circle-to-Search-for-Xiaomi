# Root Cause Analysis: Lifecycle Regressions & Investigation

**Target Issue:** "Why the application worked once on the previous night without root and then stopped working."  
**Investigation Scope:** Git commit history (`1f865f0` → `620b5f0` → `c3c7de9`), decompiled runtime flow, HyperOS process management.

---

## 1. Executive Finding
The failure was not caused by a single bug, but by a cascade of 4 interacting architectural flaws:
1. **The Recursive Screenshot Relaunch Loop** (Primary Crash Cause).
2. **Global Static Mutable Bitmap State** (Memory & Lifecycle Desync).
3. **Synthetic Touch Injection Interfering with HyperOS Full-Screen Gestures** (UI Lockup).
4. **Intent Extra Type Conflict & False Search Engine Stubs** (Dispatch Crash & Fake Search).

---

## 2. Detailed Root-Cause Breakdown

### A. The Recursive Screenshot Relaunch Loop
In commit `620b5f0`:
1. `NavigationGestureService.takeScreenshot()` invoked the system capture callback.
2. If `onFailure(errorCode)` was returned (e.g. rate limit, display orientation transition, or secure screen), the service called `launchOverlayActivity()`!
3. `CircleSearchActivity.onCreate()` invoked `loadScreenshotOrRequest()`.
4. Because `currentScreenshot` was null, it executed:
   ```kotlin
   finish()
   service.triggerCircleToSearch()
   ```
5. This immediately triggered `takeScreenshot()` again, which failed again, launching the Activity again, which called `triggerCircleToSearch()` again.
6. Result: An uncontrollable rapid relaunch loop that locked up the UI thread, drained CPU, and triggered Android system watchdog termination.

### B. Global Static Mutable Bitmap State
`currentScreenshot` was held as a public companion object variable on `CircleSearchActivity`:
- When Android destroyed or recreated `CircleSearchActivity` under low memory or configuration change (screen rotation), `currentScreenshot` was either recycled or null.
- If the user triggered search a second time, the activity either attempted to draw a recycled bitmap (throwing `RuntimeException: Canvas: trying to use a recycled bitmap`) or displayed a black screen.

### C. Synthetic Touch Injection Ruining HyperOS Navigation
In `NavigationGestureService`, when the bottom touch overlay received an `ACTION_UP` or quick flick that didn't meet the hold duration, it attempted heuristic simulation:
```kotlin
if (dy > touchSlop * 2.5f) {
    performGlobalAction(GLOBAL_ACTION_HOME)
}
```
HyperOS handles full-screen navigation gestures at the hardware compositor level. Intercepting the raw touch stream and injecting a simulated `GLOBAL_ACTION_HOME` caused gesture conflicts, dropped frames, and frozen navigation bars when returning to home apps like Nova or MIUI Launcher.

### D. Google Intent Extra Overwrite & Bing/Yandex Web Stubs
1. In `LensSearchHelper`:
   ```kotlin
   putExtra(Intent.EXTRA_STREAM, imageUri)
   putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri))
   ```
   Writing the same key `EXTRA_STREAM` twice in succession caused type clobbering.
2. Bing and Yandex visual search merely opened `https://www.bing.com/visualsearch` without passing the cropped image URI. The user saw a browser landing page rather than visual search results.

---

## 3. The Definitive Fix in Shovo Circle Search
- **SearchSessionManager:** Introduces isolated `Session` objects with unique UUIDs.
- **Loop Prevention:** On screenshot failure, `NavigationGestureService` posts a clean user Toast and **NEVER** launches the activity.
- **No Recursive Call:** If `CircleSearchActivity` receives no active bitmap, it displays a polite notification and exits cleanly.
- **Natural Touch Handling:** Removed synthetic `performGlobalAction` injection. If a gesture is cancelled, the overlay resets state cleanly.
- **Truthful Search Dispatcher:** Dispatches image URIs to verified installed apps (Google Lens, Bing App, Yandex App, or System Chooser) with `FLAG_GRANT_READ_URI_PERMISSION`.

