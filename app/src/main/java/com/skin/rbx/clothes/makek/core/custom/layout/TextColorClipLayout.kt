package com.skin.rbx.clothes.makek.core.custom.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout

/** Clips children to the stepped silhouette shared by the text-color borders. */
class TextColorClipLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val contentClipPath = Path()

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return

        val scaleX = width / VIEWPORT_SIZE
        val scaleY = height / VIEWPORT_SIZE
        contentClipPath.apply {
            reset()
            moveTo(12.86f * scaleX, 0f)
            lineTo(23.14f * scaleX, 0f)
            lineTo(23.14f * scaleX, 2.57f * scaleY)
            lineTo(28.29f * scaleX, 2.57f * scaleY)
            lineTo(28.29f * scaleX, 5.14f * scaleY)
            lineTo(30.86f * scaleX, 5.14f * scaleY)
            lineTo(30.86f * scaleX, 7.71f * scaleY)
            lineTo(33.43f * scaleX, 7.71f * scaleY)
            lineTo(33.43f * scaleX, 12.86f * scaleY)
            lineTo(width.toFloat(), 12.86f * scaleY)
            lineTo(width.toFloat(), 23.14f * scaleY)
            lineTo(33.43f * scaleX, 23.14f * scaleY)
            lineTo(33.43f * scaleX, 28.29f * scaleY)
            lineTo(30.86f * scaleX, 28.29f * scaleY)
            lineTo(30.86f * scaleX, 30.86f * scaleY)
            lineTo(28.29f * scaleX, 30.86f * scaleY)
            lineTo(28.29f * scaleX, 33.43f * scaleY)
            lineTo(23.14f * scaleX, 33.43f * scaleY)
            lineTo(23.14f * scaleX, height.toFloat())
            lineTo(12.86f * scaleX, height.toFloat())
            lineTo(12.86f * scaleX, 33.43f * scaleY)
            lineTo(7.71f * scaleX, 33.43f * scaleY)
            lineTo(7.71f * scaleX, 30.86f * scaleY)
            lineTo(5.14f * scaleX, 30.86f * scaleY)
            lineTo(5.14f * scaleX, 28.29f * scaleY)
            lineTo(2.57f * scaleX, 28.29f * scaleY)
            lineTo(2.57f * scaleX, 23.14f * scaleY)
            lineTo(0f, 23.14f * scaleY)
            lineTo(0f, 12.86f * scaleY)
            lineTo(2.57f * scaleX, 12.86f * scaleY)
            lineTo(2.57f * scaleX, 7.71f * scaleY)
            lineTo(5.14f * scaleX, 7.71f * scaleY)
            lineTo(5.14f * scaleX, 5.14f * scaleY)
            lineTo(7.71f * scaleX, 5.14f * scaleY)
            lineTo(7.71f * scaleX, 2.57f * scaleY)
            lineTo(12.86f * scaleX, 2.57f * scaleY)
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
        const val VIEWPORT_SIZE = 36f
    }
}
