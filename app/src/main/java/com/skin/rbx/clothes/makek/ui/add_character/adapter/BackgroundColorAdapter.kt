package com.skin.rbx.clothes.makek.ui.add_character.adapter
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.databinding.ItemBackgroundColorBinding

class BackgroundColorAdapter :
    BaseAdapter<SelectedModel, ItemBackgroundColorBinding>(ItemBackgroundColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onBackgroundColorClick: ((Int, Int) -> Unit) = {_,_ ->}

    var currentSelected = -1
    override fun onBind(binding: ItemBackgroundColorBinding, item: SelectedModel, position: Int) {
        Log.d("BackgroundColorAdapter", "onBind position=$position, color=${String.format("#%06X", 0xFFFFFF and item.value)}, isSelected=${item.isSelected}, path=${item.path}")

        binding.apply {
            btnNone.gone()
            if (item.isSelected) {
                cardCtn.setStrokeColor(Color.parseColor("#000000"))
            } else {
                cardCtn.setStrokeColor(Color.TRANSPARENT)
            }
            // Set circular stroke for position 0, regular stroke for others


            if (position == ADD_COLOR_POSITION) {
                cardCtn.cardElevation = 0f
                cardCtn.setCardBackgroundColor(Color.TRANSPARENT)
                imvColor.visible()
                Log.d("BackgroundColorAdapter", "Position 0: Loading img with CircleCrop")
                Glide.with(root.context).clear(imvColor)
                imvColor.background = null
                Glide.with(root.context)
                    .load(R.drawable.img)
                   // .transform(CircleCrop())
                    .into(imvColor)
                root.tap { onChooseColorClick.invoke() }
            } else {
                cardCtn.cardElevation = UnitHelper.dpToPx(root.resources, 2f)
                imvColor.visible()
                Log.d("BackgroundColorAdapter", "Position $position: Setting color background")
                Glide.with(root.context).clear(imvColor)
                imvColor.setImageDrawable(null)
                imvColor.background = GradientDrawable().apply {
                   // shape = GradientDrawable.OVAL
                    setColor(item.value)
                }
                root.tap { onBackgroundColorClick.invoke(item.value, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>){
        Log.d("BackgroundColorAdapter", "submitItem called with position=$position")
        Log.d("BackgroundColorAdapter", "Item at position 0: color=${String.format("#%06X", 0xFFFFFF and list[0].value)}, isSelected=${list[0].isSelected}")
        if (position == 0) {
            Log.d("BackgroundColorAdapter", "WARNING: Position 0 was selected!")
        }

        items.clear()
        items.addAll(list)

        if (position != currentSelected){
            Log.d("BackgroundColorAdapter", "Notifying changes for positions $currentSelected and $position")
            if (currentSelected >= 0) notifyItemChanged(currentSelected)
            notifyItemChanged(position)
            currentSelected = position
        } else {
            Log.d("BackgroundColorAdapter", "Notifying change for position $position")
            notifyItemChanged(position)
        }
    }

    override fun submitList(list: List<SelectedModel>) {
        currentSelected = list.indexOfFirst { it.isSelected }
        super.submitList(list)
    }

    companion object {
        const val ADD_COLOR_POSITION = 0
    }
}
