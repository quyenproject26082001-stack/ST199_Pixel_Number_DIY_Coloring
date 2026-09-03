package com.skin.rbx.clothes.makek.ui.add_character.adapter

import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.loadImageSticker
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.databinding.ItemSpeechBinding

class SpeechAdapter : BaseAdapter<SelectedModel, ItemSpeechBinding>(ItemSpeechBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemSpeechBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSpeech)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}
