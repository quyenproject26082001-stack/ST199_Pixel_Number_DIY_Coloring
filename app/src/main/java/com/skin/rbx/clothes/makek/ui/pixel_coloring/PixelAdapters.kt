package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.databinding.ItemChoosePixelBinding
import com.skin.rbx.clothes.makek.databinding.ItemPixelColorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PixelLevelAdapter(
    private val repository: PixelRepository,
    private val progressStore: PixelProgressStore,
    private val scope: LifecycleCoroutineScope,
    private val onClick: (PixelLevelEntry) -> Unit,
) : ListAdapter<PixelLevelEntry, PixelLevelAdapter.Holder>(LEVEL_DIFF) {
    private val previewCache = object : LruCache<String, PixelPreviewState>(PREVIEW_CACHE_KB) {
        override fun sizeOf(key: String, value: PixelPreviewState): Int =
            (value.bitmap.byteCount / 1024).coerceAtLeast(1)
    }

    init {
        setHasStableIds(true)
    }

    fun refreshProgress(levelId: String? = null) {
        if (levelId == null) {
            previewCache.evictAll()
            if (itemCount > 0) notifyItemRangeChanged(0, itemCount)
            return
        }
        previewCache.remove(levelId)
        val position = currentList.indexOfFirst { it.id == levelId }
        if (position >= 0) notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemChoosePixelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    override fun getItemId(position: Int): Long = getItem(position).id.fold(1_125_899_906_842_597L) {
        hash, character -> hash * 31L + character.code
    }

    override fun onViewRecycled(holder: Holder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class Holder(private val binding: ItemChoosePixelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var loadJob: Job? = null

        fun bind(entry: PixelLevelEntry) {
            loadJob?.cancel()
            binding.root.setOnClickListener { onClick(entry) }
            previewCache.get(entry.id)?.let { state ->
                displayState(state)
                return
            }

            binding.imvPixel.clearLevel()
            showProgress(0, completed = false)
            loadJob = scope.launch {
                val state = withContext(Dispatchers.IO) {
                    runCatching {
                        val level = repository.loadLevel(entry.id)
                        val savedIndices = progressStore.load(level)
                        val completed = progressStore.isCompleted(entry.id)
                        val progress = when {
                            completed -> 100
                            level.totalPaintable == 0 -> 0
                            else -> savedIndices.size * 100 / level.totalPaintable
                        }
                        PixelPreviewState(
                            bitmap = renderPixelPreview(level, showCompleted = true),
                            progress = progress,
                            completed = completed || progress >= 100,
                        )
                    }
                }.getOrNull() ?: return@launch
                previewCache.put(entry.id, state)
                if (bindingAdapterPosition == RecyclerView.NO_POSITION ||
                    currentList.getOrNull(bindingAdapterPosition)?.id != entry.id
                ) return@launch
                displayState(state)
            }
        }

        private fun displayState(state: PixelPreviewState) {
            binding.imvPixel.setPreviewBitmap(state.bitmap)
            showProgress(state.progress, state.completed)
        }

        private fun showProgress(progress: Int, completed: Boolean) = with(binding) {
            val normalizedProgress = progress.coerceIn(0, 100)
            val hasProgress = completed || normalizedProgress > 0
            ovlPixel.isVisible = hasProgress
            icDone.isVisible = completed
            ctnProgress.isVisible = hasProgress && !completed
            icFull.tag = normalizedProgress
            icFull.doOnLayout { view ->
                val currentProgress = (view.tag as? Int)?.coerceIn(0, 100) ?: 0
                view.clipBounds = Rect(
                    0,
                    0,
                    view.width * currentProgress / 100,
                    view.height,
                )
            }
        }

        fun recycle() {
            loadJob?.cancel()
            binding.imvPixel.clearLevel()
            binding.root.setOnClickListener(null)
            showProgress(0, completed = false)
        }
    }

    private companion object {
        private const val PREVIEW_CACHE_KB = 8 * 1024

        val LEVEL_DIFF = object : DiffUtil.ItemCallback<PixelLevelEntry>() {
            override fun areItemsTheSame(oldItem: PixelLevelEntry, newItem: PixelLevelEntry): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PixelLevelEntry, newItem: PixelLevelEntry): Boolean =
                oldItem == newItem
        }
    }
}

private data class PixelPreviewState(
    val bitmap: Bitmap,
    val progress: Int,
    val completed: Boolean,
)

class PixelPaletteAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<PixelPaletteAdapter.Holder>() {
    private var items = emptyList<PixelPaletteItem>()
    private var selectedColorId = 1

    fun submit(items: List<PixelPaletteItem>, selectedColorId: Int) {
        this.items = items
        this.selectedColorId = selectedColorId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemPixelColorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(private val binding: ItemPixelColorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PixelPaletteItem) = with(binding) {
            colorCard.setCardBackgroundColor(item.color)
            root.alpha = if (item.percent >= 100) 0.3f else 1f
            progressRing.progress = item.percent
            val selected = item.colorId == selectedColorId
            icDone2.isVisible = selected
            tvColorNumber.isVisible = !selected
            val textColor =
                if (ColorUtils.calculateLuminance(item.color) > 0.6) Color.BLACK else Color.WHITE
            tvColorNumber.setTextColor(textColor)
            icDone2.backgroundTintList = ColorStateList.valueOf(textColor)
            tvColorNumber.text = item.colorId.toString()
            root.setOnClickListener { onClick(item.colorId) }
        }
    }
}
