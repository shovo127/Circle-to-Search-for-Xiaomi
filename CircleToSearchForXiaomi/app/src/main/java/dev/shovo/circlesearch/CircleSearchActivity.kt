package dev.shovo.circlesearch

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CircleSearchActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CircleSearchActivity"
        var currentScreenshot: Bitmap? = null
    }

    private lateinit var ivBackground: ImageView
    private lateinit var circleCanvas: CircleCanvasView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure edge-to-edge transparent window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = Color.TRANSPARENT
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

        loadScreenshotOrRequest()

        circleCanvas.setOnSelectionCompleteListener { rect ->
            handleSelectionComplete(rect)
        }
    }

    private fun loadScreenshotOrRequest() {
        val screenshot = currentScreenshot
        if (screenshot != null && !screenshot.isRecycled) {
            ivBackground.setImageBitmap(screenshot)
        } else {
            val service = NavigationGestureService.instance
            if (service != null) {
                finish()
                service.triggerCircleToSearch()
            } else {
                window.decorView.setBackgroundColor(Color.parseColor("#99000000"))
                Toast.makeText(this, "Please enable Circle to Search accessibility service", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleSelectionComplete(rect: RectF) {
        pbLoading.visibility = View.VISIBLE
        tvHint.setText(R.string.searching)

        Thread {
            val bmp = currentScreenshot
            if (bmp != null && !bmp.isRecycled) {
                // Calculate scale ratio between bitmap and view
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
                        val launched = LensSearchHelper.launchGoogleLensSearch(this, uri)
                        if (!launched) {
                            Toast.makeText(this, "Could not open visual search", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to crop selection", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "No screenshot available. Please enable accessibility service.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentScreenshot = null
    }
}
