package com.skin.rbx.clothes.makek.core.custom.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/** Clips child content to the filled silhouette used by bg_pixel_1. */
class PixelBorderClipLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val contentClipPath = Path()

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return

        val scaleX = width / VIEWPORT_SIZE
        val scaleY = height / VIEWPORT_SIZE

        contentClipPath.apply {
            reset()
            moveTo(7.6f * scaleX, 0f)
            lineTo(148.4f * scaleX, 0f)
            lineTo(148.4f * scaleX, 3.8f * scaleY)
            lineTo(152.2f * scaleX, 3.8f * scaleY)
            lineTo(152.2f * scaleX, 7.6f * scaleY)
            lineTo(width.toFloat(), 7.6f * scaleY)
            lineTo(width.toFloat(), 148.4f * scaleY)
            lineTo(152.2f * scaleX, 148.4f * scaleY)
            lineTo(152.2f * scaleX, 152.2f * scaleY)
            lineTo(148.4f * scaleX, 152.2f * scaleY)
            lineTo(148.4f * scaleX, height.toFloat())
            lineTo(7.6f * scaleX, height.toFloat())
            lineTo(7.6f * scaleX, 152.2f * scaleY)
            lineTo(3.8f * scaleX, 152.2f * scaleY)
            lineTo(3.8f * scaleX, 148.4f * scaleY)
            lineTo(0f, 148.4f * scaleY)
            lineTo(0f, 7.6f * scaleY)
            lineTo(3.8f * scaleX, 7.6f * scaleY)
            lineTo(3.8f * scaleX, 3.8f * scaleY)
            lineTo(7.6f * scaleX, 3.8f * scaleY)
            close()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val saveCount = canvas.save()
        canvas.clipPath(contentClipPath)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    private companion object {
        const val VIEWPORT_SIZE = 156f
    }
}
