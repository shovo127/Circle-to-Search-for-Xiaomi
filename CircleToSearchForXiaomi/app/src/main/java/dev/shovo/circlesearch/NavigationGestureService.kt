package dev.shovo.circlesearch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Accessibility capture service for Circle to Search for Xiaomi.
 *
 * No navigation touch overlay is installed. Consuming ACTION_DOWN through an accessibility
 * overlay cannot safely replay Android's native Home/Recents gesture stream. Quick Settings
 * Tile is the reliable no-root activation path.
 */
class NavigationGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "NavGestureService"
        var instance: NavigationGestureService? = null
            private set

        const val EXTRA_SESSION_ID = "dev.shovo.circlesearch.EXTRA_SESSION_ID"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val captureInProgress = AtomicBoolean(false)

    @Volatile
    private var serviceDestroyed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceDestroyed = false
        instance = this

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = 0
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info

        migrateLegacyActivation()
        Log.i(TAG, "Capture service connected; no navigation touch overlay is installed.")
    }

    private fun migrateLegacyActivation() {
        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        val current = prefs.getString("pref_activation_method", null)
        if (current != "QS_TILE" && current != "ASSISTANT") {
            prefs.edit().putString("pref_activation_method", "QS_TILE").apply()
            if (current == "BOTTOM_HOLD") {
                Log.w(TAG, "Migrated unsafe legacy bottom-hold mode to Quick Settings Tile.")
            }
        }
    }

    fun updateGestureZoneConfiguration() {
        migrateLegacyActivation()
        Log.i(TAG, "Navigation overlay remains disabled to preserve native system gestures.")
    }

    fun triggerCircleToSearch() {
        if (serviceDestroyed) {
            notifyCaptureFailure("Accessibility service is not connected.")
            return
        }
        if (!captureInProgress.compareAndSet(false, true)) {
            Log.w(TAG, "Screenshot capture already in flight; duplicate trigger ignored.")
            return
        }

        val session = SearchSessionManager.createSession()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            captureInProgress.set(false)
            SearchSessionManager.failCapture(session.id, "Requires Android 11+ (API 30+).")
            SearchSessionManager.releaseSession(session.id)
            notifyCaptureFailure("Requires Android 11 or newer.")
            return
        }

        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        captureInProgress.set(false)
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val bitmap = try {
                            Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)?.let { wrapped ->
                                wrapped.copy(Bitmap.Config.ARGB_8888, false)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy screenshot buffer: ${e.message}", e)
                            null
                        } finally {
                            try {
                                hardwareBuffer.close()
                            } catch (_: Exception) {
                                // The framework may already have closed this buffer.
                            }
                        }

                        if (serviceDestroyed) {
                            bitmap?.let { if (!it.isRecycled) it.recycle() }
                            SearchSessionManager.failCapture(session.id, "Accessibility service disconnected.")
                            SearchSessionManager.releaseSession(session.id)
                            return
                        }

                        if (bitmap != null && !bitmap.isRecycled) {
                            SearchSessionManager.completeCapture(session.id, bitmap)
                            launchOverlayActivity(session.id)
                        } else {
                            SearchSessionManager.failCapture(session.id, "Could not decode the captured display.")
                            SearchSessionManager.releaseSession(session.id)
                            notifyCaptureFailure("Unable to process the screen capture.")
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        captureInProgress.set(false)
                        val reason = when (errorCode) {
                            AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR -> "Internal screenshot error"
                            AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "Screenshot access denied"
                            AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "Capture rate limit reached"
                            AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "Invalid display"
                            else -> "Screenshot blocked by app security or system policy"
                        }
                        Log.w(TAG, "takeScreenshot failed ($errorCode): $reason")
                        SearchSessionManager.failCapture(session.id, reason)
                        SearchSessionManager.releaseSession(session.id)
                        if (!serviceDestroyed) notifyCaptureFailure(reason)
                    }
                }
            )
        } catch (e: Exception) {
            captureInProgress.set(false)
            Log.e(TAG, "takeScreenshot threw an exception: ${e.message}", e)
            SearchSessionManager.failCapture(session.id, e.message ?: "Screenshot exception")
            SearchSessionManager.releaseSession(session.id)
            if (!serviceDestroyed) notifyCaptureFailure("Screenshot failed: ${e.message}")
        }
    }

    private fun launchOverlayActivity(sessionId: String) {
        if (serviceDestroyed) {
            SearchSessionManager.releaseSession(sessionId)
            return
        }
        try {
            val intent = Intent(this, CircleSearchActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch selection overlay: ${e.message}", e)
            SearchSessionManager.failCapture(sessionId, "Activity launch failed: ${e.message}")
            SearchSessionManager.releaseSession(sessionId)
            notifyCaptureFailure("Could not open the selection screen.")
        }
    }

    private fun notifyCaptureFailure(reason: String) {
        handler.post {
            if (!serviceDestroyed) {
                Toast.makeText(this, "Circle to Search: $reason", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted.")
    }

    override fun onDestroy() {
        serviceDestroyed = true
        captureInProgress.set(false)
        handler.removeCallbacksAndMessages(null)
        if (instance === this) instance = null
        super.onDestroy()
        Log.i(TAG, "Accessibility capture service destroyed.")
    }
}
