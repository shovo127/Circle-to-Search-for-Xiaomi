package dev.shovo.circlesearch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Fullscreen translucent overlay for Circle to Search for Xiaomi.
 *
 * Implements request-scoped session leases, mathematically verified inverse matrix
 * selection coordinate mapping, aspect-ratio preservation without forced display rotation,
 * and lifecycle-aware structured concurrency.
 */
class CircleSearchActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CircleSearchActivity"
    }

    private lateinit var ivBackground: ImageView
    private lateinit var circleCanvas: CircleCanvasView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvHint: TextView
    private lateinit var layoutTopBar: View
    private lateinit var btnClose: ImageButton

    private var activeSessionId: String? = null
    private var currentLease: SearchSessionManager.Lease? = null
    private var currentMatrix = Matrix()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure edge-to-edge transparent window with cutout support
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_circle_search)

        ivBackground = findViewById(R.id.ivBackgroundScreenshot)
        circleCanvas = findViewById(R.id.circleCanvas)
        pbLoading = findViewById(R.id.pbLoading)
        tvHint = findViewById(R.id.tvHint)
        layoutTopBar = findViewById(R.id.layoutTopBar)
        btnClose = findViewById(R.id.btnClose)

        // Disable canvas until image matrix is calculated from layout
        circleCanvas.isEnabled = false

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        btnClose.setOnClickListener {
            finish()
        }

        ivBackground.addOnLayoutChangeListener { _, left, top, right, bottom, oldL, oldT, oldR, oldB ->
            if (right - left != oldR - oldL || bottom - top != oldB - oldT) {
                updateImageMatrix()
            }
        }

        bindSessionFromIntent(intent)

        circleCanvas.setOnSelectionCompleteListener { rect ->
            handleSelectionComplete(rect)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bindSessionFromIntent(intent)
    }

    private fun bindSessionFromIntent(intent: Intent) {
        // Idempotently release any previous activity lease before binding
        currentLease?.release()
        currentLease = null

        val sessionId = intent.getStringExtra(NavigationGestureService.EXTRA_SESSION_ID)
        activeSessionId = sessionId
        val session = SearchSessionManager.getSession(sessionId)

        if (session != null) {
            val lease = session.acquireLease("CircleSearchActivity")
            if (lease != null) {
                currentLease = lease
                val bmp = lease.bitmap

                if (bmp != null && !bmp.isRecycled) {
                    ivBackground.setImageBitmap(bmp)
                    updateImageMatrix()
                    pbLoading.visibility = View.GONE
                    layoutTopBar.visibility = View.VISIBLE
                    tvHint.setText(R.string.hint_circle)
                    return
                } else {
                    Log.w(TAG, "Session [$sessionId] bitmap is null or recycled")
                    lease.release()
                    currentLease = null
                }
            }
        }

        Log.w(TAG, "Failed to bind valid session for [$sessionId]; finishing cleanly")
        Toast.makeText(this, "Circle to Search: Capture session unavailable", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateImageMatrix() {
        val bmp = currentLease?.bitmap ?: return
        if (bmp.isRecycled) return

        val viewW = ivBackground.width.toFloat()
        val viewH = ivBackground.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val src = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        val dst = RectF(0f, 0f, viewW, viewH)
        val matrix = Matrix()
        // Center-fit preserves exact aspect ratio without distortion or rotation
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER)
        ivBackground.imageMatrix = matrix
        currentMatrix = matrix
        circleCanvas.isEnabled = true
    }

    private fun handleSelectionComplete(rect: RectF) {
        val lease = currentLease
        val session = lease?.session
        val bmp = lease?.bitmap

        if (lease == null || session == null || bmp == null || bmp.isRecycled) {
            Toast.makeText(this, "Session expired or unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Prevent repeated selection touches while search handoff is in progress
        circleCanvas.isEnabled = false

        // Acquire dedicated worker lease before starting background processing
        val workerLease = session.acquireLease("BackgroundCropWorker")
        if (workerLease == null) {
            Toast.makeText(this, "Session closed; cannot complete crop", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        layoutTopBar.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE

        // Mathematical coordinate mapping from view space to bitmap space
        val inverseMatrix = Matrix()
        val mappedRect = RectF()
        if (!currentMatrix.invert(inverseMatrix)) {
            Log.e(TAG, "Failed to invert display matrix")
            workerLease.release()
            finish()
            return
        }
        inverseMatrix.mapRect(mappedRect, rect)

        // Strict intersection calculation with bitmap bounds to prevent letterbox edge slivers
        val bmpBounds = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        val finalCropRect = RectF()
        if (!finalCropRect.setIntersect(mappedRect, bmpBounds) || finalCropRect.width() < 10f || finalCropRect.height() < 10f) {
            Log.w(TAG, "Selection outside content boundaries: mapped=$mappedRect, bmpBounds=$bmpBounds")
            workerLease.release()
            circleCanvas.isEnabled = true
            pbLoading.visibility = View.GONE
            layoutTopBar.visibility = View.VISIBLE
            Toast.makeText(this, "Please select an area on the screen", Toast.LENGTH_SHORT).show()
            return
        }

        // Execute background crop and save using structured coroutines
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val workerBmp = workerLease.bitmap
                if (workerBmp == null || workerBmp.isRecycled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CircleSearchActivity, "Capture buffer was recycled", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val cropped = LensSearchHelper.cropBitmap(workerBmp, finalCropRect)
                if (cropped == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CircleSearchActivity, "Failed to crop selection", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val uri = LensSearchHelper.saveCroppedBitmap(this@CircleSearchActivity, cropped)

                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val result = LensSearchHelper.dispatchVisualSearch(this@CircleSearchActivity, uri)
                        when (result) {
                            is LensSearchHelper.DispatchResult.HandedOff -> {
                                Log.i(TAG, "Handoff to provider: ${result.providerName}")
                            }
                            is LensSearchHelper.DispatchResult.ChooserOpened -> {
                                Log.i(TAG, "Universal system chooser opened")
                            }
                            is LensSearchHelper.DispatchResult.ProviderUnavailable -> {
                                Toast.makeText(
                                    this@CircleSearchActivity,
                                    result.reason,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is LensSearchHelper.DispatchResult.DispatchFailure -> {
                                Toast.makeText(
                                    this@CircleSearchActivity,
                                    "Could not dispatch visual search: ${result.error}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@CircleSearchActivity, "Failed to save cropped selection", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } finally {
                workerLease.release()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val session = currentLease?.session
        currentLease?.release()
        currentLease = null
        session?.closeSession()
    }
}
