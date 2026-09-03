package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.databinding.ItemPixelColorBinding
import com.skin.rbx.clothes.makek.databinding.ItemPixelLevelBinding
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
        ItemPixelLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
    override fun onViewRecycled(holder: Holder) = holder.recycle()

    inner class Holder(private val binding: ItemPixelLevelBinding) : RecyclerView.ViewHolder(binding.root) {
        private var loadJob: Job? = null

        fun bind(entry: PixelLevelEntry) {
            loadJob?.cancel()
            binding.preview.clearLevel()
            val completed = progressStore.isCompleted(entry.id)
            binding.tvCompleted.visibility = if (completed) View.VISIBLE else View.GONE
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
                binding.preview.setLevel(result.first, painted, result.third)
            }
        }

        fun recycle() {
            loadJob?.cancel()
            binding.preview.clearLevel()
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
            colorCard.strokeWidth = if (item.colorId == selectedColorId) 4 else 0
            colorCard.alpha = if (item.percent == 100) 0.55f else 1f
            val textColor = if (ColorUtils.calculateLuminance(item.color) > 0.6) Color.BLACK else Color.WHITE
            tvColorNumber.setTextColor(textColor)
            tvColorProgress.setTextColor(textColor)
            tvColorNumber.text = item.colorId.toString()
            tvColorProgress.text = root.context.getString(R.string.pixel_percent, item.percent)
            root.setOnClickListener { onClick(item.colorId) }
        }
    }
}
