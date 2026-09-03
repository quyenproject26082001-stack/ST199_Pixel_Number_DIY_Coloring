package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.ActivityPixelGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class PixelGalleryActivity : BaseActivity<ActivityPixelGalleryBinding>() {
    private val repository by lazy { PixelRepository(this) }
    private val progressStore by lazy { PixelProgressStore(this) }
    private lateinit var levelAdapter: PixelLevelAdapter
    private var catalog = emptyList<PixelLevelEntry>()
    private var selectedCategory = CATEGORY_ALL

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::createLevelFromImage)
    }

    override fun setViewBinding() = ActivityPixelGalleryBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        levelAdapter = PixelLevelAdapter(repository, progressStore, lifecycleScope, ::openLevel)
        binding.levelList.adapter = levelAdapter
        (binding.levelList.layoutManager as? GridLayoutManager)?.spanCount =
            if (resources.configuration.smallestScreenWidthDp >= 600) 5 else 3
        selectCategory(CATEGORY_ALL, binding.btnAll)
        loadCatalog()
    }

    override fun viewListener() = with(binding) {
        actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        btnUpload.tap { imagePicker.launch("image/*") }
        categoryButtons().forEach { (button, category) ->
            button.setOnClickListener { selectCategory(category, button) }
        }
    }

    override fun initActionBar() = with(binding.actionBar) {
        setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        setTextActionBar(tvCenter, getString(R.string.pixel_gallery))
    }

    override fun onResume() {
        super.onResume()
        if (::levelAdapter.isInitialized) levelAdapter.refreshProgress()
    }

    private fun loadCatalog() {
        lifecycleScope.launch {
            catalog = withContext(Dispatchers.IO) { repository.loadCatalog() }
            binding.loading.visibility = View.GONE
            binding.levelList.visibility = View.VISIBLE
            showSelectedCategory()
        }
    }

    private fun selectCategory(category: String, selectedButton: Button) {
        selectedCategory = category
        categoryButtons().forEach { (button, _) -> button.isSelected = button === selectedButton }
        showSelectedCategory()
    }

    private fun showSelectedCategory() {
        if (!::levelAdapter.isInitialized) return
        val filtered = if (selectedCategory == CATEGORY_ALL) catalog else {
            catalog.filter { it.category == selectedCategory }
        }
        levelAdapter.submitList(filtered)
        binding.levelList.scrollToPosition(0)
    }

    private fun categoryButtons(): List<Pair<Button, String>> = with(binding) {
        listOf(
            btnAll to CATEGORY_ALL,
            btnAnimal to "animal",
            btnFood to "food",
            btnHuman to "human",
            btnMonster to "monster",
            btnPlant to "plant",
            btnVehicle to "vehicle",
            btnChristmas to "christmas",
            btnHalloween to "halloween",
            btnRandom to "random",
        )
    }

    private fun openLevel(entry: PixelLevelEntry) {
        startActivity(Intent(this, PixelColoringActivity::class.java).putExtra(PixelColoringActivity.EXTRA_LEVEL_ID, entry.id))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun createLevelFromImage(uri: Uri) {
        binding.loading.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false
        lifecycleScope.launch {
            val level = withContext(Dispatchers.IO) {
                runCatching { quantizeBitmap(decodeForPixelArt(uri)) }.getOrNull()
            }
            binding.loading.visibility = View.GONE
            binding.btnUpload.isEnabled = true
            if (level == null) {
                showToast(R.string.pixel_image_error)
                return@launch
            }
            startActivity(Intent(this@PixelGalleryActivity, PixelColoringActivity::class.java).apply {
                putExtra(PixelColoringActivity.EXTRA_LEVEL_ID, level.id)
                putExtra(PixelColoringActivity.EXTRA_CUSTOM_LEVEL, repository.levelToJson(level))
            })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun decodeForPixelArt(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val size = info.size
                val ratio = TARGET_SIZE.toFloat() / max(size.width, size.height).coerceAtLeast(1)
                decoder.setTargetSize(
                    (size.width * ratio).roundToInt().coerceAtLeast(1),
                    (size.height * ratio).roundToInt().coerceAtLeast(1),
                )
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        val decoded = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Cannot decode image")
        val ratio = TARGET_SIZE.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).roundToInt().coerceAtLeast(1),
            (decoded.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > TARGET_SIZE * 2) sample *= 2
        return sample
    }

    private fun quantizeBitmap(bitmap: Bitmap): PixelLevel {
        val palette = mutableListOf<Int>()
        val grid = MutableList(bitmap.height) { y ->
            MutableList(bitmap.width) { x ->
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) < 128) return@MutableList 0
                val rgb = Color.rgb(
                    ((Color.red(pixel) / 4f).roundToInt() * 4).coerceAtMost(255),
                    ((Color.green(pixel) / 4f).roundToInt() * 4).coerceAtMost(255),
                    ((Color.blue(pixel) / 4f).roundToInt() * 4).coerceAtMost(255),
                )
                var index = palette.indexOf(rgb)
                if (index == -1) {
                    index = if (palette.size < MAX_COLORS) {
                        palette.add(rgb)
                        palette.lastIndex
                    } else {
                        closestColor(rgb, palette)
                    }
                }
                index + 1
            }
        }
        bitmap.recycle()
        return PixelLevel(
            id = "user_custom_${System.currentTimeMillis()}",
            category = "custom",
            width = grid.firstOrNull()?.size ?: 1,
            height = grid.size,
            palette = palette.map { String.format("#%06X", it and 0xFFFFFF) },
            grid = grid,
        )
    }

    private fun closestColor(color: Int, palette: List<Int>): Int {
        var bestIndex = 0
        var bestDistance = Long.MAX_VALUE
        palette.forEachIndexed { index, candidate ->
            val dr = Color.red(color) - Color.red(candidate)
            val dg = Color.green(color) - Color.green(candidate)
            val db = Color.blue(color) - Color.blue(candidate)
            val distance = (dr * dr + dg * dg + db * db).toLong()
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    companion object {
        private const val CATEGORY_ALL = "all"
        private const val TARGET_SIZE = 100
        private const val MAX_COLORS = 60
    }
}
