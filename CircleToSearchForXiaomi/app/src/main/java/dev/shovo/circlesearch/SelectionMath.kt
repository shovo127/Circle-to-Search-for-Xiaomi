package dev.shovo.circlesearch

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Pure mathematical geometry and coordinate transformations for Circle to Search for Xiaomi.
 *
 * Provides deterministic, framework-independent coordinate mapping between view coordinates
 * and bitmap pixel space, taking into account FIT_CENTER letterboxing, scaling ratios,
 * edge clamping, and selection threshold validation.
 */
object SelectionMath {

    data class CropRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = max(0f, right - left)
        val height: Float get() = max(0f, bottom - top)

        fun toRectF(): RectF = RectF(left, top, right, bottom)
    }

    data class FitCenterTransform(
        val scale: Float,
        val dx: Float,
        val dy: Float
    )

    /**
     * Computes uniform scale factor and centering offsets identical to Matrix.ScaleToFit.CENTER.
     */
    fun computeFitCenterTransform(
        contentWidth: Float,
        contentHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): FitCenterTransform {
        if (!contentWidth.isFinite() || !contentHeight.isFinite() || !viewWidth.isFinite() || !viewHeight.isFinite() ||
            contentWidth <= 0f || contentHeight <= 0f || viewWidth <= 0f || viewHeight <= 0f) {
            return FitCenterTransform(0f, 0f, 0f)
        }
        val scale = min(viewWidth / contentWidth, viewHeight / contentHeight)
        val dx = (viewWidth - contentWidth * scale) / 2f
        val dy = (viewHeight - contentHeight * scale) / 2f
        if (!scale.isFinite() || scale <= 0f || !dx.isFinite() || !dy.isFinite()) {
            return FitCenterTransform(0f, 0f, 0f)
        }
        return FitCenterTransform(scale, dx, dy)
    }

    /**
     * Maps view coordinates to bitmap space using the inverse of the FIT_CENTER transform.
     */
    fun mapViewToContentSpace(
        viewLeft: Float,
        viewTop: Float,
        viewRight: Float,
        viewBottom: Float,
        transform: FitCenterTransform
    ): CropRect {
        val s = transform.scale
        if (!s.isFinite() || s <= 0f ||
            !transform.dx.isFinite() || !transform.dy.isFinite() ||
            !viewLeft.isFinite() || !viewTop.isFinite() || !viewRight.isFinite() || !viewBottom.isFinite()) {
            return CropRect(0f, 0f, 0f, 0f)
        }

        val l = (viewLeft - transform.dx) / s
        val t = (viewTop - transform.dy) / s
        val r = (viewRight - transform.dx) / s
        val b = (viewBottom - transform.dy) / s

        return CropRect(
            left = min(l, r),
            top = min(t, b),
            right = max(l, r),
            bottom = max(t, b)
        )
    }

    /**
     * Clamps a content-space rectangle to valid bitmap boundaries and enforces a minimum dimension.
     * Returns null if the selection is completely outside the bitmap or smaller than [minDimension].
     */
    fun clampAndValidateCrop(
        mapped: CropRect,
        contentWidth: Float,
        contentHeight: Float,
        minDimension: Float = 10f
    ): CropRect? {
        if (!mapped.left.isFinite() || !mapped.top.isFinite() || !mapped.right.isFinite() || !mapped.bottom.isFinite() ||
            !contentWidth.isFinite() || !contentHeight.isFinite() || !minDimension.isFinite() ||
            contentWidth <= 0f || contentHeight <= 0f || minDimension <= 0f) {
            return null
        }
        val clampedLeft = max(0f, min(mapped.left, contentWidth))
        val clampedTop = max(0f, min(mapped.top, contentHeight))
        val clampedRight = max(0f, min(mapped.right, contentWidth))
        val clampedBottom = max(0f, min(mapped.bottom, contentHeight))

        val width = clampedRight - clampedLeft
        val height = clampedBottom - clampedTop

        if (width < minDimension || height < minDimension) {
            return null
        }
        return CropRect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    /**
     * Full pipeline: transforms view-space touch selection to valid bitmap crop bounds.
     */
    fun computeCrop(
        viewLeft: Float,
        viewTop: Float,
        viewRight: Float,
        viewBottom: Float,
        contentWidth: Float,
        contentHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        minDimension: Float = 10f
    ): CropRect? {
        if (contentWidth <= 0f || contentHeight <= 0f || viewWidth <= 0f || viewHeight <= 0f ||
            !contentWidth.isFinite() || !contentHeight.isFinite() || !viewWidth.isFinite() || !viewHeight.isFinite() ||
            !viewLeft.isFinite() || !viewTop.isFinite() || !viewRight.isFinite() || !viewBottom.isFinite()) {
            return null
        }
        val transform = computeFitCenterTransform(contentWidth, contentHeight, viewWidth, viewHeight)
        val mapped = mapViewToContentSpace(viewLeft, viewTop, viewRight, viewBottom, transform)
        return clampAndValidateCrop(mapped, contentWidth, contentHeight, minDimension)
    }

    /**
     * Convenience bridge for Android RectF instances.
     */
    fun computeCropRectF(
        viewRect: RectF,
        contentWidth: Float,
        contentHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        minDimension: Float = 10f
    ): RectF? {
        val result = computeCrop(
            viewRect.left,
            viewRect.top,
            viewRect.right,
            viewRect.bottom,
            contentWidth,
            contentHeight,
            viewWidth,
            viewHeight,
            minDimension
        )
        return result?.toRectF()
    }
}

