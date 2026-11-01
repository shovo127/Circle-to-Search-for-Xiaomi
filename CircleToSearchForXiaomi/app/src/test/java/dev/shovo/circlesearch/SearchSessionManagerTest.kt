package dev.shovo.circlesearch

import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SearchSessionManagerTest {

    @Before
    fun setUp() {
        SearchSessionManager.resetForTest()
    }

    @After
    fun tearDown() {
        SearchSessionManager.resetForTest()
    }

    private fun createMockBitmap(): Bitmap {
        return mock(Bitmap::class.java)
    }

    @Test
    fun testSessionCreationAndUniqueIds() {
        val s1 = SearchSessionManager.createSession()
        val s2 = SearchSessionManager.createSession()

        assertNotNull(s1.id)
        assertNotNull(s2.id)
        assertNotEquals(s1.id, s2.id)
        assertEquals(SearchSessionManager.State.CAPTURING, s1.state)
        assertEquals(SearchSessionManager.State.CAPTURING, s2.state)
    }

    @Test
    fun testCaptureFailureTransitionsToFailed() {
        val s = SearchSessionManager.createSession()
        SearchSessionManager.failCapture(s.id, "Test screenshot error")

        assertEquals(SearchSessionManager.State.FAILED, s.state)
        assertEquals("Test screenshot error", s.errorMessage)
    }

    @Test
    fun testIdempotentLeaseRelease() {
        val s = SearchSessionManager.createSession()
        s.complete(createMockBitmap())

        val lease1 = s.acquireLease("Owner1")
        assertNotNull(lease1)
        assertFalse(lease1!!.isReleased)

        // Multiple calls to release must be idempotent
        lease1.release()
        assertTrue(lease1.isReleased)

        lease1.release() // Should not underflow
        lease1.close()   // AutoCloseable should also not underflow

        // Acquire new lease to verify state
        val lease2 = s.acquireLease("Owner2")
        assertNotNull(lease2)
        lease2!!.release()
    }

    @Test
    fun testCannotAcquireLeaseOnTerminalSession() {
        val s = SearchSessionManager.createSession()
        s.closeSession()

        val lease = s.acquireLease("NewOwner")
        assertNull("Should not acquire lease on closed session", lease)
    }

    @Test
    fun testDeferredExpirationWhileLeaseActive() {
        val s = SearchSessionManager.createSession()
        s.complete(createMockBitmap())

        val lease = s.acquireLease("ActiveWorker")
        assertNotNull(lease)

        // Expire session while lease is held
        s.expire()
        assertEquals(SearchSessionManager.State.EXPIRED, s.state)

        // New lease acquisition must be rejected
        val newLease = s.acquireLease("ForbiddenWorker")
        assertNull("Should not acquire lease on expired session", newLease)

        // Releasing the existing lease triggers terminal cleanup
        lease!!.release()
        assertEquals(SearchSessionManager.State.EXPIRED, s.state)
        assertNull("Session must be removed from registry after cleanup", SearchSessionManager.getSession(s.id))
    }

    @Test
    fun testLateCaptureRejectedWhenSessionTerminal() {
        val s = SearchSessionManager.createSession()
        s.fail("Cancelled before capture")

        // Late callback should not transition back to READY
        val completed = s.complete(createMockBitmap())
        assertFalse("Late capture must be rejected", completed)
        assertNotEquals(SearchSessionManager.State.READY, s.state)
    }

    @Test
    fun testConcurrentLeaseAcquireAndRelease() {
        val s = SearchSessionManager.createSession()
        s.complete(createMockBitmap())

        val threadCount = 20
        val latch = CountDownLatch(threadCount)

        for (i in 0 until threadCount) {
            Thread {
                val lease = s.acquireLease("Thread-$i")
                if (lease != null) {
                    Thread.sleep(5)
                    lease.release()
                    lease.release() // Duplicate release test
                }
                latch.countDown()
            }.start()
        }

        assertTrue("All concurrent leases must complete", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testAutomaticExpiryWithMockClock() {
        var mockTime = 100_000L
        SearchSessionManager.timeProvider = { mockTime }

        val s = SearchSessionManager.createSession()
        s.complete(createMockBitmap())
        assertEquals(SearchSessionManager.State.READY, s.state)

        // Advance clock by 4 minutes (less than 5-minute TTL)
        mockTime += 4 * 60 * 1000L
        SearchSessionManager.cleanExpiredSessions()
        assertNotNull(SearchSessionManager.getSession(s.id))
        assertEquals(SearchSessionManager.State.READY, s.state)

        // Advance clock past 5 minutes (TTL exceeded)
        mockTime += 2 * 60 * 1000L
        SearchSessionManager.cleanExpiredSessions()

        // Abandoned session with 0 leases must be purged
        assertNull("Expired session must be purged from registry", SearchSessionManager.getSession(s.id))
        assertEquals(SearchSessionManager.State.EXPIRED, s.state)
    }

    @Test
    fun testIndependentConcurrentSessionsIsolation() {
        val s1 = SearchSessionManager.createSession()
        val s2 = SearchSessionManager.createSession()
        val bmp1 = createMockBitmap()
        val bmp2 = createMockBitmap()

        s1.complete(bmp1)
        s2.complete(bmp2)

        val lease1 = s1.acquireLease("Worker1")
        val lease2 = s2.acquireLease("Worker2")

        assertSame(bmp1, lease1?.bitmap)
        assertSame(bmp2, lease2?.bitmap)

        // Releasing and closing session 1 must have no impact on session 2
        lease1?.release()
        s1.closeSession()

        assertNull(SearchSessionManager.getSession(s1.id))
        assertNotNull(SearchSessionManager.getSession(s2.id))
        assertEquals(SearchSessionManager.State.READY, s2.state)
        assertFalse(lease2!!.isReleased)
        assertSame(bmp2, lease2.bitmap)

        lease2.release()
        s2.closeSession()
        assertNull(SearchSessionManager.getSession(s2.id))
    }
}
