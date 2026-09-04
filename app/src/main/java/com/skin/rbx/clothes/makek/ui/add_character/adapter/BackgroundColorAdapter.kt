package com.skin.rbx.clothes.makek.ui.add_character.adapter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import com.bumptech.glide.Glide
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
            cardCtn.isSelected = item.isSelected

            if (position == ADD_COLOR_POSITION) {
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
