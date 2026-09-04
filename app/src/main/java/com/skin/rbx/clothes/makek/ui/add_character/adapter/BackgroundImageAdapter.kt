package com.skin.rbx.clothes.makek.ui.add_character.adapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.skin.rbx.clothes.makek.core.base.BaseAdapter
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.utils.DataLocal
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.databinding.ItemBackgroundImageBinding
import com.facebook.shimmer.ShimmerDrawable

class BackgroundImageAdapter :
    BaseAdapter<SelectedModel, ItemBackgroundImageBinding>(ItemBackgroundImageBinding::inflate) {
    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = { _, _ -> }
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedModel, position: Int) {
        binding.apply {
            tvAddImg.isSelected=true
            containerCard.isSelected = item.isSelected

            lnlAddItem.gone()
            btnNone.gone()
            imvImage.gone()

            when (position) {
                ADD_IMAGE_POSITION -> {
                    lnlAddItem.visible()
                    lnlAddItem.tap(800) { onAddImageClick.invoke() }
                }

                NONE_ITEM_POSITION -> {
                    btnNone.visible()
                    btnNone.tap { onBackgroundImageClick.invoke(item.path, position) }
                }

                else -> {
                    imvImage.visible()
                    val shimmerDrawable = ShimmerDrawable().apply {
                        setShimmer(DataLocal.shimmer)
                    }
                    Glide.with(root)
                        .load(item.path)
                        .placeholder(shimmerDrawable)
                        .error(shimmerDrawable)
                        .override(256, 256)
                        .encodeQuality(60)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imvImage)
                    imvImage.tap { onBackgroundImageClick.invoke(item.path, position) }
                }
            }
        }
    }

    override fun submitList(list: List<SelectedModel>) {
        currentSelected = list.indexOfFirst { it.isSelected }
        super.submitList(list)
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        if (position != currentSelected) {
            items.clear()
            items.addAll(list)

            if (currentSelected >= 0) notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }

    companion object {
        const val ADD_IMAGE_POSITION = 0
        const val NONE_ITEM_POSITION = 1
    }
}
