package dev.shovo.circlesearch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
 * Accessibility Service providing bottom-center hold detection for Circle to Search for Xiaomi
 * on Xiaomi HyperOS 3 / Android 16 (Xiaomi 13 'fuxi').
 *
 * Utilizes a TYPE_ACCESSIBILITY_OVERLAY touch zone calibrated over the navigation pill.
 * Implements a formal gesture state machine distinguishing stationary holds from navigation swipes.
 */
class NavigationGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "NavGestureService"
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
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private enum class GestureState {
        IDLE,
        PENDING,
        ACTIVATED,
        CANCELLED
    }

    private var gestureState = GestureState.IDLE
    private var isCapturing = false

    var holdThresholdMs: Long = 450L
        get() {
            val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
            return prefs.getLong("pref_hold_threshold_ms", 450L).coerceIn(300L, 750L)
        }
        set(value) {
            field = value.coerceIn(300L, 750L)
            getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("pref_hold_threshold_ms", field)
                .apply()
        }

    private val holdRunnable = Runnable {
        if (gestureState == GestureState.PENDING) {
            gestureState = GestureState.ACTIVATED
            Log.d(TAG, "Hold threshold met ($holdThresholdMs ms) -> triggering Circle to Search")
            vibrateHaptic()
            triggerCircleToSearch()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Enforce 0 event listening so the service produces no CPU overhead or window event flooding
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = 0
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupBottomGestureZone()
        Log.i(TAG, "NavigationGestureService connected on HyperOS 3 / Android 16 (0 events declared)")
    }

    fun updateGestureZoneConfiguration() {
        setupBottomGestureZone()
    }

    private fun setupBottomGestureZone() {
        if (pillHandleView != null) {
            try {
                if (pillHandleView?.isAttachedToWindow == true) {
                    windowManager?.removeView(pillHandleView)
                }
            } catch (_: Exception) {}
            pillHandleView = null
        }

        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        val activationMethod = prefs.getString("pref_activation_method", "BOTTOM_HOLD")
        if (activationMethod != "BOTTOM_HOLD") {
            Log.i(TAG, "Bottom hold inactive (preferred method: $activationMethod)")
            return
        }

        val displayMetrics = resources.displayMetrics
        val pillWidthDp = prefs.getInt("pref_pill_width_dp", 160)
        val pillHeightDp = prefs.getInt("pref_pill_height_dp", 24)

        val pillWidth = (pillWidthDp * displayMetrics.density).toInt()
        val pillHeight = (pillHeightDp * displayMetrics.density).toInt()
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
                        activePointerId = event.getPointerId(0)
                        downX = event.rawX
                        downY = event.rawY
                        downTime = SystemClock.uptimeMillis()
                        gestureState = GestureState.PENDING

                        handler.removeCallbacks(holdRunnable)
                        handler.postDelayed(holdRunnable, holdThresholdMs)
                        true
                    }

                    MotionEvent.ACTION_POINTER_DOWN -> {
                        // Multi-touch interruption: cancel hold timer
                        if (gestureState == GestureState.PENDING) {
                            gestureState = GestureState.CANCELLED
                            handler.removeCallbacks(holdRunnable)
                            Log.d(TAG, "Secondary pointer down -> cancelled hold")
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (gestureState == GestureState.PENDING) {
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex >= 0) {
                                val currentX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawX(pointerIndex) else event.rawX
                                val currentY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawY(pointerIndex) else event.rawY
                                val dx = abs(currentX - downX)
                                val dy = downY - currentY // positive when swiping upwards

                                // Cancel if user moves horizontally or upwards beyond touch slop
                                if (dy > touchSlop * 2.0f || dx > touchSlop * 2.5f || (currentY - downY) > touchSlop * 2.0f) {
                                    gestureState = GestureState.CANCELLED
                                    handler.removeCallbacks(holdRunnable)
                                    Log.d(TAG, "Touch exceeded jitter slop (dy=$dy, dx=$dx) -> cancelled hold timer")
                                }
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(holdRunnable)
                        val wasActivated = (gestureState == GestureState.ACTIVATED)
                        val wasCancelled = (gestureState == GestureState.CANCELLED)
                        gestureState = GestureState.IDLE

                        if (wasCancelled && event.actionMasked == MotionEvent.ACTION_UP) {
                            val elapsed = SystemClock.uptimeMillis() - downTime
                            val dy = downY - event.rawY
                            // Handle upward navigation gesture delegation
                            if (dy > touchSlop * 2.0f) {
                                if (dy > touchSlop * 4.5f && elapsed > 280) {
                                    Log.d(TAG, "Delegating Recents gesture")
                                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                                } else {
                                    Log.d(TAG, "Delegating Home gesture")
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                }
                            }
                        }
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        wasActivated || wasCancelled
                    }

                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(pillHandleView, params)
            Log.d(TAG, "Pill overlay registered: ${pillWidth}x${pillHeight}px at bottom center")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register pill overlay: ${e.message}", e)
        }
    }

    /**
     * Executes non-root screenshot capture via Android AccessibilityService API.
     * Guaranteed:
     * - No recursive re-triggering.
     * - Request-scoped Session created in SearchSessionManager.
     * - Safe HardwareBuffer closure.
     * - Complete error recovery and cleanup.
     */
    fun triggerCircleToSearch() {
        if (isCapturing) {
            Log.w(TAG, "Screenshot capture already in flight, ignoring duplicate trigger")
            return
        }

        isCapturing = true
        val session = SearchSessionManager.createSession()

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
                                SearchSessionManager.completeCapture(session.id, bitmap)
                                launchOverlayActivity(session.id)
                            } else {
                                SearchSessionManager.failCapture(session.id, "Failed to decode screen buffer")
                                SearchSessionManager.releaseSession(session.id)
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
                            SearchSessionManager.failCapture(session.id, reason)
                            SearchSessionManager.releaseSession(session.id)
                            notifyCaptureFailure(reason)
                        }
                    }
                )
            } catch (e: Exception) {
                isCapturing = false
                Log.e(TAG, "takeScreenshot exception: ${e.message}", e)
                SearchSessionManager.failCapture(session.id, e.message ?: "Screenshot exception")
                SearchSessionManager.releaseSession(session.id)
                notifyCaptureFailure("Screenshot exception: ${e.message}")
            }
        } else {
            isCapturing = false
            SearchSessionManager.failCapture(session.id, "Requires Android 11+ (API 30+)")
            SearchSessionManager.releaseSession(session.id)
            notifyCaptureFailure("Requires Android 11+ (API 30+)")
        }
    }

    private fun launchOverlayActivity(sessionId: String) {
        try {
            val intent = Intent(this, CircleSearchActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CircleSearchActivity: ${e.message}", e)
            SearchSessionManager.failCapture(sessionId, "Activity launch failed: ${e.message}")
            SearchSessionManager.releaseSession(sessionId)
            notifyCaptureFailure("Could not launch selection overlay.")
        }
    }

    private fun notifyCaptureFailure(reason: String) {
        handler.post {
            Toast.makeText(this, "Circle to Search: $reason", Toast.LENGTH_LONG).show()
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
                if (pillHandleView?.isAttachedToWindow == true) {
                    windowManager?.removeView(pillHandleView)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing pill overlay on destroy: ${e.message}")
            }
            pillHandleView = null
        }
        Log.i(TAG, "NavigationGestureService destroyed")
    }
}
