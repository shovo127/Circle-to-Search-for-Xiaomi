package dev.shovo.circlesearch

import org.junit.Assert.*
import org.junit.Test

/**
 * Validates the actual production SelectionMath coordinate transform and boundary validation engine.
 */
class SelectionMathTest {

    @Test
    fun testFitCenterUniformScalingPortrait() {
        val bmpWidth = 1080f
        val bmpHeight = 2400f
        val viewWidth = 1080f
        val viewHeight = 2400f

        val transform = SelectionMath.computeFitCenterTransform(bmpWidth, bmpHeight, viewWidth, viewHeight)

        assertEquals(1.0f, transform.scale, 0.001f)
        assertEquals(0f, transform.dx, 0.001f)
        assertEquals(0f, transform.dy, 0.001f)
    }

    @Test
    fun testFitCenterLetterboxedLandscapeOnPortraitView() {
        val bmpWidth = 2400f
        val bmpHeight = 1080f
        val viewWidth = 1080f
        val viewHeight = 2400f

        val transform = SelectionMath.computeFitCenterTransform(bmpWidth, bmpHeight, viewWidth, viewHeight)

        assertEquals(0.45f, transform.scale, 0.001f)
        assertEquals(0f, transform.dx, 0.001f)
        assertTrue("Top letterbox offset must be positive", transform.dy > 0f)

        // Point inside the top letterbox (above actual image content)
        val touchInLetterboxY = 100f
        assertTrue("Touch is in top letterbox bar", touchInLetterboxY < transform.dy)

        // Map touch into content space
        val mapped = SelectionMath.mapViewToContentSpace(
            viewLeft = 0f,
            viewTop = touchInLetterboxY,
            viewRight = 100f,
            viewBottom = touchInLetterboxY + 50f,
            transform = transform
        )
        assertTrue("Mapped coordinate in letterbox must be negative in bitmap space", mapped.top < 0f)

        // Entire selection in letterbox must be rejected by clampAndValidateCrop
        val validated = SelectionMath.clampAndValidateCrop(mapped, bmpWidth, bmpHeight, minDimension = 10f)
        assertNull("Selection entirely within letterbox area must not yield valid crop", validated)
    }

    @Test
    fun testSelectionClampedToValidBitmapContent() {
        val bmpWidth = 1080f
        val bmpHeight = 2400f

        // Selection partially overlapping the right and bottom boundaries
        val overlapping = SelectionMath.CropRect(
            left = 900f,
            top = 500f,
            right = 1200f,
            bottom = 800f
        )
        val clamped = SelectionMath.clampAndValidateCrop(overlapping, bmpWidth, bmpHeight, minDimension = 10f)

        assertNotNull("Overlapping selection must yield valid clamped crop", clamped)
        assertEquals(900f, clamped!!.left, 0.001f)
        assertEquals(500f, clamped.top, 0.001f)
        assertEquals(1080f, clamped.right, 0.001f) // Clamped to content width
        assertEquals(800f, clamped.bottom, 0.001f)
        assertEquals(180f, clamped.width, 0.001f)
        assertEquals(300f, clamped.height, 0.001f)
    }

    @Test
    fun testTinySelectionRejectedBelowThreshold() {
        val bmpWidth = 1080f
        val bmpHeight = 2400f

        // 5x5 px micro-selection
        val tiny = SelectionMath.CropRect(
            left = 1075f,
            top = 500f,
            right = 1080f,
            bottom = 505f
        )
        val result = SelectionMath.clampAndValidateCrop(tiny, bmpWidth, bmpHeight, minDimension = 10f)
        assertNull("Sub-10px selection must be rejected by production validator", result)
    }

    @Test
    fun testFullPipelineComputeCrop() {
        // Full pipeline test mapping view-space coordinates to clamped bitmap crop
        val crop = SelectionMath.computeCrop(
            viewLeft = 100f,
            viewTop = 200f,
            viewRight = 300f,
            viewBottom = 400f,
            contentWidth = 1080f,
            contentHeight = 2400f,
            viewWidth = 1080f,
            viewHeight = 2400f,
            minDimension = 10f
        )

        assertNotNull(crop)
        assertEquals(100f, crop!!.left, 0.001f)
        assertEquals(200f, crop.top, 0.001f)
        assertEquals(300f, crop.right, 0.001f)
        assertEquals(400f, crop.bottom, 0.001f)
        assertEquals(200f, crop.width, 0.001f)
        assertEquals(200f, crop.height, 0.001f)
    }
}
