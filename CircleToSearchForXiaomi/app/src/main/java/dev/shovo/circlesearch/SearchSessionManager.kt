package dev.shovo.circlesearch

import android.graphics.Bitmap
import android.util.Log
import java.util.UUID

/**
 * Manages request-specific screenshot sessions for Shovo Circle Search.
 *
 * Replaces dangerous static mutable Bitmap state with an explicit, thread-safe
 * session lifecycle model. Guarantees:
 * 1. Exactly one active capture session per user gesture.
 * 2. Automatic recycling of previous session bitmaps to prevent memory leaks.
 * 3. Prevention of recursive relaunch loops if screen capture fails.
 * 4. Stale session invalidation upon Activity destruction or cancellation.
 */
object SearchSessionManager {

    private const val TAG = "SearchSessionManager"

    data class Session(
        val id: String = UUID.randomUUID().toString(),
        val createdAt: Long = System.currentTimeMillis(),
        var bitmap: Bitmap? = null,
        var errorMessage: String? = null
    )

    private var activeSession: Session? = null

    /**
     * Initializes a brand-new session for a newly triggered gesture.
     * Safely recycles any existing bitmap from an unconsumed session.
     */
    @Synchronized
    fun createNewSession(): Session {
        activeSession?.bitmap?.let { oldBmp ->
            if (!oldBmp.isRecycled) {
                Log.d(TAG, "Recycling prior unused session bitmap")
                oldBmp.recycle()
            }
        }
        val session = Session()
        activeSession = session
        Log.d(TAG, "Created new SearchSession [${session.id}]")
        return session
    }

    /**
     * Retrieves the currently active session, or null if none is pending.
     */
    @Synchronized
    fun getActiveSession(): Session? {
        return activeSession
    }

    /**
     * Associates a successfully decoded screenshot Bitmap with the target session.
     * If the session ID has changed (e.g. user re-triggered), the bitmap is discarded.
     */
    @Synchronized
    fun setSessionBitmap(sessionId: String, bmp: Bitmap) {
        val current = activeSession
        if (current != null && current.id == sessionId) {
            current.bitmap = bmp
            current.errorMessage = null
            Log.d(TAG, "Attached screenshot ${bmp.width}x${bmp.height} to session [$sessionId]")
        } else {
            Log.w(TAG, "Session mismatch or expired [$sessionId vs ${current?.id}]; recycling bitmap")
            if (!bmp.isRecycled) {
                bmp.recycle()
            }
        }
    }

    /**
     * Records a capture failure message for the target session.
     */
    @Synchronized
    fun setSessionError(sessionId: String, errorMsg: String) {
        val current = activeSession
        if (current != null && current.id == sessionId) {
            current.errorMessage = errorMsg
            Log.w(TAG, "Session [$sessionId] marked with error: $errorMsg")
        }
    }

    /**
     * Clears and releases all resources associated with the active session.
     */
    @Synchronized
    fun clearSession() {
        activeSession?.bitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                bmp.recycle()
            }
        }
        Log.d(TAG, "Cleared active SearchSession [${activeSession?.id}]")
        activeSession = null
    }
}

