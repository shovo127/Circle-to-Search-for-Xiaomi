package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log

/**
 * Official AOSP VoiceInteractionSession for Shovo Circle Search.
 *
 * Receives the zero-prompt system screenshot callback directly from Android WindowManager
 * via onHandleScreenshot without requiring AccessibilityService capture permissions.
 */
class ShovoVoiceSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "ShovoVoiceSession"
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "ShovoVoiceSession onShow (flags=$showFlags)")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        if (screenshot != null && !screenshot.isRecycled) {
            Log.d(TAG, "Received system screenshot via Assist API: ${screenshot.width}x${screenshot.height}")
            val session = SearchSessionManager.createNewSession()
            SearchSessionManager.setSessionBitmap(session.id, screenshot)

            val intent = Intent(context, CircleSearchActivity::class.java).apply {
                putExtra(NavigationGestureService.EXTRA_SESSION_ID, session.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
            hide()
        } else {
            Log.w(TAG, "Assist screenshot is null or unavailable")
            hide()
        }
    }
}

