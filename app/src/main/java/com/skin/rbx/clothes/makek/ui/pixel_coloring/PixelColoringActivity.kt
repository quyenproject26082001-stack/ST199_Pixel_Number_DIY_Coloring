package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.startIntentRightToLeft
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.helper.LanguageHelper
import com.skin.rbx.clothes.makek.databinding.ActivityPixelColoringBinding
import com.skin.rbx.clothes.makek.dialog.YesNoDialog
import com.skin.rbx.clothes.makek.ui.add_character.AddCharacterActivity
import java.io.File
import java.io.FileOutputStream
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
    private var navigatingToAddCharacter = false

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
        actionBar.btnActionBarLeft.tap { confirmExit() }
        actionBar.btnActionBarRight.tap { navigateToAddCharacter() }
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
        //setImageActionBar(btnActionBarRight, R.drawable.ic_back)
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
        navigateToAddCharacter()
    }

    private fun navigateToAddCharacter() {
        if (navigatingToAddCharacter) return
        val level = currentLevel ?: return
        val bitmap = binding.pixelCanvas.createCompletedBitmap() ?: return
        navigatingToAddCharacter = true
        lifecycleScope.launch {
            val path = saveCompletedArtwork(bitmap, level.id)
            if (path == null) {
                navigatingToAddCharacter = false
                showToast(R.string.save_failed_please_try_again)
                return@launch
            }
            startIntentRightToLeft(AddCharacterActivity::class.java, path)
        }
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
        val selectedTool = pixelCanvas.getTool()
        btnWand.isSelected = selectedTool == PixelTool.WAND
        btnBomb.isSelected = selectedTool == PixelTool.BOMB
        btnWand.animateSelectedScale(btnWand.isSelected)
        btnBomb.animateSelectedScale(btnBomb.isSelected)
    }

    private fun View.animateSelectedScale(selected: Boolean) {
        val targetScale = if (selected) 1.2f else 1f
        animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(150L)
            .start()
    }

    private fun saveProgress() {
        val level = currentLevel ?: return
        progressStore.save(level.id, binding.pixelCanvas.getPaintedSnapshot())
    }

    private suspend fun saveCompletedArtwork(bitmap: Bitmap, levelId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val directory = File(filesDir, "pixel_coloring_completed").apply { mkdirs() }
                val file = File(directory, "$levelId.png")
                FileOutputStream(file).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                }
                file.absolutePath
            } catch (_: Exception) {
                null
            } finally {
                bitmap.recycle()
            }
        }

    private fun closeEditor() {
        saveProgress()
        handleBackLeftToRight()
    }

    private fun confirmExit() {
        val dialog = YesNoDialog(this, R.string.exit, R.string.do_you_want_to_exit)
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            closeEditor()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
    }

    override fun onBackPressed() {
        confirmExit()
    }

    override fun onPause() {
        saveHandler.removeCallbacks(saveRunnable)
        saveProgress()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        navigatingToAddCharacter = false
    }

    companion object {
        const val EXTRA_LEVEL_ID = "pixel_level_id"
        const val EXTRA_CUSTOM_LEVEL = "pixel_custom_level"
    }
}
