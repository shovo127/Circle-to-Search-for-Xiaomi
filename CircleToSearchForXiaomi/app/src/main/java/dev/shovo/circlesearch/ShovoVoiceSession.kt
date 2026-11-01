package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.widget.Toast

/**
 * Official AOSP VoiceInteractionSession for Circle to Search for Xiaomi.
 *
 * Receives the zero-prompt system screenshot callback directly from Android WindowManager
 * via onHandleScreenshot without requiring AccessibilityService capture permissions.
 */
class ShovoVoiceSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "VoiceSession"
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "VoiceInteractionSession onShow (flags=$showFlags)")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        if (screenshot != null && !screenshot.isRecycled) {
            Log.d(TAG, "Received system screenshot via Assist API: ${screenshot.width}x${screenshot.height}")
            val session = SearchSessionManager.createSession()
            SearchSessionManager.completeCapture(session.id, screenshot)

            val intent = Intent(context, CircleSearchActivity::class.java).apply {
                putExtra(NavigationGestureService.EXTRA_SESSION_ID, session.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
            hide()
        } else {
            Log.w(TAG, "Assist screenshot is null or blocked by system policy (FLAG_SECURE)")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Circle to Search: Screen capture blocked by policy", Toast.LENGTH_SHORT).show()
            }
            hide()
        }
    }
}
