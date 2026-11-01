package dev.shovo.circlesearch

import android.content.Intent
import android.content.pm.ActivityInfo
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
 * selection coordinate mapping, and lifecycle-aware structured concurrency.
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
    private var currentSession: SearchSessionManager.Session? = null

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        btnClose.setOnClickListener {
            finish()
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
        // Release previous session lease if updating
        currentSession?.releaseLease()

        val sessionId = intent.getStringExtra(NavigationGestureService.EXTRA_SESSION_ID)
        activeSessionId = sessionId
        val session = SearchSessionManager.getSession(sessionId)

        if (session != null && session.acquireLease()) {
            currentSession = session
            val bmp = session.bitmap

            if (bmp != null && !bmp.isRecycled) {
                // Adapt orientation dynamically to match screenshot aspect ratio
                if (bmp.width > bmp.height) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                ivBackground.setImageBitmap(bmp)
                pbLoading.visibility = View.GONE
                layoutTopBar.visibility = View.VISIBLE
                tvHint.setText(R.string.hint_circle)
            } else {
                Log.w(TAG, "Session [$sessionId] bitmap is null or recycled")
                Toast.makeText(this, "Circle to Search: Capture buffer unavailable", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Log.w(TAG, "Failed to acquire lease for session [$sessionId]; terminating cleanly")
            Toast.makeText(this, "Circle to Search: No active capture session", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleSelectionComplete(rect: RectF) {
        val session = currentSession
        val bmp = session?.bitmap

        if (session == null || bmp == null || bmp.isRecycled) {
            Toast.makeText(this, "Session expired or unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        layoutTopBar.visibility = View.GONE
        pbLoading.visibility = View.VISIBLE

        // Mathematical coordinate mapping from view space to bitmap space
        val mappedRect = RectF()
        val inverseMatrix = Matrix()
        if (ivBackground.imageMatrix != null && ivBackground.imageMatrix.invert(inverseMatrix)) {
            inverseMatrix.mapRect(mappedRect, rect)
        } else {
            // Direct scale fallback for FIT_XY
            val canvasW = max(1f, circleCanvas.width.toFloat())
            val canvasH = max(1f, circleCanvas.height.toFloat())
            val scaleX = bmp.width.toFloat() / canvasW
            val scaleY = bmp.height.toFloat() / canvasH
            mappedRect.set(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY
            )
        }

        // Clamp strictly to bitmap dimensions
        val left = max(0f, min(mappedRect.left, (bmp.width - 1).toFloat()))
        val top = max(0f, min(mappedRect.top, (bmp.height - 1).toFloat()))
        val right = max(left + 1f, min(mappedRect.right, bmp.width.toFloat()))
        val bottom = max(top + 1f, min(mappedRect.bottom, bmp.height.toFloat()))
        val finalCropRect = RectF(left, top, right, bottom)

        // Execute background crop and save using structured coroutines
        lifecycleScope.launch(Dispatchers.Default) {
            // Acquire temporary lease for background worker
            session.acquireLease()
            try {
                val cropped = LensSearchHelper.cropBitmap(bmp, finalCropRect)
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
                        Toast.makeText(this@CircleSearchActivity, "Failed to crop selection", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } finally {
                session.releaseLease()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentSession?.releaseLease()
        currentSession = null
    }
}
