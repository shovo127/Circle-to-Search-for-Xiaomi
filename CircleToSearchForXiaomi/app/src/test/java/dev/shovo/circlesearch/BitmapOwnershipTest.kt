package dev.shovo.circlesearch

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BitmapOwnershipTest {

    @Test
    fun fullScreenCropMustNotRecycleTheSessionSourceBitmap() {
        val source = mock(Bitmap::class.java)
        assertFalse(LensSearchHelper.shouldRecycleCroppedBitmap(source, source))
    }

    @Test
    fun distinctCropBitmapCanBeRecycledAfterSaving() {
        val source = mock(Bitmap::class.java)
        val cropped = mock(Bitmap::class.java)
        assertTrue(LensSearchHelper.shouldRecycleCroppedBitmap(source, cropped))
    }
}
