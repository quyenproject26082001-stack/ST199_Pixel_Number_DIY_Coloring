package com.skin.rbx.clothes.makek.ui.outfit
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.raed.rasmview.brushtool.data.Brush
import com.raed.rasmview.brushtool.data.BrushesRepository
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.dp
import com.skin.rbx.clothes.makek.core.helper.BitmapHelper
import com.skin.rbx.clothes.makek.data.model.draw.Draw
import com.skin.rbx.clothes.makek.data.model.draw.DrawableDraw
import com.skin.rbx.clothes.makek.databinding.ActivityTextureEditorBinding
import com.skin.rbx.clothes.makek.databinding.C253ItemStickerBinding
import com.skin.rbx.clothes.makek.databinding.ItemTextureBrushColorBinding
import com.skin.rbx.clothes.makek.dialog.ChooseColorDialog
import com.skin.rbx.clothes.makek.dialog.TextDialog
import com.skin.rbx.clothes.makek.listener.listenerdraw.OnDrawListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class TextureEditorActivity : BaseActivity<ActivityTextureEditorBinding>() {

    private val type by lazy { intent.getStringExtra(EXTRA_TYPE).orEmpty() }
    private val sourceUrl by lazy { intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty() }
    private val option by lazy { intent.getIntExtra(EXTRA_OPTION, OPTION_BRUSH) }
    private val darkTheme by lazy { intent.getBooleanExtra(EXTRA_DARK_THEME, false) }
    private var brushSize = .3f
    private var brushSizePx = DEFAULT_BRUSH_SIZE_PX
    private var brushColor = Color.BLACK
    private var isEraserSelected = false
    private var overlayCount = 0
    private val itemAdapter = ItemAdapter(::onAssetClicked)
    private val brushColors by lazy {
        mutableListOf<Int?>(
            null,
            ContextCompat.getColor(this, R.color.black),
            ContextCompat.getColor(this, R.color.white),
            ContextCompat.getColor(this, R.color.color_19),
            ContextCompat.getColor(this, R.color.color_2),
            ContextCompat.getColor(this, R.color.color_3),
            ContextCompat.getColor(this, R.color.color_4),
            ContextCompat.getColor(this, R.color.color_5),
            ContextCompat.getColor(this, R.color.color_6),
            ContextCompat.getColor(this, R.color.color_7),
            ContextCompat.getColor(this, R.color.color_8),
        )
    }
    private val colorAdapter by lazy { BrushColorAdapter(brushColors, ::selectBrushColor) }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch { loadBitmap(uri.toString())?.let { addDrawable(it) } }
    }

    override fun setViewBinding() = ActivityTextureEditorBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        binding.root.setBackgroundResource(if (darkTheme) R.drawable.bg_app1 else R.drawable.bg_app)
        binding.drawView.setConstrained(true).setLocked(false).setOnDrawListener(drawListener)
        binding.rcvList.adapter = itemAdapter
        Glide.with(this).load(OutfitUrls.glideSource(sourceUrl, filesDir)).into(binding.imvClothes)
        setupOption()
    }

    override fun viewListener() {
        binding.btnDone.setOnClickListener { checkInternet {  saveTexture() }}
    }

    override fun initActionBar() = Unit

    private fun setupOption() {
        when (option) {
            OPTION_IMAGE -> setupList("image")
            OPTION_STICKER -> setupList("sticker")
            OPTION_EMOJI -> setupList("emoji")
            OPTION_TEXT -> setupText()
            else -> setupBrush()
        }
    }

    private fun setupList(folder: String) = with(binding) {
        setBrushControlsVisible(false)
        rasmView.visibility = View.GONE
        drawView.visibility = View.VISIBLE
        rcvList.visibility = View.VISIBLE
        val paths = assets.list(folder).orEmpty().sortedWith(compareBy({ it.substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }, { it }))
            .map { "file:///android_asset/$folder/$it" }
        itemAdapter.submit(paths, showAddImageLabel = folder == "image")
    }

    private fun setupBrush() = with(binding) {
        setBrushControlsVisible(true)
        rasmView.visibility = View.VISIBLE
        drawView.visibility = View.GONE
        rcvList.visibility = View.GONE
        rcvColor.adapter = colorAdapter
        rcvColor.layoutManager = LinearLayoutManager(this@TextureEditorActivity, RecyclerView.HORIZONTAL, false)
        rasmView.rasmContext.apply {
            setBackgroundColor(Color.TRANSPARENT)
            brushConfig = BrushesRepository(resources).get(Brush.Pen).also { it.size = brushSize }
            this.brushColor = this@TextureEditorActivity.brushColor
        }
        selectPen()
        setupSizeDrag()
        setBrushSizePx(brushSizePx)
        selectBrushColor(colorAdapter.selected)
        btnChooseBrush.setOnClickListener { selectPen() }
        btnEraser.setOnClickListener { selectEraser() }
    }

    private fun setupText() = with(binding) {
        setBrushControlsVisible(false)
        rasmView.visibility = View.GONE
        rcvList.visibility = View.GONE
        drawView.visibility = View.VISIBLE
        TextDialog(this@TextureEditorActivity).apply {
            onDoneClick = { bitmap, text -> bitmap?.let { addDrawable(it, text) } }
            onBackClick = { finish() }
            show()
        }
    }

    private fun setBrushControlsVisible(visible: Boolean) = with(binding) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        lnlOption.visibility = visibility
        brushControlsScroll.visibility = visibility
        tvSize.visibility = visibility
        sbDrag.visibility = visibility
        icThumb.visibility = visibility
        tvPx.visibility = visibility
        tvColor.visibility = visibility
        rcvColor.visibility = visibility
    }

    private fun selectPen() = with(binding) {
        isEraserSelected = false
        lnlOption.setBackgroundResource(R.drawable.ic_pen_slt)
        tvColor.visibility = View.VISIBLE
        rcvColor.visibility = View.VISIBLE
        rasmView.rasmContext.brushConfig = BrushesRepository(resources).get(Brush.Pen).also { it.size = brushSize }
        rasmView.rasmContext.brushColor = brushColor
    }

    private fun selectEraser() = with(binding) {
        isEraserSelected = true
        lnlOption.setBackgroundResource(R.drawable.ic_eraser_slt)
        tvColor.visibility = View.GONE
        rcvColor.visibility = View.GONE
        rasmView.rasmContext.brushConfig = BrushesRepository(resources).get(Brush.HardEraser).also { it.size = brushSize }
    }

    private fun setupSizeDrag() = with(binding) {
        val listener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    brushControlsScroll.requestDisallowInterceptTouchEvent(true)
                    updateBrushSizeFromTouch(event.rawX)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    brushControlsScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
        sbDrag.setOnTouchListener(listener)
        icThumb.setOnTouchListener(listener)
    }

    private fun updateBrushSizeFromTouch(rawX: Float) {
        with(binding) {
            if (sbDrag.width <= 0 || icThumb.width <= 0) return@with
            val location = IntArray(2)
            sbDrag.getLocationOnScreen(location)
            val left = location[0] + icThumb.width / 2f
            val right = location[0] + sbDrag.width - icThumb.width / 2f
            val fraction = ((rawX.coerceIn(left, right) - left) / (right - left)).coerceIn(0f, 1f)
            val px = MIN_BRUSH_SIZE_PX + ((MAX_BRUSH_SIZE_PX - MIN_BRUSH_SIZE_PX) * fraction).roundToInt()
            setBrushSizePx(px)
        }
    }

    private fun setBrushSizePx(px: Int) = with(binding) {
        brushSizePx = px.coerceIn(MIN_BRUSH_SIZE_PX, MAX_BRUSH_SIZE_PX)
        brushSize = brushSizePx / 100f
        tvPx.text = getString(R.string.texture_brush_px, brushSizePx)
        rasmView.rasmContext.brushConfig.size = brushSize
        sbDrag.post {
            val trackWidth = (sbDrag.width - icThumb.width).coerceAtLeast(0)
            val fraction = (brushSizePx - MIN_BRUSH_SIZE_PX).toFloat() / (MAX_BRUSH_SIZE_PX - MIN_BRUSH_SIZE_PX)
            val translation = (fraction - 0.5f) * trackWidth
            icThumb.translationX = translation
            tvPx.translationX = translation
        }
    }

    private fun selectBrushColor(position: Int) {
        if (position == 0) {
            ChooseColorDialog(this).apply {
                onDoneEvent = { color ->
                    brushColors[0] = color
                    applyBrushColor(0, color)
                    dismiss()
                }
                show()
            }
            return
        }
        applyBrushColor(position, brushColors[position] ?: Color.BLACK)
    }

    private fun applyBrushColor(position: Int, color: Int) {
        brushColor = color
        colorAdapter.selected = position
        if (!isEraserSelected) binding.rasmView.rasmContext.brushColor = color
    }

    private fun onAssetClicked(path: String, position: Int) {
        if (option == OPTION_IMAGE && position == 0) {
            imagePicker.launch("image/*")
            return
        }
        lifecycleScope.launch { loadBitmap(path)?.let { addDrawable(it) } }
    }

    private suspend fun loadBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (path.startsWith("content://")) {
                contentResolver.openInputStream(Uri.parse(path))!!.use { android.graphics.BitmapFactory.decodeStream(it) }
            } else {
                Glide.with(this@TextureEditorActivity).load(path).submit().get().toBitmap()
            }
        }.getOrNull()
    }

    private fun addDrawable(bitmap: Bitmap, textContent: String? = null) {
        overlayCount++
        val draw = DrawableDraw(
            BitmapDrawable(resources, bitmap),
            "${System.currentTimeMillis()}.png"
        ).apply {
            isText = option == OPTION_TEXT
            this.textContent = textContent
        }
        binding.drawView.addDraw(draw)
    }

    private fun saveTexture() {
        binding.drawView.hideSelect()
        if (option != OPTION_BRUSH && overlayCount == 0) {
            finish()
            return
        }
        lifecycleScope.launch {
            showLoading()
            val fileName = withContext(Dispatchers.IO) {
                val captured = BitmapHelper.createBimapFromView(binding.flExport)
                val output = Bitmap.createScaledBitmap(captured, 585, 559, true)
                val directory = File(filesDir, "outfit").apply { mkdirs() }
                val file = File(directory, "${type}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
                file.name
            }
            dismissLoading()
            setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT_URL, OutfitUrls.INTERNAL_PREFIX + fileName))
            finish()
        }
    }

    private val drawListener = object : OnDrawListener {
        override fun onAddedDraw(draw: Draw) = Unit
        override fun onClickedDraw(draw: Draw) = Unit
        override fun onDeletedDraw(draw: Draw) { overlayCount = (overlayCount - 1).coerceAtLeast(0) }
        override fun onDragFinishedDraw(draw: Draw) = Unit
        override fun onTouchedDownDraw(draw: Draw) = Unit
        override fun onZoomFinishedDraw(draw: Draw) = Unit
        override fun onFlippedDraw(draw: Draw) = Unit
        override fun onDoubleTappedDraw(draw: Draw) = Unit
        override fun onHideOptionIconDraw() = Unit
        override fun onUndoDeleteDraw(draw: List<Draw?>) = Unit
        override fun onUndoUpdateDraw(draw: List<Draw?>) = Unit
        override fun onUndoDeleteAll() = Unit
        override fun onRedoAll() = Unit
        override fun onReplaceDraw(draw: Draw) = Unit
        override fun onEditText(draw: DrawableDraw) = Unit
        override fun onReplace(draw: Draw) = Unit
    }

    private class ItemAdapter(private val onClick: (String, Int) -> Unit) : RecyclerView.Adapter<ItemAdapter.Holder>() {
        private var items = emptyList<String>()
        private var showAddImageLabel = false
        fun submit(value: List<String>, showAddImageLabel: Boolean) {
            items = value
            this.showAddImageLabel = showAddImageLabel
            notifyDataSetChanged()
        }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(C253ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) =
            holder.bind(items[position], position, showAddImageLabel && position == 0, onClick)
        class Holder(private val binding: C253ItemStickerBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(path: String, position: Int, isAddImage: Boolean, onClick: (String, Int) -> Unit) {
                Glide.with(binding.root).load(path).into(binding.imvSticker)
                binding.imvSticker.translationY = if (isAddImage) {
                    -UnitHelper.dpToPx(binding.root.resources, 2f)
                } else {
                    0f
                }
                binding.tvAddImg.isSelected = true
                binding.tvAddImg.visibility = if (isAddImage) View.VISIBLE else View.GONE
                binding.root.setOnClickListener { onClick(path, position) }
            }
        }
    }

    private class BrushColorAdapter(
        private val items: List<Int?>,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<BrushColorAdapter.Holder>() {
        var selected = 1
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            ItemTextureBrushColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) =
            holder.bind(items[position], position == 0, position == selected) { onClick(position) }

        class Holder(private val binding: ItemTextureBrushColorBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(color: Int?, isAddColor: Boolean, selected: Boolean, onClick: () -> Unit) = with(binding) {
                val inset = (if (selected) 4 else 0).dp(root.context)
                cardColor.strokeWidth = UnitHelper.dpToPxInt(
                    root.resources,
                    if (selected) 2f else 0.4f
                )
                cardColor.strokeColor = Color.BLACK
                cardColor.setCardBackgroundColor(Color.TRANSPARENT)
                (ctnCard.layoutParams as FrameLayout.LayoutParams).apply {
                    setMargins(inset, inset, inset, inset)
                    ctnCard.layoutParams = this
                }
                btnAddColor.visibility = if (isAddColor) View.VISIBLE else View.GONE
                ctnCard.setCardBackgroundColor(color ?: Color.WHITE)
                root.setOnClickListener { onClick() }
            }
        }
    }
    companion object {
        const val EXTRA_TYPE = "texture_type"
        const val EXTRA_SOURCE_URL = "texture_source_url"
        const val EXTRA_OPTION = "texture_option"
        const val EXTRA_DARK_THEME = "texture_dark_theme"
        const val EXTRA_RESULT_URL = "texture_result_url"
        private const val OPTION_IMAGE = 0
        private const val OPTION_BRUSH = 1
        private const val OPTION_STICKER = 2
        private const val OPTION_EMOJI = 3
        private const val OPTION_TEXT = 4
        private const val MIN_BRUSH_SIZE_PX = 2
        private const val DEFAULT_BRUSH_SIZE_PX = 30
        private const val MAX_BRUSH_SIZE_PX = 100
    }
}
