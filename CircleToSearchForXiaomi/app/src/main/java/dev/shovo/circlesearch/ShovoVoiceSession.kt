package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Official AOSP VoiceInteractionSession for Circle to Search for Xiaomi.
 *
 * Receives the zero-prompt system screenshot callback directly from Android WindowManager
 * via onHandleScreenshot when triggered by Long-Press Power or system Assistant shortcuts.
 */
class ShovoVoiceSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "VoiceSession"
    }

    private val isProcessingScreenshot = AtomicBoolean(false)

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "VoiceInteractionSession onShow (flags=$showFlags)")
        isProcessingScreenshot.set(false)

        val hasScreenshotFlag = (showFlags and SHOW_WITH_SCREENSHOT) != 0
        if (!hasScreenshotFlag) {
            Log.w(TAG, "onShow called without SHOW_WITH_SCREENSHOT flag")
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)

        if (!isProcessingScreenshot.compareAndSet(false, true)) {
            Log.w(TAG, "Screenshot already processed for this session invocation; ignoring duplicate callback")
            return
        }

        if (screenshot != null && !screenshot.isRecycled) {
            Log.d(TAG, "Received system screenshot via Assist API: ${screenshot.width}x${screenshot.height}")
            val session = SearchSessionManager.createSession()
            SearchSessionManager.completeCapture(session.id, screenshot)

            val intent = Intent(context, CircleSearchActivity::class.java).apply {
                putExtra(NavigationGestureService.EXTRA_SESSION_ID, session.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startAssistantActivity(intent)
                } else {
                    startVoiceActivity(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "startAssistantActivity failed: ${e.message}; attempting context.startActivity fallback")
                try {
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to launch CircleSearchActivity from assistant session: ${ex.message}", ex)
                    SearchSessionManager.failCapture(session.id, "Activity launch failed")
                    SearchSessionManager.releaseSession(session.id)
                }
            } finally {
                hide()
            }
        } else {
            Log.w(TAG, "Assist screenshot is null or blocked by system policy (FLAG_SECURE)")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Circle to Search: Screen capture blocked by security policy", Toast.LENGTH_SHORT).show()
            }
            hide()
        }
    }

    override fun onHide() {
        super.onHide()
        isProcessingScreenshot.set(false)
    }
}
