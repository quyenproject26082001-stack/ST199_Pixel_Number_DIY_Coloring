package com.skin.rbx.clothes.makek.ui.outfit.c253.seekbar
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.skin.rbx.clothes.makek.R


class SizeSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var minValue = 0

    // Track
    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG)
    private var trackPath = Path()

    // Thumb
    private var thumbDrawable: Drawable =
        ContextCompat.getDrawable(context, R.drawable.img_thumb)!!
    private var thumbSize = UnitHelper.dpToPx(resources, 16f)

    // Min/Max indicator
    private val minRadius = UnitHelper.dpToPx(resources, 1f)
    private val maxRadius = UnitHelper.dpToPx(resources, 6f)

    // Nền (fill)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.FILL
    }

    // Viền (stroke)
    private val strokeWidth = UnitHelper.dpToPx(resources, 1f)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }

    // Paint cho phần đã kéo (Progress)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.blue_000D4C)
        style = Paint.Style.FILL
    }

    // Paint cho phần nền chưa kéo (Track)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gray_CCCCCC)
        style = Paint.Style.FILL
    }

    // Các biến phụ trợ tính toán
    // Dùng maxRadius để đều 2 đầu
    private val startX get() = maxRadius
    private val endX get() = width - maxRadius

    private var progress = 50
    private var maxSize = 100

    var onSizeChanged: ((Int) -> Unit)? = null

    init {
        paintTrack.shader = LinearGradient(
            startX, 0f, endX, 0f,
            ContextCompat.getColor(context, R.color.white),
            ContextCompat.getColor(context, R.color.white),
            Shader.TileMode.CLAMP
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val usableWidth = endX - startX
        val ratio = progress / maxSize.toFloat()
        val thumbX = startX + ratio * usableWidth

        // 1. Tính toán độ cao tại vị trí Thumb (vì thanh trượt hình thang/tam giác)
        // Chiều cao tại start là minRadius*2, tại end là maxRadius*2
        val currentHalfHeight = minRadius + (maxRadius - minRadius) * ratio

        // 2. Vẽ phần nền xám (Phần chưa kéo - phía bên phải thumb)
        val backgroundPath = Path().apply {
            moveTo(thumbX, centerY - currentHalfHeight)
            lineTo(endX, centerY - maxRadius)
            lineTo(endX, centerY + maxRadius)
            lineTo(thumbX, centerY + currentHalfHeight)
            close()
        }
        canvas.drawPath(backgroundPath, trackPaint)

        // 3. Vẽ phần Progress (Phần đã kéo - phía bên trái thumb)
        val progressPath = Path().apply {
            moveTo(startX, centerY - minRadius)
            lineTo(thumbX, centerY - currentHalfHeight)
            lineTo(thumbX, centerY + currentHalfHeight)
            lineTo(startX, centerY + minRadius)
            close()
        }
        canvas.drawPath(progressPath, progressPaint)

        // 4. Vẽ các hình tròn ở 2 đầu để tạo độ mượt (Optional)
        canvas.drawCircle(startX, centerY, minRadius, progressPaint)
        canvas.drawCircle(endX, centerY, maxRadius, trackPaint)

        // 5. Vẽ Thumb
        val half = thumbSize / 2
        thumbDrawable.setBounds(
            (thumbX - half).toInt(),
            (centerY - half).toInt(),
            (thumbX + half).toInt(),
            (centerY + half).toInt()
        )
        thumbDrawable.draw(canvas)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val startX = minRadius
                val endX = width - maxRadius

                val usableWidth = endX - startX
                val touchX = event.x.coerceIn(startX, endX)
                val range = maxSize - minValue

                // Tính toán newProgress tạm thời dựa trên vị trí chạm
                var newProgress = (minValue + ((touchX - startX) / usableWidth) * range).toInt()

                // --- LOGIC GIỚI HẠN MỚI ---
                // Tính toán giới hạn tối thiểu (2/10 của maxSize)
                val minProgressLimit = (maxSize * 0.07f).toInt()

                // Áp dụng giới hạn: nếu newProgress nhỏ hơn giới hạn, đặt nó bằng giới hạn
                if (newProgress < minProgressLimit) {
                    newProgress = minProgressLimit
                }
                // --- KẾT THÚC LOGIC GIỚI HẠN MỚI ---

                // Đảm bảo progress không vượt quá minValue và maxSize (mặc dù đã xử lý giới hạn trên)
                newProgress = newProgress.coerceIn(minValue, maxSize)

                if (newProgress != progress) {
                    progress = newProgress
                    onSizeChanged?.invoke(progress)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setProgress(value: Int) {
        progress = value.coerceIn(minValue, maxSize)
        invalidate()
    }

    fun setMinValue(value: Int) {
        minValue = value
        progress = progress.coerceIn(minValue, maxSize)
        invalidate()
    }

    fun getProgress(): Int = progress

}
