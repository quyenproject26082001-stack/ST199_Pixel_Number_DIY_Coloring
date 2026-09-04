package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.helper.SoundHelper
import java.util.ArrayDeque
import kotlin.math.floor
import kotlin.math.min

class PixelCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onProgress(percent: Int, colorPercents: IntArray)
        fun onToolConsumed()
        fun onCompleted()
    }

    var listener: Listener? = null
    var miniMapView: PixelMiniMapView? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0x994A1E1D.toInt()
    }
    private val handler = Handler(Looper.getMainLooper())
    private var level: PixelLevel? = null
    private var colors = IntArray(0)
    private var painted = BooleanArray(0)
    private var paintedCount = 0
    private var completedNotified = false
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var transforming = false
    private var blockPaintUntilUp = false
    private var hintIndex = -1
    private var selectedColorId = 1
    private var selectedTool = PixelTool.NONE

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScale = scale
            scale = (scale * detector.scaleFactor).coerceIn(2f, 150f)
            val ratio = scale / oldScale
            offsetX = detector.focusX - (detector.focusX - offsetX) * ratio
            offsetY = detector.focusY - (detector.focusY - offsetY) * ratio
            invalidateWithMiniMap()
            return true
        }
    })

    fun setLevel(level: PixelLevel, savedIndices: Set<Int>) {
        this.level = level
        colors = IntArray(level.palette.size) { index -> parseColor(level.palette[index]) }
        painted = BooleanArray(level.width * level.height)
        savedIndices.forEach { index -> if (index in painted.indices) painted[index] = true }
        paintedCount = painted.count { it }
        selectedColorId = 1
        selectedTool = PixelTool.NONE
        completedNotified = paintedCount >= level.totalPaintable
        hintIndex = -1
        post {
            centerImage()
            notifyProgress()
        }
    }

    fun setSelectedColor(colorId: Int) {
        selectedColorId = colorId
        invalidate()
    }

    fun setTool(tool: PixelTool) {
        selectedTool = tool
    }

    fun getTool(): PixelTool = selectedTool
    fun getPaintedSnapshot(): BooleanArray = painted.copyOf()

    fun createCompletedBitmap(targetSize: Int = COMPLETED_BITMAP_SIZE): Bitmap? {
        val data = level ?: return null
        if (data.width <= 0 || data.height <= 0) return null

        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = min(targetSize.toFloat() / data.width, targetSize.toFloat() / data.height)
        val left = (targetSize - data.width * scale) / 2f
        val top = (targetSize - data.height * scale) / 2f
        val bitmapPaint = Paint().apply { isAntiAlias = false }

        for (y in 0 until data.height) {
            for (x in 0 until data.width) {
                val colorId = data.grid[y][x]
                if (colorId == 0) continue
                bitmapPaint.color = colors.getOrElse(colorId - 1) { Color.WHITE }
                canvas.drawRect(
                    left + x * scale,
                    top + y * scale,
                    left + (x + 1) * scale,
                    top + (y + 1) * scale,
                    bitmapPaint,
                )
            }
        }
        return bitmap
    }

    fun centerImage() {
        val data = level ?: return
        if (width == 0 || height == 0) return
        val fit = min((width - 40f) / data.width, (height - 40f) / data.height)
        scale = fit.coerceAtLeast(2f)
        offsetX = (width - data.width * scale) / 2f
        offsetY = (height - data.height * scale) / 2f
        invalidateWithMiniMap()
    }

    fun showHint(): Boolean {
        val data = level ?: return false
        val target = painted.indices.firstOrNull { index ->
            !painted[index] && data.grid[index / data.width][index % data.width] == selectedColorId
        } ?: return false
        hintIndex = target
        scale = 40f
        val x = target % data.width
        val y = target / data.width
        offsetX = width / 2f - (x + 0.5f) * scale
        offsetY = height / 2f - (y + 0.5f) * scale
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        handler.removeCallbacksAndMessages(HINT_TOKEN)
        handler.postAtTime({ hintIndex = -1; invalidate() }, HINT_TOKEN, SystemClock.uptimeMillis() + 3000)
        invalidateWithMiniMap()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldw == 0 || oldh == 0) centerImage()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = level ?: return
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        val showNumbers = scale > 15f
        gridPaint.strokeWidth = 1f / scale
        gridPaint.color = 0x334A1E1D
        textPaint.textSize = 0.42f
        for (y in 0 until data.height) {
            for (x in 0 until data.width) {
                val colorId = data.grid[y][x]
                if (colorId == 0) continue
                val index = y * data.width + x
                if (painted[index]) {
                    fillPaint.color = colors.getOrElse(colorId - 1) { Color.WHITE }
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, fillPaint)
                } else {
                    fillPaint.color = if (colorId == selectedColorId) 0x224A1E1D else 0x084A1E1D
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, fillPaint)
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, gridPaint)
                    if (showNumbers) {
                        val baseline = y + 0.5f - (textPaint.ascent() + textPaint.descent()) / 2f
                        canvas.drawText(colorId.toString(), x + 0.5f, baseline, textPaint)
                    }
                }
                if (index == hintIndex) {
                    gridPaint.color = 0xFF4A1E1D.toInt()
                    gridPaint.strokeWidth = 3f / scale
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, gridPaint)
                    gridPaint.color = 0x334A1E1D
                    gridPaint.strokeWidth = 1f / scale
                }
            }
        }
        canvas.restore()
        updateMiniMap()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                transforming = false
                blockPaintUntilUp = false
                paintAt(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                transforming = true
                blockPaintUntilUp = true
                lastFocusX = focusX(event)
                lastFocusY = focusY(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val focusX = focusX(event)
                    val focusY = focusY(event)
                    offsetX += focusX - lastFocusX
                    offsetY += focusY - lastFocusY
                    lastFocusX = focusX
                    lastFocusY = focusY
                    invalidateWithMiniMap()
                } else if (!transforming && !blockPaintUntilUp && !scaleDetector.isInProgress) {
                    paintAt(event.x, event.y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount - 1 < 2) transforming = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                transforming = false
                blockPaintUntilUp = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun paintAt(screenX: Float, screenY: Float) {
        val data = level ?: return
        val x = floor((screenX - offsetX) / scale).toInt()
        val y = floor((screenY - offsetY) / scale).toInt()
        if (x !in 0 until data.width || y !in 0 until data.height) return
        val changed = when (selectedTool) {
            PixelTool.WAND -> floodFill(x, y)
            PixelTool.BOMB -> bomb(x, y)
            PixelTool.NONE -> paintSingle(x, y)
        }
        if (selectedTool != PixelTool.NONE) {
            selectedTool = PixelTool.NONE
            listener?.onToolConsumed()
        }
        if (changed) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            SoundHelper.playSound(R.raw.touch)
            notifyProgress()
            invalidateWithMiniMap()
            checkCompleted()
        }
    }

    private fun paintSingle(x: Int, y: Int): Boolean {
        val data = level ?: return false
        val index = y * data.width + x
        if (data.grid[y][x] != selectedColorId || painted[index]) return false
        painted[index] = true
        paintedCount++
        return true
    }

    private fun floodFill(startX: Int, startY: Int): Boolean {
        val data = level ?: return false
        val targetColor = data.grid[startY][startX]
        if (targetColor == 0) return false
        var changed = false
        val stack = ArrayDeque<Int>()
        stack.add(startY * data.width + startX)
        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            if (index !in painted.indices || painted[index]) continue
            val x = index % data.width
            val y = index / data.width
            if (data.grid[y][x] != targetColor) continue
            painted[index] = true
            paintedCount++
            changed = true
            if (x > 0) stack.add(index - 1)
            if (x + 1 < data.width) stack.add(index + 1)
            if (y > 0) stack.add(index - data.width)
            if (y + 1 < data.height) stack.add(index + data.width)
        }
        return changed
    }

    private fun bomb(centerX: Int, centerY: Int): Boolean {
        val data = level ?: return false
        var changed = false
        for (y in centerY - 3..centerY + 3) {
            for (x in centerX - 3..centerX + 3) {
                if (x !in 0 until data.width || y !in 0 until data.height) continue
                val index = y * data.width + x
                if (data.grid[y][x] != 0 && !painted[index]) {
                    painted[index] = true
                    paintedCount++
                    changed = true
                }
            }
        }
        return changed
    }

    private fun notifyProgress() {
        val data = level ?: return
        val totals = IntArray(data.palette.size)
        val done = IntArray(data.palette.size)
        for (index in painted.indices) {
            val colorId = data.grid[index / data.width][index % data.width]
            if (colorId == 0 || colorId > totals.size) continue
            totals[colorId - 1]++
            if (painted[index]) done[colorId - 1]++
        }
        val colorPercents = IntArray(totals.size) { index ->
            if (totals[index] == 0) 100 else done[index] * 100 / totals[index]
        }
        val percent = if (data.totalPaintable == 0) 100 else paintedCount * 100 / data.totalPaintable
        listener?.onProgress(percent, colorPercents)
    }

    private fun checkCompleted() {
        val data = level ?: return
        if (!completedNotified && paintedCount >= data.totalPaintable) {
            completedNotified = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            listener?.onCompleted()
        }
    }

    private fun invalidateWithMiniMap() {
        invalidate()
        updateMiniMap()
    }

    private fun updateMiniMap() {
        val data = level ?: return
        miniMapView?.update(data, painted, scale, offsetX, offsetY, width, height)
    }

    private fun focusX(event: MotionEvent): Float = (0 until event.pointerCount).map { event.getX(it) }.average().toFloat()
    private fun focusY(event: MotionEvent): Float = (0 until event.pointerCount).map { event.getY(it) }.average().toFloat()
    private fun parseColor(value: String): Int = runCatching { Color.parseColor(value) }.getOrDefault(Color.WHITE)

    companion object {
        private const val COMPLETED_BITMAP_SIZE = 512
        private val HINT_TOKEN = Any()
    }
}
