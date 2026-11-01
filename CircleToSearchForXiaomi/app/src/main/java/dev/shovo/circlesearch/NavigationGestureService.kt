package dev.shovo.circlesearch

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlin.math.abs

/**
 * Accessibility Service providing bottom-center hold detection for Shovo Circle Search
 * on Xiaomi HyperOS 3 / Android 16 (Xiaomi 13 'fuxi').
 *
 * Utilizes a TYPE_ACCESSIBILITY_OVERLAY touch zone calibrated over the navigation pill.
 * Accurately detects stationary long-press while preserving native full-screen gestures
 * (Back, Home, Recents) without heuristic synthetic touch injection.
 */
class NavigationGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "ShovoNavService"
        var instance: NavigationGestureService? = null
            private set

        const val EXTRA_SESSION_ID = "dev.shovo.circlesearch.EXTRA_SESSION_ID"
    }

    private var windowManager: WindowManager? = null
    private var pillHandleView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    private enum class GestureState {
        IDLE,
        HOLD_PENDING,
        HOLD_TRIGGERED,
        CANCELLED
    }

    private var gestureState = GestureState.IDLE
    private var isCapturing = false

    var holdThresholdMs: Long = 450L
        get() {
            val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
            return prefs.getLong("pref_hold_threshold_ms", 450L)
        }
        set(value) {
            field = value
            getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("pref_hold_threshold_ms", value)
                .apply()
        }

    private val holdRunnable = Runnable {
        if (gestureState == GestureState.HOLD_PENDING) {
            gestureState = GestureState.HOLD_TRIGGERED
            Log.d(TAG, "Hold duration reached ($holdThresholdMs ms) -> triggering Shovo Circle Search")
            vibrateHaptic()
            triggerCircleToSearch()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupBottomGestureZone()
        Log.i(TAG, "Shovo NavigationGestureService connected successfully on HyperOS 3 / Android 16")
    }

    fun updateGestureZoneConfiguration() {
        setupBottomGestureZone()
    }

    private fun setupBottomGestureZone() {
        if (pillHandleView != null) {
            try {
                windowManager?.removeView(pillHandleView)
            } catch (_: Exception) {}
            pillHandleView = null
        }

        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        val activationMethod = prefs.getString("pref_activation_method", "BOTTOM_HOLD")
        if (activationMethod != "BOTTOM_HOLD") {
            Log.i(TAG, "Bottom hold inactive (user preferred method: $activationMethod)")
            return
        }

        val displayMetrics = resources.displayMetrics
        // Navigation pill on Xiaomi 13 (fuxi) is ~160dp wide and ~20dp tall.
        // Calibrated 160dp width and 24dp height right at the bottom edge ensures reliable contact
        // while leaving screen corners and lateral sides 100% open for system gestures.
        val pillWidth = (160 * displayMetrics.density).toInt()
        val pillHeight = (24 * displayMetrics.density).toInt()
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        val params = WindowManager.LayoutParams(
            pillWidth,
            pillHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        pillHandleView = View(this).apply {
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        downTime = SystemClock.uptimeMillis()
                        gestureState = GestureState.HOLD_PENDING

                        handler.removeCallbacks(holdRunnable)
                        handler.postDelayed(holdRunnable, holdThresholdMs)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (gestureState == GestureState.HOLD_PENDING) {
                            val dx = abs(event.rawX - downX)
                            val dy = downY - event.rawY // positive when swiping upwards

                            // If finger moves intentionally beyond hold jitter tolerance, cancel hold.
                            // Deliberate upward navigation swipes exceed 3.0x touchSlop.
                            if (dy > touchSlop * 3.0f || dx > touchSlop * 3.5f) {
                                gestureState = GestureState.CANCELLED
                                handler.removeCallbacks(holdRunnable)
                                Log.d(TAG, "Touch exceeded jitter slop (dy=$dy, dx=$dx) -> cancelled hold timer")
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(holdRunnable)
                        val wasTriggered = (gestureState == GestureState.HOLD_TRIGGERED)
                        gestureState = GestureState.IDLE
                        wasTriggered
                    }

                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(pillHandleView, params)
            Log.d(TAG, "Bottom gesture zone registered: ${pillWidth}x${pillHeight}px at bottom-center")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register bottom gesture overlay: ${e.message}", e)
        }
    }

    /**
     * Executes non-root screenshot capture via Android AccessibilityService API.
     * Guaranteed:
     * - No recursive re-triggering.
     * - Concurrency guard via isCapturing.
     * - Safe HardwareBuffer closure.
     * - Direct session delivery to SearchSessionManager.
     */
    fun triggerCircleToSearch() {
        if (isCapturing) {
            Log.w(TAG, "Screenshot capture already in flight, ignoring duplicate trigger")
            return
        }

        isCapturing = true
        val session = SearchSessionManager.createNewSession()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            isCapturing = false
                            val hardwareBuffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.let { hwBmp ->
                                    hwBmp.copy(Bitmap.Config.ARGB_8888, false)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error copying screenshot buffer: ${e.message}", e)
                                null
                            } finally {
                                try {
                                    hardwareBuffer.close()
                                } catch (_: Exception) {}
                            }

                            if (bitmap != null) {
                                SearchSessionManager.setSessionBitmap(session.id, bitmap)
                                launchOverlayActivity(session.id)
                            } else {
                                SearchSessionManager.setSessionError(session.id, "Failed to decode screen buffer")
                                notifyCaptureFailure("Unable to process display buffer.")
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            isCapturing = false
                            val reason = when (errorCode) {
                                AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR -> "Internal system error"
                                AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "Accessibility permission denied"
                                AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "Capture rate limit exceeded"
                                AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "Invalid display"
                                else -> "Screen protected by app security (FLAG_SECURE) or system policy"
                            }
                            Log.w(TAG, "takeScreenshot failed ($errorCode): $reason")
                            SearchSessionManager.setSessionError(session.id, reason)
                            // Crucial: DO NOT launch activity on failure! Notify user cleanly.
                            notifyCaptureFailure(reason)
                        }
                    }
                )
            } catch (e: Exception) {
                isCapturing = false
                Log.e(TAG, "takeScreenshot exception: ${e.message}", e)
                SearchSessionManager.setSessionError(session.id, e.message ?: "Screenshot exception")
                notifyCaptureFailure("Screenshot exception: ${e.message}")
            }
        } else {
            isCapturing = false
            notifyCaptureFailure("Requires Android 11+ (API 30+)")
        }
    }

    private fun launchOverlayActivity(sessionId: String) {
        val intent = Intent(this, CircleSearchActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun notifyCaptureFailure(reason: String) {
        handler.post {
            Toast.makeText(this, "Shovo Circle Search: $reason", Toast.LENGTH_LONG).show()
        }
    }

    private fun vibrateHaptic() {
        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        val hapticEnabled = prefs.getBoolean("pref_haptic_feedback", true)
        if (!hapticEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(60)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed -> updating bottom overlay geometry")
        handler.postDelayed({ setupBottomGestureZone() }, 150)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        handler.removeCallbacksAndMessages(null)
        if (pillHandleView != null) {
            try {
                windowManager?.removeView(pillHandleView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing pill overlay on destroy: ${e.message}")
            }
            pillHandleView = null
        }
        Log.i(TAG, "Shovo NavigationGestureService destroyed")
    }
}
