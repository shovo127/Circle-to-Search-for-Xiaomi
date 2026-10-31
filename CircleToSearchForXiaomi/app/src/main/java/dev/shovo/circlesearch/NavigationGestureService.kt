package dev.shovo.circlesearch

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class NavigationGestureService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var pillHandleView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    private var isHolding = false
    private val holdThresholdMs = 450L

    private val holdRunnable = Runnable {
        isHolding = true
        vibrateHaptic()
        triggerCircleToSearch()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        setupBottomGestureZone()
    }

    private fun setupBottomGestureZone() {
        if (pillHandleView != null) return

        // Width: 260dp, Height: 40dp, positioned at bottom-center over the gesture line
        val displayMetrics = resources.displayMetrics
        val pillWidth = (260 * displayMetrics.density).toInt()
        val pillHeight = (40 * displayMetrics.density).toInt()

        val params = WindowManager.LayoutParams(
            pillWidth,
            pillHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isHolding = false
                        handler.postDelayed(holdRunnable, holdThresholdMs)
                        false // Pass event through so normal system gestures can start
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(holdRunnable)
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // If moved significantly upwards, cancel hold (user is swiping home/recents)
                        if (event.y < -30f) {
                            handler.removeCallbacks(holdRunnable)
                        }
                        false
                    }
                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(pillHandleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerCircleToSearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val colorSpace = screenshot.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        val copy = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()

                        CircleSearchActivity.currentScreenshot = copy
                        launchOverlayActivity()
                    }

                    override fun onFailure(errorCode: Int) {
                        launchOverlayActivity()
                    }
                }
            )
        } else {
            launchOverlayActivity()
        }
    }

    private fun launchOverlayActivity() {
        val intent = Intent(this, CircleSearchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun vibrateHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (pillHandleView != null) {
            try {
                windowManager?.removeView(pillHandleView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pillHandleView = null
        }
    }
}
