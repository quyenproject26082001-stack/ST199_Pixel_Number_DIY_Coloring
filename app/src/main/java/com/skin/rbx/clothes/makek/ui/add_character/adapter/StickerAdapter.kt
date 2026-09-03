package com.skin.rbx.clothes.makek.ui.add_character.adapter

import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.loadImageSticker
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}