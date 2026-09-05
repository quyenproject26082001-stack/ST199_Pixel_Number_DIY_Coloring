package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class PixelPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {
    fun setLevel(level: PixelLevel, painted: BooleanArray? = null, showCompleted: Boolean = false) {
        setPreviewBitmap(renderPixelPreview(level, painted, showCompleted))
    }

    fun setPreviewBitmap(bitmap: Bitmap?) {
        setImageBitmap(bitmap)
    }

    fun clearLevel() {
        setImageDrawable(null)
    }
}

internal fun renderPixelPreview(
    level: PixelLevel,
    painted: BooleanArray? = null,
    showCompleted: Boolean = false,
    targetSize: Int = 256,
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    val colors = level.palette.map { value ->
        runCatching { Color.parseColor(value) }.getOrDefault(Color.WHITE)
    }
    val scale = min(targetSize.toFloat() / level.width, targetSize.toFloat() / level.height)
    val left = (targetSize - level.width * scale) / 2f
    val top = (targetSize - level.height * scale) / 2f

    for (y in 0 until level.height) {
        for (x in 0 until level.width) {
            val colorId = level.grid[y][x]
            if (colorId == 0) continue
            val index = y * level.width + x
            paint.color = if (showCompleted || painted?.getOrNull(index) == true) {
                colors.getOrElse(colorId - 1) { Color.WHITE }
            } else {
                Color.WHITE
            }
            canvas.drawRect(
                left + x * scale,
                top + y * scale,
                left + (x + 1) * scale,
                top + (y + 1) * scale,
                paint,
            )
        }
    }
    return bitmap
}
