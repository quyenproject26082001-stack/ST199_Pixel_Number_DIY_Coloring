package com.skin.rbx.clothes.makek.ui.my_creation.adapter

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ItemPackageBinding
import com.skin.rbx.clothes.makek.ui.outfit.OutfitPackage
import com.skin.rbx.clothes.makek.ui.outfit.OutfitUrls
import java.io.File

class MyPackageAdapter : BaseAdapter<OutfitPackage, ItemPackageBinding>(ItemPackageBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    override fun areItemsTheSame(oldItem: OutfitPackage, newItem: OutfitPackage) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: OutfitPackage, newItem: OutfitPackage) = oldItem == newItem

    override fun onBind(binding: ItemPackageBinding, item: OutfitPackage, position: Int) {
        binding.apply {
            val firstEntry = item.entries.first()
            val source = OutfitUrls.glideSource(firstEntry.previewUrl, root.context.filesDir)
            val localFile = source as? File
            Glide.with(root.context)
                .load(source)
                .thumbnail(0.1f)
                .override(256, 256)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(ObjectKey(localFile?.lastModified() ?: firstEntry.previewUrl))
                .into(imvImage)

            btnDelete.gone()
            btnSelect.visibility = if (item.isShowSelection) android.view.View.VISIBLE else android.view.View.GONE
            frameCover.visibility = if (item.isSelected && item.isShowSelection) android.view.View.VISIBLE else android.view.View.GONE
            btnSelect.setImageResource(if (item.isSelected) R.drawable.ic_slt_item else R.drawable.ic_uslt_item)

            root.tap { onItemClick.invoke(item.id) }
            root.setOnLongClickListener {
                if (items.any { pkg -> pkg.isShowSelection }) return@setOnLongClickListener false
                val (rv, rvChild) = findRecyclerView(root) ?: return@setOnLongClickListener false
                val actualPos = rv.getChildAdapterPosition(rvChild)
                if (actualPos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                onLongClick.invoke(actualPos)
                true
            }
            btnSelect.tap {
                val (rv, rvChild) = findRecyclerView(root) ?: return@tap
                val actualPos = rv.getChildAdapterPosition(rvChild)
                if (actualPos != RecyclerView.NO_POSITION) onItemTick.invoke(actualPos)
            }
        }
    }

    override fun onViewRecycled(holder: BaseAdapter<OutfitPackage, ItemPackageBinding>.BaseViewHolder) {
        Glide.with(holder.binding.root).clear(holder.binding.imvImage)
        super.onViewRecycled(holder)
    }

    private fun findRecyclerView(view: android.view.View): Pair<RecyclerView, android.view.View>? {
        var child: android.view.View = view
        var parent = view.parent
        while (parent != null) {
            if (parent is RecyclerView) return Pair(parent, child)
            child = parent as? android.view.View ?: return null
            parent = child.parent
        }
        return null
    }
}
