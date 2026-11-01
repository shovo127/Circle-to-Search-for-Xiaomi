package dev.shovo.circlesearch

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SearchSessionManagerTest {

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
        // Simulate complete without actual Android Bitmap (null for JVM unit test)
        s.completeWithoutBitmapForTest()

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
        s.completeWithoutBitmapForTest()

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
        val completed = s.completeWithoutBitmapForTest()
        assertFalse("Late capture must be rejected", completed)
        assertNotEquals(SearchSessionManager.State.READY, s.state)
    }

    @Test
    fun testConcurrentLeaseAcquireAndRelease() {
        val s = SearchSessionManager.createSession()
        s.completeWithoutBitmapForTest()

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
}
