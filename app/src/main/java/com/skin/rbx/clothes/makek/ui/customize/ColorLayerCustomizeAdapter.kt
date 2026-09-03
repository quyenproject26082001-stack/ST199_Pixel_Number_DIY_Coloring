package com.skin.rbx.clothes.makek.ui.customize
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.data.model.custom.ItemColorModel
import com.skin.rbx.clothes.makek.databinding.ItemColorBinding

class ColorLayerCustomizeAdapter(val context: Context) :
    BaseAdapter<ItemColorModel, ItemColorBinding>(ItemColorBinding::inflate) {
    var onItemClick: ((Int) -> Unit) = {}
    override fun onBind(binding: ItemColorBinding, item: ItemColorModel, position: Int) {
        binding.apply {
            imvImage.background = GradientDrawable().apply {
              //  shape = GradientDrawable.OVAL
                setColor(item.color.toColorInt())
            }
            btnImage.updateLayoutParams<FrameLayout.LayoutParams> {
                val margin = if (item.isSelected) {
                    UnitHelper.dpToPxInt(context.resources, 4f)
                } else {
                    0
                }
                setMargins(margin, margin, margin, margin)
            }
            imvFocus.setImageResource(if(item.isSelected) R.drawable.bg_focus_color else R.drawable.bg_focus_color_uslt)
            root.tap {
                val rv = root.parent as? RecyclerView ?: return@tap
                val currentPosition = rv.getChildAdapterPosition(root)
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClick.invoke(currentPosition)
                }
            }
        }
    }
}
