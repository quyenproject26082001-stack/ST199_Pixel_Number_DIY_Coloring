package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PixelPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var level: PixelLevel? = null
    private var painted: BooleanArray? = null
    private var showCompleted = false

    fun setLevel(level: PixelLevel, painted: BooleanArray? = null, showCompleted: Boolean = false) {
        this.level = level
        this.painted = painted
        this.showCompleted = showCompleted
        invalidate()
    }

    fun clearLevel() {
        level = null
        painted = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = level ?: return
        val scale = min(width.toFloat() / data.width, height.toFloat() / data.height)
        val left = (width - data.width * scale) / 2f
        val top = (height - data.height * scale) / 2f
        for (y in 0 until data.height) {
            for (x in 0 until data.width) {
                val colorId = data.grid[y][x]
                if (colorId == 0) continue
                val index = y * data.width + x
                paint.color = if (showCompleted || painted?.getOrNull(index) == true) {
                    parseColor(data.palette[colorId - 1])
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
    }

    private fun parseColor(value: String): Int = runCatching { Color.parseColor(value) }.getOrDefault(Color.WHITE)
}
