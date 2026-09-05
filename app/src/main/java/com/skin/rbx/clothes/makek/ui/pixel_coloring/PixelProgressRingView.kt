package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.skin.rbx.clothes.makek.core.helper.UnitHelper
import kotlin.math.min

class PixelProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val strokeWidth = UnitHelper.dpToPx(resources, 3f)
    private val progressBounds = RectF()
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#891903")
        style = Paint.Style.STROKE
        this.strokeWidth = this@PixelProgressRingView.strokeWidth
        strokeCap = Paint.Cap.BUTT
    }

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0) return

        val diameter = min(width, height).toFloat() - strokeWidth
        val left = (width - diameter) / 2f
        val top = (height - diameter) / 2f
        progressBounds.set(left, top, left + diameter, top + diameter)
        canvas.drawArc(
            progressBounds,
            START_ANGLE,
            -FULL_SWEEP * progress / 100f,
            false,
            progressPaint,
        )
    }

    private companion object {
        const val START_ANGLE = -90f
        const val FULL_SWEEP = 360f
    }
}
