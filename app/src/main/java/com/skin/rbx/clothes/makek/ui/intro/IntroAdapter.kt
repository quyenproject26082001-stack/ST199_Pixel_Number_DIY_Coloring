package com.skin.rbx.clothes.makek.ui.intro

import android.content.Context
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.select
import com.skin.rbx.clothes.makek.core.extensions.strings
import com.skin.rbx.clothes.makek.data.model.IntroModel
import com.skin.rbx.clothes.makek.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}