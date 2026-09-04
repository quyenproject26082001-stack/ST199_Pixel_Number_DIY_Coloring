package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
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
) : RecyclerView.Adapter<PixelLevelAdapter.Holder>() {
    private val items = mutableListOf<PixelLevelEntry>()

    fun submitList(newItems: List<PixelLevelEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun refreshProgress() = notifyDataSetChanged()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemChoosePixelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
    override fun onViewRecycled(holder: Holder) = holder.recycle()

    inner class Holder(private val binding: ItemChoosePixelBinding) : RecyclerView.ViewHolder(binding.root) {
        private var loadJob: Job? = null

        fun bind(entry: PixelLevelEntry) {
            loadJob?.cancel()
            binding.imvPixel.clearLevel()
            val completed = progressStore.isCompleted(entry.id)
            showProgress(if (completed) 100 else 0, completed)
            binding.root.setOnClickListener { onClick(entry) }
            loadJob = scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val level = repository.loadLevel(entry.id)
                        val progress = progressStore.load(level)
                        Triple(level, progress, progressStore.isCompleted(entry.id))
                    }
                }.getOrNull() ?: return@launch
                if (bindingAdapterPosition == RecyclerView.NO_POSITION ||
                    items.getOrNull(bindingAdapterPosition)?.id != entry.id
                ) return@launch
                val painted = BooleanArray(result.first.width * result.first.height)
                result.second.forEach { index -> if (index in painted.indices) painted[index] = true }
                val progress = if (result.third) {
                    100
                } else if (result.first.totalPaintable == 0) {
                    0
                } else {
                    result.second.size * 100 / result.first.totalPaintable
                }
                val isFinished = result.third || progress >= 100
                binding.imvPixel.setLevel(result.first, painted, showCompleted = true)
                showProgress(progress, isFinished)
            }
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
            showProgress(0, completed = false)
        }
    }
}

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

    inner class Holder(private val binding: ItemPixelColorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PixelPaletteItem) = with(binding) {
            colorCard.setCardBackgroundColor(item.color)
            root.alpha = if (item.percent >= 100) 0.3f else 1f
            progressRing.progress = item.percent
            val selected = item.colorId == selectedColorId
            icDone2.isVisible = selected
            tvColorNumber.isVisible = !selected
            val textColor = if (ColorUtils.calculateLuminance(item.color) > 0.6) Color.BLACK else Color.WHITE
            tvColorNumber.setTextColor(textColor)
            tvColorNumber.text = item.colorId.toString()
            root.setOnClickListener { onClick(item.colorId) }
        }
    }
}
