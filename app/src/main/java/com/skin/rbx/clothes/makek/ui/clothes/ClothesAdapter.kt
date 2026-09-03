package com.skin.rbx.clothes.makek.ui.clothes
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.graphics.Color
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.ItemClothesAccessoryBinding
import com.skin.rbx.clothes.makek.databinding.ItemClothesBinding

class ClothesAdapter :
    BaseAdapter<ClothesListItem, ItemClothesBinding>(ItemClothesBinding::inflate) {

    var onItemClick: (ClothesListItem) -> Unit = {}
    var useAccessoryStyle: Boolean = false

    override fun onBind(binding: ItemClothesBinding, item: ClothesListItem, position: Int) {
        binding.cardContainer.apply {
            cardElevation = UnitHelper.dpToPx(context.resources, 4f)
            strokeWidth = UnitHelper.dpToPxInt(context.resources, 1f)
            strokeColor = Color.BLACK
        }
        loadImage(binding.root.context, item.previewUrl, binding.imvImage)
        binding.root.tap { onItemClick(item) }
    }
}

class AccessoryClothesAdapter :
    BaseAdapter<ClothesListItem, ItemClothesAccessoryBinding>(ItemClothesAccessoryBinding::inflate) {

    var onItemClick: (ClothesListItem) -> Unit = {}
    var useAccessoryStyle: Boolean = false

    override fun onBind(binding: ItemClothesAccessoryBinding, item: ClothesListItem, position: Int) {
        binding.cardContainer.apply {
            cardElevation = UnitHelper.dpToPx(context.resources, 4f)
            strokeWidth = UnitHelper.dpToPxInt(context.resources, 1f)
            strokeColor = Color.BLACK
        }
        loadImage(binding.root.context, item.previewUrl, binding.imvImage)
        binding.root.tap { onItemClick(item) }
    }
}
