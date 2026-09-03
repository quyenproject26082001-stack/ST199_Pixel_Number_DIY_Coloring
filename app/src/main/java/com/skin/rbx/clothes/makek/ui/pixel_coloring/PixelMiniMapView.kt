package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PixelMiniMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var level: PixelLevel? = null
    private var painted: BooleanArray? = null
    private var imageScale = 1f
    private var imageOffsetX = 0f
    private var imageOffsetY = 0f
    private var viewportWidth = 0
    private var viewportHeight = 0

    fun update(
        level: PixelLevel,
        painted: BooleanArray,
        imageScale: Float,
        imageOffsetX: Float,
        imageOffsetY: Float,
        viewportWidth: Int,
        viewportHeight: Int,
    ) {
        this.level = level
        this.painted = painted
        this.imageScale = imageScale
        this.imageOffsetX = imageOffsetX
        this.imageOffsetY = imageOffsetY
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        val zoomed = level.width * imageScale > viewportWidth * 1.2f ||
            level.height * imageScale > viewportHeight * 1.2f
        visibility = if (zoomed) VISIBLE else GONE
        if (zoomed) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = level ?: return
        val state = painted ?: return
        val mapScale = min(width.toFloat() / data.width, height.toFloat() / data.height)
        val left = (width - data.width * mapScale) / 2f
        val top = (height - data.height * mapScale) / 2f

        for (y in 0 until data.height) {
            for (x in 0 until data.width) {
                val colorId = data.grid[y][x]
                if (colorId == 0) continue
                paint.style = Paint.Style.FILL
                paint.color = if (state[y * data.width + x]) {
                    parseColor(data.palette[colorId - 1])
                } else {
                    0x22FFFFFF
                }
                canvas.drawRect(
                    left + x * mapScale,
                    top + y * mapScale,
                    left + (x + 1) * mapScale,
                    top + (y + 1) * mapScale,
                    paint,
                )
            }
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        canvas.drawRect(
            left + (-imageOffsetX / imageScale) * mapScale,
            top + (-imageOffsetY / imageScale) * mapScale,
            left + ((-imageOffsetX + viewportWidth) / imageScale) * mapScale,
            top + ((-imageOffsetY + viewportHeight) / imageScale) * mapScale,
            paint,
        )
    }

    private fun parseColor(value: String): Int = runCatching { Color.parseColor(value) }.getOrDefault(Color.WHITE)
}
