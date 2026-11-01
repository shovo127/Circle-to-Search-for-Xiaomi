package dev.shovo.circlesearch

import android.graphics.Bitmap
import android.util.Log
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe, request-scoped session registry for Circle to Search for Xiaomi.
 *
 * Implements:
 * 1. Concurrent registry mapping unique session IDs to isolated session leases.
 * 2. Immutable session ID propagation via Intent extras.
 * 3. Idempotent reference counting / lease mechanism ensuring a bitmap is NEVER recycled
 *    while an Activity or background worker holds an active lease.
 * 4. Bounded TTL expiration (5 minutes) for abandoned sessions with deferred cleanup.
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

    /**
     * Idempotent lease token ensuring exact single-release semantics per owner.
     */
    class Lease internal constructor(
        val session: Session,
        val ownerTag: String
    ) : Closeable, AutoCloseable {
        private val released = AtomicBoolean(false)

        val isReleased: Boolean
            get() = released.get()

        val bitmap: Bitmap?
            get() = if (!released.get()) session.bitmap else null

        fun release() {
            if (released.compareAndSet(false, true)) {
                session.internalReleaseLease(ownerTag)
            }
        }

        override fun close() {
            release()
        }
    }

    class Session internal constructor(
        val id: String = UUID.randomUUID().toString(),
        val createdAt: Long = System.currentTimeMillis()
    ) {
        @Volatile
        var state: State = State.CAPTURING
            private set

        @Volatile
        var bitmap: Bitmap? = null
            private set

        @Volatile
        var errorMessage: String? = null
            private set

        private var activeLeases = 0

        @Synchronized
        fun acquireLease(ownerTag: String = "anonymous"): Lease? {
            if (state == State.RELEASED || state == State.EXPIRED || state == State.FAILED) {
                Log.w(TAG, "Cannot acquire lease on session [$id] in terminal state $state")
                return null
            }
            activeLeases++
            Log.d(TAG, "Acquired lease for [$id] by [$ownerTag], activeLeases=$activeLeases")
            return Lease(this, ownerTag)
        }

        @Synchronized
        internal fun internalReleaseLease(ownerTag: String) {
            if (activeLeases > 0) {
                activeLeases--
            } else {
                Log.w(TAG, "Warning: release called on session [$id] with 0 active leases")
            }
            Log.d(TAG, "Released lease for [$id] by [$ownerTag], remaining=$activeLeases, state=$state")

            if (activeLeases == 0) {
                if (state == State.EXPIRED || state == State.RELEASED || state == State.FAILED) {
                    destroyBitmapInternal()
                    sessions.remove(id)
                }
            }
        }

        @Synchronized
        fun closeSession() {
            if (state == State.RELEASED) return
            Log.d(TAG, "closeSession called on [$id], activeLeases=$activeLeases")
            state = State.RELEASED
            if (activeLeases == 0) {
                destroyBitmapInternal()
                sessions.remove(id)
            }
        }

        @Synchronized
        internal fun expire() {
            if (state == State.RELEASED || state == State.EXPIRED || state == State.FAILED) return
            Log.d(TAG, "Session [$id] marked EXPIRED")
            state = State.EXPIRED
            if (activeLeases == 0) {
                destroyBitmapInternal()
                sessions.remove(id)
            }
        }

        @Synchronized
        internal fun complete(bmp: Bitmap): Boolean {
            if (state != State.CAPTURING) {
                Log.w(TAG, "Session [$id] is in state $state (not CAPTURING); rejecting complete")
                return false
            }
            bitmap = bmp
            state = State.READY
            Log.d(TAG, "Session [$id] transitioned to READY (${bmp.width}x${bmp.height})")
            return true
        }

        @Synchronized
        internal fun completeWithoutBitmapForTest(): Boolean {
            if (state != State.CAPTURING) return false
            state = State.READY
            return true
        }

        @Synchronized
        internal fun fail(reason: String): Boolean {
            if (state != State.CAPTURING) {
                Log.w(TAG, "Session [$id] is in state $state (not CAPTURING); rejecting fail")
                return false
            }
            errorMessage = reason
            state = State.FAILED
            Log.w(TAG, "Session [$id] transitioned to FAILED: $reason")
            if (activeLeases == 0) {
                destroyBitmapInternal()
                sessions.remove(id)
            }
            return true
        }

        private fun destroyBitmapInternal() {
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
        if (session != null) {
            val completed = session.complete(bmp)
            if (!completed && !bmp.isRecycled) {
                bmp.recycle()
            }
        } else {
            Log.w(TAG, "Session [$sessionId] not found in registry; discarding incoming bitmap")
            if (!bmp.isRecycled) {
                bmp.recycle()
            }
        }
    }

    fun failCapture(sessionId: String, errorMsg: String) {
        val session = sessions[sessionId]
        if (session != null) {
            session.fail(errorMsg)
        } else {
            Log.w(TAG, "Session [$sessionId] not found for failure notification")
        }
    }

    fun releaseSession(sessionId: String?) {
        if (sessionId.isNullOrEmpty()) return
        val session = sessions[sessionId] ?: return
        session.closeSession()
    }

    private fun cleanExpiredSessions() {
        val now = System.currentTimeMillis()
        val iterator = sessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val s = entry.value
            if (now - s.createdAt > SESSION_TTL_MS) {
                Log.d(TAG, "Purging expired session [${s.id}]")
                s.expire()
                if (s.state == State.RELEASED) {
                    iterator.remove()
                }
            }
        }
    }
}
