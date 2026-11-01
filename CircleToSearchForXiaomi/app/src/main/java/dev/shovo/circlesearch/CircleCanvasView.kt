package dev.shovo.circlesearch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance interactive drawing canvas for Circle to Search for Xiaomi.
 *
 * Implements Google-style dual-tone glowing stroke (outer electric cyan glow + inner white core),
 * freeform lasso path tracing, and post-selection bounding box computation with auto-expansion
 * for micro-taps.
 */
class CircleCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var maxY = -Float.MAX_VALUE
    private var isDrawing = false
    private var isSelectionConfirmed = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    // Outer Cyan Glow
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9900E5FF") // Electric Cyan Translucent Glow
        strokeWidth = 18f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Inner White Core
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Translucent Selection Scrim Fill
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2600E5FF") // Subtle translucent cyan fill
        style = Paint.Style.FILL
    }

    // Finalized Dashed Bounding Box
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
    }

    private var onSelectionCompleteListener: ((RectF) -> Unit)? = null

    fun setOnSelectionCompleteListener(listener: (RectF) -> Unit) {
        this.onSelectionCompleteListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                val rawX = event.x
                val rawY = event.y
                if (!rawX.isFinite() || !rawY.isFinite()) return false

                val clampedX = max(0f, min(rawX, width.toFloat()))
                val clampedY = max(0f, min(rawY, height.toFloat()))

                path.reset()
                path.moveTo(clampedX, clampedY)
                minX = clampedX
                minY = clampedY
                maxX = clampedX
                maxY = clampedY
                isDrawing = true
                isSelectionConfirmed = false
                vibrateTick()
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDrawing) return false
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false

                val rawX = event.getX(pointerIndex)
                val rawY = event.getY(pointerIndex)
                if (!rawX.isFinite() || !rawY.isFinite()) return true

                val clampedX = max(0f, min(rawX, width.toFloat()))
                val clampedY = max(0f, min(rawY, height.toFloat()))

                path.lineTo(clampedX, clampedY)
                minX = min(minX, clampedX)
                minY = min(minY, clampedY)
                maxX = max(maxX, clampedX)
                maxY = max(maxY, clampedY)
                invalidate()
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Ignore additional touches; do not let secondary fingers warp the drawing
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                if (event.getPointerId(pointerIndex) == activePointerId) {
                    val rawX = event.getX(pointerIndex)
                    val rawY = event.getY(pointerIndex)
                    finalizeSelection(rawX, rawY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDrawing) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    val (rawX, rawY) = if (pointerIndex >= 0) {
                        Pair(event.getX(pointerIndex), event.getY(pointerIndex))
                    } else {
                        Pair(null, null)
                    }
                    finalizeSelection(rawX, rawY)
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                isSelectionConfirmed = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                path.reset()
                minX = Float.MAX_VALUE
                minY = Float.MAX_VALUE
                maxX = -Float.MAX_VALUE
                maxY = -Float.MAX_VALUE
                invalidate()
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun finalizeSelection(rawX: Float?, rawY: Float?) {
        if (!isDrawing) return
        if (rawX != null && rawY != null && rawX.isFinite() && rawY.isFinite()) {
            val clampedX = max(0f, min(rawX, width.toFloat()))
            val clampedY = max(0f, min(rawY, height.toFloat()))
            path.lineTo(clampedX, clampedY)
            minX = min(minX, clampedX)
            minY = min(minY, clampedY)
            maxX = max(maxX, clampedX)
            maxY = max(maxY, clampedY)
        }

        isDrawing = false
        isSelectionConfirmed = true
        path.close()
        invalidate()

        // Density-aware minimum dimension (48dp ~ 130-150px on modern high-DPI displays)
        val minDimension = 48f * resources.displayMetrics.density
        val rect = RectF(minX, minY, maxX, maxY)

        if (rect.width() < minDimension || rect.height() < minDimension) {
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val halfSpan = max(minDimension, max(rect.width(), rect.height())) / 2f
            rect.set(
                max(0f, centerX - halfSpan),
                max(0f, centerY - halfSpan),
                min(width.toFloat(), centerX + halfSpan),
                min(height.toFloat(), centerY + halfSpan)
            )
        }

        if (rect.width() > 0f && rect.height() > 0f && !rect.isEmpty) {
            vibrateConfirm()
            onSelectionCompleteListener?.invoke(rect)
        }
        activePointerId = MotionEvent.INVALID_POINTER_ID
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!path.isEmpty) {
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, glowPaint)
            canvas.drawPath(path, corePaint)

            if (isSelectionConfirmed && minX < maxX && minY < maxY) {
                canvas.drawRoundRect(minX, minY, maxX, maxY, 16f, 16f, borderPaint)
            }
        }
    }

    private fun isHapticEnabled(): Boolean {
        val prefs = context.getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pref_haptic_feedback", true)
    }

    private fun vibrateTick() {
        if (!isHapticEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateConfirm() {
        if (!isHapticEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(30)
            }
        } catch (_: Exception) {}
    }
}
