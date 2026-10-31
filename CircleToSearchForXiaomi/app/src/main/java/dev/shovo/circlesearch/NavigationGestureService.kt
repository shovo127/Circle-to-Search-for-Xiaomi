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
import kotlin.math.abs

/**
 * Accessibility Service providing bottom-center hold detection for Xiaomi HyperOS 3 / Android 16.
 *
 * Employs a TYPE_ACCESSIBILITY_OVERLAY touch zone calibrated over the navigation gesture indicator.
 * Accurately distinguishes stationary long-press (Circle to Search) from navigation swipes (Home / Recents).
 */
class NavigationGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "NavGestureService"
        var instance: NavigationGestureService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var pillHandleView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var isHoldTriggered = false
    private var isSwipeDetected = false

    // Configurable hold threshold (default 450ms)
    var holdThresholdMs: Long = 450L
        get() {
            val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
            return prefs.getLong("hold_threshold_ms", 450L)
        }
        set(value) {
            field = value
            getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("hold_threshold_ms", value)
                .apply()
        }

    private val holdRunnable = Runnable {
        isHoldTriggered = true
        Log.d(TAG, "Bottom hold threshold met ($holdThresholdMs ms) -> triggering Circle to Search")
        vibrateHaptic()
        triggerCircleToSearch()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupBottomGestureZone()
        Log.i(TAG, "NavigationGestureService connected on HyperOS 3 / Android 16")
    }

    private fun setupBottomGestureZone() {
        if (pillHandleView != null) {
            try {
                windowManager?.removeView(pillHandleView)
            } catch (_: Exception) {}
            pillHandleView = null
        }

        val displayMetrics = resources.displayMetrics
        // Navigation pill on Xiaomi 13 is ~160dp wide and ~20dp tall.
        // We set 180dp width and 32dp height for optimal touch target while keeping edges free.
        val pillWidth = (180 * displayMetrics.density).toInt()
        val pillHeight = (32 * displayMetrics.density).toInt()
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
                        isHoldTriggered = false
                        isSwipeDetected = false

                        handler.postDelayed(holdRunnable, holdThresholdMs)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!isHoldTriggered && !isSwipeDetected) {
                            val dx = abs(event.rawX - downX)
                            val dy = downY - event.rawY // positive when swiping up

                            // Normal human hold has finger jitter of ~20-30dp (~60-90px on 440dpi).
                            // A deliberate navigation swipe is an intentional upward/lateral motion > 3.5x touchSlop.
                            if (dy > touchSlop * 3.5f || dx > touchSlop * 4.0f) {
                                isSwipeDetected = true
                                handler.removeCallbacks(holdRunnable)
                                Log.d(TAG, "Swipe gesture detected (dy=$dy, dx=$dx) -> cancelled hold timer")
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(holdRunnable)

                        if (!isHoldTriggered) {
                            val elapsed = SystemClock.uptimeMillis() - downTime
                            val dy = downY - event.rawY

                            if (isSwipeDetected || dy > touchSlop * 2.5f) {
                                // Delegate navigation gesture
                                if (dy > touchSlop * 6.0f && elapsed > 280) {
                                    Log.d(TAG, "Delegating GLOBAL_ACTION_RECENTS")
                                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                                } else if (dy > touchSlop * 2.5f) {
                                    Log.d(TAG, "Delegating GLOBAL_ACTION_HOME")
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                }
                            }
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(pillHandleView, params)
            Log.d(TAG, "Pill overlay registered: ${pillWidth}x${pillHeight}px at bottom center")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pill overlay: ${e.message}", e)
        }
    }

    fun triggerCircleToSearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val hardwareBuffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.let { hwBmp ->
                                    hwBmp.copy(Bitmap.Config.ARGB_8888, false)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error copying screenshot buffer: ${e.message}")
                                null
                            } finally {
                                try {
                                    hardwareBuffer.close()
                                } catch (_: Exception) {}
                            }

                            CircleSearchActivity.currentScreenshot = bitmap
                            launchOverlayActivity()
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "takeScreenshot failed with code $errorCode; launching fallback overlay")
                            launchOverlayActivity()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "takeScreenshot exception: ${e.message}", e)
                launchOverlayActivity()
            }
        } else {
            launchOverlayActivity()
        }
    }

    private fun launchOverlayActivity() {
        val intent = Intent(this, CircleSearchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun vibrateHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
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
        Log.d(TAG, "Configuration changed -> updating bottom overlay")
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
    }
}
