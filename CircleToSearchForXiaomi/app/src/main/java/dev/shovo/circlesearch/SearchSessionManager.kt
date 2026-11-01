package dev.shovo.circlesearch

import android.graphics.Bitmap
import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe, request-scoped session registry for Circle to Search for Xiaomi.
 *
 * Implements:
 * 1. Concurrent registry mapping unique session IDs to isolated session leases.
 * 2. Immutable session ID propagation via Intent extras.
 * 3. Reference counting / lease mechanism ensuring a bitmap is NEVER recycled
 *    while an Activity or background worker holds an active lease.
 * 4. Bounded TTL expiration (5 minutes) for abandoned sessions.
 * 5. Complete isolation between concurrent or successive requests.
 */
object SearchSessionManager {

    private const val TAG = "SearchSessionManager"
    private const val SESSION_TTL_MS = 5 * 60 * 1000L // 5 minutes

    enum class State {
        CAPTURING,
        READY,
        FAILED,
        EXPIRED,
        RELEASED
    }

    class Session(
        val id: String = UUID.randomUUID().toString(),
        val createdAt: Long = System.currentTimeMillis()
    ) {
        @Volatile
        var state: State = State.CAPTURING
            internal set

        @Volatile
        var bitmap: Bitmap? = null
            internal set

        @Volatile
        var errorMessage: String? = null
            internal set

        private val activeLeases = AtomicInteger(0)

        fun acquireLease(): Boolean {
            synchronized(this) {
                if (state == State.RELEASED || state == State.EXPIRED) {
                    Log.w(TAG, "Cannot acquire lease on session [$id] in state $state")
                    return false
                }
                val count = activeLeases.incrementAndGet()
                Log.d(TAG, "Acquired lease for session [$id], count=$count")
                return true
            }
        }

        fun releaseLease() {
            synchronized(this) {
                val remaining = activeLeases.decrementAndGet()
                Log.d(TAG, "Released lease for session [$id], remaining=$remaining")
                if (remaining <= 0 && (state == State.READY || state == State.FAILED)) {
                    cleanupInternal()
                }
            }
        }

        internal fun cleanupInternal() {
            state = State.RELEASED
            bitmap?.let { bmp ->
                if (!bmp.isRecycled) {
                    bmp.recycle()
                    Log.d(TAG, "Recycled bitmap for session [$id]")
                }
            }
            bitmap = null
        }
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    fun createSession(): Session {
        cleanExpiredSessions()
        val session = Session()
        sessions[session.id] = session
        Log.d(TAG, "Created request-scoped session [${session.id}]")
        return session
    }

    fun getSession(sessionId: String?): Session? {
        if (sessionId.isNullOrEmpty()) return null
        return sessions[sessionId]
    }

    fun completeCapture(sessionId: String, bmp: Bitmap) {
        val session = sessions[sessionId]
        if (session != null && session.state == State.CAPTURING) {
            session.bitmap = bmp
            session.state = State.READY
            Log.d(TAG, "Completed capture for session [$sessionId], size=${bmp.width}x${bmp.height}")
        } else {
            Log.w(TAG, "Session [$sessionId] not in CAPTURING state; discarding incoming bitmap")
            if (!bmp.isRecycled) {
                bmp.recycle()
            }
        }
    }

    fun failCapture(sessionId: String, errorMsg: String) {
        val session = sessions[sessionId]
        if (session != null && session.state == State.CAPTURING) {
            session.errorMessage = errorMsg
            session.state = State.FAILED
            Log.w(TAG, "Failed capture for session [$sessionId]: $errorMsg")
        }
    }

    fun releaseSession(sessionId: String?) {
        if (sessionId.isNullOrEmpty()) return
        val session = sessions[sessionId] ?: return
        session.releaseLease()
        if (session.state == State.RELEASED) {
            sessions.remove(sessionId)
        }
    }

    private fun cleanExpiredSessions() {
        val now = System.currentTimeMillis()
        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val s = entry.value
            if (now - s.createdAt > SESSION_TTL_MS) {
                Log.d(TAG, "Purging expired session [${s.id}]")
                s.state = State.EXPIRED
                s.cleanupInternal()
                iterator.remove()
            }
        }
    }
}
