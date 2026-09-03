package com.skin.rbx.clothes.makek.core.custom.loading

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

class GuideLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = SOURCE_BORDER
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ORANGE
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val borderRect = RectF()
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, WAITING_PROGRESS).apply {
        duration = 1400L
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    private var finishAnimator: ValueAnimator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val desiredWidth = if (measuredWidth > 0) measuredWidth else SOURCE_WIDTH.toInt()
        val desiredHeight = (desiredWidth * SOURCE_HEIGHT / SOURCE_WIDTH).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scale = min(width / SOURCE_WIDTH, height / SOURCE_HEIGHT)
        val left = (width - SOURCE_WIDTH * scale) / 2f
        val top = (height - SOURCE_HEIGHT * scale) / 2f

        borderPaint.strokeWidth = SOURCE_BORDER * scale
        borderRect.set(
            left + borderPaint.strokeWidth / 2f,
            top + borderPaint.strokeWidth / 2f,
            left + SOURCE_WIDTH * scale - borderPaint.strokeWidth / 2f,
            top + SOURCE_HEIGHT * scale - borderPaint.strokeWidth / 2f
        )
        canvas.drawRect(left, top, left + SOURCE_WIDTH * scale, top + SOURCE_HEIGHT * scale, backgroundPaint)
        canvas.drawRect(borderRect, borderPaint)

        val totalBlocks = calculateBlockCount()
        val visibleBlocks = floor(progress * totalBlocks).toInt().coerceIn(0, totalBlocks)
        val blockRightLimit = left + (SOURCE_WIDTH - SOURCE_INSET) * scale
        for (index in 0 until visibleBlocks) {
            val blockLeft = left + (SOURCE_INSET + index * SOURCE_STEP) * scale
            val blockTop = top + SOURCE_INSET * scale
            canvas.drawRect(
                blockLeft,
                blockTop,
                min(blockLeft + SOURCE_BLOCK * scale, blockRightLimit),
                blockTop + SOURCE_BLOCK * scale,
                fillPaint
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) animator.start()
    }

    override fun onDetachedFromWindow() {
        finishAnimator?.cancel()
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun finishLoading(durationMs: Long = 500L, onFinished: () -> Unit) {
        if (progress >= 1f) {
            onFinished()
            return
        }
        animator.cancel()
        finishAnimator?.cancel()
        var isCancelled = false
        finishAnimator = ValueAnimator.ofFloat(progress, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) onFinished()
                }
            })
            start()
        }
    }

    private fun calculateBlockCount(): Int {
        return ceil((SOURCE_WIDTH - SOURCE_INSET * 2) / SOURCE_STEP).toInt()
    }

    companion object {
        private const val SOURCE_WIDTH = 480f
        private const val SOURCE_HEIGHT = 32f
        private const val SOURCE_BORDER = 2f
        private const val SOURCE_INSET = 6f
        private const val SOURCE_BLOCK = 20f
        private const val SOURCE_GAP = 4f
        private const val SOURCE_STEP = SOURCE_BLOCK + SOURCE_GAP
        private const val WAITING_PROGRESS = 0.7f
        private const val ORANGE = 0xFFFF8D1A.toInt()
    }
}
