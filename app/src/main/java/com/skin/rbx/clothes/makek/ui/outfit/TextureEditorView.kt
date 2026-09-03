package com.skin.rbx.clothes.makek.ui.outfit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class TextureEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var baseBitmap: Bitmap? = null
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private val displayMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val path = Path()
    private val history = ArrayDeque<Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }
    private var lastX = 0f
    private var lastY = 0f

    var erasing: Boolean = false
        set(value) {
            field = value
            paint.xfermode = if (value) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        }

    fun setBaseBitmap(bitmap: Bitmap) {
        baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(overlayBitmap!!)
        history.clear()
        updateMatrix(width, height)
        invalidate()
    }

    fun setBrushColor(color: Int) {
        erasing = false
        paint.color = color
    }

    fun setBrushSize(size: Float) {
        paint.strokeWidth = size
    }

    fun addBitmap(bitmap: Bitmap) {
        val canvas = overlayCanvas ?: return
        saveHistory()
        val targetWidth = (canvas.width * 0.32f).coerceAtLeast(1f)
        val scale = targetWidth / bitmap.width
        val targetHeight = bitmap.height * scale
        val left = (canvas.width - targetWidth) / 2f
        val top = (canvas.height - targetHeight) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + targetWidth, top + targetHeight), null)
        invalidate()
    }

    fun addText(text: String, color: Int = paint.color) {
        val canvas = overlayCanvas ?: return
        if (text.isBlank()) return
        saveHistory()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            textSize = canvas.width * 0.12f
            style = Paint.Style.FILL
        }
        canvas.drawText(text, canvas.width / 2f, canvas.height / 2f, textPaint)
        invalidate()
    }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        overlayBitmap?.recycle()
        overlayBitmap = previous
        overlayCanvas = Canvas(previous)
        invalidate()
    }

    fun clearEdits() {
        val canvas = overlayCanvas ?: return
        saveHistory()
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun exportBitmap(): Bitmap? {
        val base = baseBitmap ?: return null
        return Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888).also { output ->
            Canvas(output).apply {
                drawBitmap(base, 0f, 0f, null)
                overlayBitmap?.let { drawBitmap(it, 0f, 0f, null) }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateMatrix(w, h)
    }

    private fun updateMatrix(w: Int, h: Int) {
        val bitmap = baseBitmap ?: return
        if (w == 0 || h == 0) return
        val scale = min(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
        val dx = (w - bitmap.width * scale) / 2f
        val dy = (h - bitmap.height * scale) / 2f
        displayMatrix.reset()
        displayMatrix.postScale(scale, scale)
        displayMatrix.postTranslate(dx, dy)
        displayMatrix.invert(inverseMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        baseBitmap?.let { canvas.drawBitmap(it, displayMatrix, null) }
        overlayBitmap?.let { canvas.drawBitmap(it, displayMatrix, null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (baseBitmap == null) return false
        val point = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(point)
        val x = point[0]
        val y = point[1]
        val bitmap = baseBitmap ?: return false
        if (x !in 0f..bitmap.width.toFloat() || y !in 0f..bitmap.height.toFloat()) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                saveHistory()
                path.reset()
                path.moveTo(x, y)
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                path.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f)
                overlayCanvas?.drawPath(path, paint)
                path.reset()
                path.moveTo(x, y)
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                overlayCanvas?.drawLine(lastX, lastY, x, y, paint)
                path.reset()
                invalidate()
            }
        }
        return true
    }

    private fun saveHistory() {
        val current = overlayBitmap ?: return
        if (history.size == MAX_HISTORY) history.removeFirst().recycle()
        history.addLast(current.copy(Bitmap.Config.ARGB_8888, true))
    }

    companion object {
        private const val MAX_HISTORY = 12
    }
}
