package dev.shovo.circlesearch

import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Fullscreen translucent overlay for Shovo Circle Search.
 * Displays the current session screenshot and enables freeform circle / rectangle selection.
 */
class CircleSearchActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShovoCircleSearchAct"
    }

    private lateinit var ivBackground: ImageView
    private lateinit var circleCanvas: CircleCanvasView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvHint: TextView

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        loadSessionScreenshot()

        circleCanvas.setOnSelectionCompleteListener { rect ->
            handleSelectionComplete(rect)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadSessionScreenshot()
    }

    private fun loadSessionScreenshot() {
        val session = SearchSessionManager.getActiveSession()
        val bmp = session?.bitmap

        if (bmp != null && !bmp.isRecycled) {
            ivBackground.setImageBitmap(bmp)
            pbLoading.visibility = View.GONE
            tvHint.visibility = View.VISIBLE
            tvHint.setText(R.string.hint_circle)
        } else {
            // No valid session available. Terminate cleanly without recursive re-triggering!
            Log.w(TAG, "No valid screenshot session available; terminating activity cleanly")
            Toast.makeText(this, "Shovo Circle Search: No capture session active", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleSelectionComplete(rect: RectF) {
        pbLoading.visibility = View.VISIBLE
        tvHint.setText(R.string.searching)

        Thread {
            val session = SearchSessionManager.getActiveSession()
            val bmp = session?.bitmap
            if (bmp != null && !bmp.isRecycled) {
                val canvasW = circleCanvas.width.toFloat()
                val canvasH = circleCanvas.height.toFloat()

                val scaleX = if (canvasW > 0) bmp.width / canvasW else 1.0f
                val scaleY = if (canvasH > 0) bmp.height / canvasH else 1.0f

                val mappedRect = RectF(
                    rect.left * scaleX,
                    rect.top * scaleY,
                    rect.right * scaleX,
                    rect.bottom * scaleY
                )

                val cropped = LensSearchHelper.cropBitmap(bmp, mappedRect)
                val uri = LensSearchHelper.saveCroppedBitmap(this, cropped)

                runOnUiThread {
                    if (uri != null) {
                        val result = LensSearchHelper.dispatchVisualSearch(this, uri)
                        when (result) {
                            is LensSearchHelper.DispatchResult.Success -> {
                                Log.i(TAG, "Visual search launched successfully: ${result.providerName}")
                            }
                            is LensSearchHelper.DispatchResult.EngineNotInstalled -> {
                                Toast.makeText(
                                    this,
                                    "${result.engineName} app is not installed. Please install it or use Google Lens.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is LensSearchHelper.DispatchResult.Failed -> {
                                Toast.makeText(this, "Could not dispatch visual search: ${result.reason}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to crop selection", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Session expired or unavailable", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        SearchSessionManager.clearSession()
    }
}
