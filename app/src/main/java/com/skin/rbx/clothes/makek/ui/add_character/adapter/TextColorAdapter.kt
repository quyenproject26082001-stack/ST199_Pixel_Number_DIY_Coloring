package com.skin.rbx.clothes.makek.ui.add_character.adapter
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.util.Log
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.databinding.ItemTextColorBinding

class TextColorAdapter : BaseAdapter<SelectedModel, ItemTextColorBinding>(ItemTextColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onTextColorClick: ((Int, Int) -> Unit) = { _, _ -> }

    private var currentSelected = 1


    override fun onBind(binding: ItemTextColorBinding, item: SelectedModel, position: Int) {
        Log.d("TextColorAdapter", "onBind position=$position, color=${String.format("#%06X", 0xFFFFFF and item.value)}, isSelected=${item.isSelected}")

        binding.apply {
            colorClip.isSelected = item.isSelected

            if (position == 0) {
                Log.d("TextColorAdapter", "Position 0: Clearing and loading img0text_color")
                imvColor.visible()

                // Set margin to 0dp for position 0 to make it bigger
                val layoutParams = imvColor.layoutParams as android.widget.FrameLayout.LayoutParams
                layoutParams.setMargins(0, 0, 0, 0)
                imvColor.layoutParams = layoutParams

                // First clear any existing background drawable
                imvColor.background = null
                // Set background to transparent
                imvColor.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Also clear the parent FrameLayout's background
                val parentFrame = imvColor.parent as? android.widget.FrameLayout
                parentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                Log.d("TextColorAdapter", "Position 0: Background set to TRANSPARENT (ImageView and parent), about to set image resource")
                // Now set the image resource
                imvColor.setImageResource(R.drawable.img0text_color)
                btnAddColor.visible()
                root.tap { onChooseColorClick.invoke() }
            } else {
                Log.d("TextColorAdapter", "Position $position: Setting color background")
                imvColor.visible()

                // Set margin to 2dp for other positions (keep normal size)
                val layoutParams = imvColor.layoutParams as android.widget.FrameLayout.LayoutParams
                val margin = UnitHelper.dpToPxInt(imvColor.context.resources, 1f)
                layoutParams.setMargins(margin, margin, margin, margin)
                imvColor.layoutParams = layoutParams

                imvColor.setImageResource(0) // Clear image resource
                btnAddColor.gone()

                // Ensure parent FrameLayout background is transparent
                val parentFrame = imvColor.parent as? android.widget.FrameLayout
                parentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Create circular drawable for color
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(item.value)
                }
                imvColor.background = drawable

                root.tap { onTextColorClick.invoke(item.value, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        Log.d("TextColorAdapter", "submitItem called with position=$position")
        Log.d("TextColorAdapter", "Item at position 0: color=${String.format("#%06X", 0xFFFFFF and list[0].value)}, isSelected=${list[0].isSelected}")
        if (position == 0) {
            Log.d("TextColorAdapter", "WARNING: Position 0 was selected!")
        }

        items.clear()
        items.addAll(list)

        if (position != currentSelected) {
            Log.d("TextColorAdapter", "Notifying changes for positions $currentSelected and $position")
            notifyItemChanged(currentSelected)
            notifyItemChanged(position)
            currentSelected = position
        } else {
            Log.d("TextColorAdapter", "Notifying change for position $position")
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>){
        items.clear()
        items.addAll(list)
        currentSelected = 1
        notifyDataSetChanged()
    }
}
