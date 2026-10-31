package dev.shovo.circlesearch

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

class CircleSearchActivity : Activity() {

    companion object {
        var currentScreenshot: Bitmap? = null
    }

    private lateinit var ivBackground: ImageView
    private lateinit var circleCanvas: CircleCanvasView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_search)

        ivBackground = findViewById(R.id.ivBackgroundScreenshot)
        circleCanvas = findViewById(R.id.circleCanvas)
        pbLoading = findViewById(R.id.pbLoading)
        tvHint = findViewById(R.id.tvHint)

        val screenshot = currentScreenshot
        if (screenshot != null) {
            ivBackground.setImageBitmap(screenshot)
        } else {
            // Default dark translucent background if screenshot not pre-loaded
            window.decorView.setBackgroundColor(Color.parseColor("#99000000"))
        }

        circleCanvas.setOnSelectionCompleteListener { rect ->
            pbLoading.visibility = View.VISIBLE
            tvHint.setText(R.string.searching)

            Thread {
                val bmp = currentScreenshot
                if (bmp != null) {
                    val cropped = LensSearchHelper.cropBitmap(bmp, rect)
                    val uri = LensSearchHelper.saveCroppedBitmap(this, cropped)
                    if (uri != null) {
                        runOnUiThread {
                            LensSearchHelper.launchGoogleLensSearch(this, uri)
                            finish()
                        }
                    } else {
                        runOnUiThread { finish() }
                    }
                } else {
                    runOnUiThread { finish() }
                }
            }.start()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentScreenshot = null
    }
}

