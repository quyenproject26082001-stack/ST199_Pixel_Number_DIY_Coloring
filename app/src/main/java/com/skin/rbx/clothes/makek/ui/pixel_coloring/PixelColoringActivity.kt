package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.ActivityPixelColoringBinding
import com.skin.rbx.clothes.makek.databinding.DialogPixelCompleteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PixelColoringActivity : BaseActivity<ActivityPixelColoringBinding>(), PixelCanvasView.Listener {
    private val repository by lazy { PixelRepository(this) }
    private val progressStore by lazy { PixelProgressStore(this) }
    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { saveProgress() }
    private lateinit var paletteAdapter: PixelPaletteAdapter
    private var currentLevel: PixelLevel? = null
    private var selectedColorId = 1
    private var lastColorPercents = IntArray(0)
    private var completionDialog: Dialog? = null

    override fun setViewBinding() = ActivityPixelColoringBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        paletteAdapter = PixelPaletteAdapter { colorId ->
            selectedColorId = colorId
            binding.pixelCanvas.setSelectedColor(colorId)
            updatePalette(lastColorPercents)
        }
        binding.paletteList.adapter = paletteAdapter
        binding.pixelCanvas.listener = this
        binding.pixelCanvas.miniMapView = binding.miniMap
        loadLevel(intent.getStringExtra(EXTRA_LEVEL_ID).orEmpty(), intent.getStringExtra(EXTRA_CUSTOM_LEVEL))
    }

    override fun viewListener() = with(binding) {
        actionBar.btnActionBarLeft.tap { closeEditor() }
        btnWand.tap {
            val tool = if (pixelCanvas.getTool() == PixelTool.WAND) PixelTool.NONE else PixelTool.WAND
            pixelCanvas.setTool(tool)
            updateToolSelection()
        }
        btnBomb.tap {
            val tool = if (pixelCanvas.getTool() == PixelTool.BOMB) PixelTool.NONE else PixelTool.BOMB
            pixelCanvas.setTool(tool)
            updateToolSelection()
        }
        btnHint.tap { pixelCanvas.showHint() }
        btnResetZoom.tap { pixelCanvas.centerImage() }
    }

    override fun initActionBar() = with(binding.actionBar) {
        setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        setTextActionBar(tvCenter, getString(R.string.pixel_coloring))
    }

    private fun loadLevel(id: String, customJson: String? = null) {
        saveHandler.removeCallbacks(saveRunnable)
        lifecycleScope.launch {
            val level = withContext(Dispatchers.IO) {
                if (!customJson.isNullOrBlank()) repository.levelFromJson(customJson) else repository.loadLevel(id)
            }
            currentLevel = level
            selectedColorId = 1
            lastColorPercents = IntArray(level.palette.size)
            val saved = withContext(Dispatchers.IO) { progressStore.load(level) }
            binding.pixelCanvas.setLevel(level, saved)
            binding.actionBar.tvCenter.text = level.id.replace('_', ' ')
            updateToolSelection()
        }
    }

    override fun onProgress(percent: Int, colorPercents: IntArray) {
        lastColorPercents = colorPercents
        binding.progressBar.progress = percent
        binding.tvProgress.text = getString(R.string.pixel_percent, percent)
        updatePalette(colorPercents)
        saveHandler.removeCallbacks(saveRunnable)
        saveHandler.postDelayed(saveRunnable, 1000)
    }

    override fun onToolConsumed() = updateToolSelection()

    override fun onCompleted() {
        val level = currentLevel ?: return
        saveProgress()
        progressStore.setCompleted(level.id)
        showCompletionDialog(level)
    }

    private fun updatePalette(percents: IntArray) {
        val level = currentLevel ?: return
        val items = level.palette.mapIndexed { index, value ->
            PixelPaletteItem(
                colorId = index + 1,
                color = runCatching { Color.parseColor(value) }.getOrDefault(Color.WHITE),
                percent = percents.getOrElse(index) { 0 },
            )
        }
        paletteAdapter.submit(items, selectedColorId)
    }

    private fun updateToolSelection() = with(binding) {
        btnWand.isSelected = pixelCanvas.getTool() == PixelTool.WAND
        btnBomb.isSelected = pixelCanvas.getTool() == PixelTool.BOMB
    }

    private fun saveProgress() {
        val level = currentLevel ?: return
        progressStore.save(level.id, binding.pixelCanvas.getPaintedSnapshot())
    }

    private fun showCompletionDialog(level: PixelLevel) {
        if (completionDialog?.isShowing == true) return
        val dialogBinding = DialogPixelCompleteBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setGravity(Gravity.CENTER)
        }
        dialogBinding.resultPreview.setLevel(level, showCompleted = true)
        dialogBinding.btnClose.tap {
            dialog.dismiss()
            closeEditor()
        }
        dialogBinding.btnNext.tap {
            dialog.dismiss()
            openNextLevel(level)
        }
        dialog.setOnDismissListener { completionDialog = null }
        completionDialog = dialog
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun openNextLevel(level: PixelLevel) {
        if (level.category == "custom") {
            closeEditor()
            return
        }
        lifecycleScope.launch {
            val catalog = withContext(Dispatchers.IO) { repository.loadCatalog() }
            val index = catalog.indexOfFirst { it.id == level.id }
            val next = catalog.getOrNull(index + 1)
            if (next == null) closeEditor() else loadLevel(next.id)
        }
    }

    private fun closeEditor() {
        saveProgress()
        handleBackLeftToRight()
    }

    override fun onPause() {
        saveHandler.removeCallbacks(saveRunnable)
        saveProgress()
        super.onPause()
    }

    override fun onDestroy() {
        completionDialog?.dismiss()
        completionDialog = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_LEVEL_ID = "pixel_level_id"
        const val EXTRA_CUSTOM_LEVEL = "pixel_custom_level"
    }
}
