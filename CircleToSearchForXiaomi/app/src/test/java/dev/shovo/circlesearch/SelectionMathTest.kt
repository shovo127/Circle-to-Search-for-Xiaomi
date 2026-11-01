package dev.shovo.circlesearch

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class SelectionMathTest {

    data class RectMath(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = max(0f, right - left)
        val height: Float get() = max(0f, bottom - top)

        fun intersectWith(other: RectMath): RectMath? {
            val iLeft = max(this.left, other.left)
            val iTop = max(this.top, other.top)
            val iRight = min(this.right, other.right)
            val iBottom = min(this.bottom, other.bottom)

            return if (iLeft < iRight && iTop < iBottom) {
                RectMath(iLeft, iTop, iRight, iBottom)
            } else {
                null
            }
        }
    }

    @Test
    fun testFitCenterUniformScalingPortrait() {
        val bmpWidth = 1080f
        val bmpHeight = 2400f
        val viewWidth = 1080f
        val viewHeight = 2400f

        // 1:1 match
        val scale = min(viewWidth / bmpWidth, viewHeight / bmpHeight)
        val dx = (viewWidth - bmpWidth * scale) / 2f
        val dy = (viewHeight - bmpHeight * scale) / 2f

        assertEquals(1.0f, scale, 0.001f)
        assertEquals(0f, dx, 0.001f)
        assertEquals(0f, dy, 0.001f)
    }

    @Test
    fun testFitCenterLetterboxedLandscapeOnPortraitView() {
        val bmpWidth = 2400f
        val bmpHeight = 1080f
        val viewWidth = 1080f
        val viewHeight = 2400f

        val scale = min(viewWidth / bmpWidth, viewHeight / bmpHeight)
        val scaledW = bmpWidth * scale
        val scaledH = bmpHeight * scale
        val dx = (viewWidth - scaledW) / 2f
        val dy = (viewHeight - scaledH) / 2f

        assertEquals(0.45f, scale, 0.001f)
        assertEquals(0f, dx, 0.001f)
        assertTrue("Top letterbox offset must be positive", dy > 0f)

        // Point inside the letterbox (above image content)
        val touchInLetterboxY = 100f
        assertTrue("Touch is in top letterbox", touchInLetterboxY < dy)

        // Mapping letterbox touch to bitmap space produces negative coordinate
        val mappedY = (touchInLetterboxY - dy) / scale
        assertTrue("Mapped coordinate outside bitmap is negative", mappedY < 0f)

        // Intersection test must reject selections completely in letterbox
        val selection = RectMath(0f, mappedY, 100f, mappedY + 50f)
        val bmpBounds = RectMath(0f, 0f, bmpWidth, bmpHeight)
        assertNull("Selection in letterbox must not intersect bitmap content", selection.intersectWith(bmpBounds))
    }

    @Test
    fun testSelectionClampedToValidBitmapContent() {
        val bmpBounds = RectMath(0f, 0f, 1080f, 2400f)

        // Selection partially overlapping the right edge
        val overlappingSelection = RectMath(900f, 500f, 1200f, 800f)
        val intersected = overlappingSelection.intersectWith(bmpBounds)

        assertNotNull(intersected)
        assertEquals(900f, intersected!!.left, 0.001f)
        assertEquals(500f, intersected.top, 0.001f)
        assertEquals(1080f, intersected.right, 0.001f) // Clamped to width
        assertEquals(800f, intersected.bottom, 0.001f)
        assertEquals(180f, intersected.width, 0.001f)
        assertEquals(300f, intersected.height, 0.001f)
    }

    @Test
    fun testTinySelectionRejectedBelowThreshold() {
        val bmpBounds = RectMath(0f, 0f, 1080f, 2400f)
        val tinySelection = RectMath(1075f, 500f, 1080f, 505f) // 5x5px
        val intersected = tinySelection.intersectWith(bmpBounds)

        assertNotNull(intersected)
        val isTooSmall = intersected!!.width < 10f || intersected.height < 10f
        assertTrue("Sub-10px selection must be rejected", isTooSmall)
    }
}
