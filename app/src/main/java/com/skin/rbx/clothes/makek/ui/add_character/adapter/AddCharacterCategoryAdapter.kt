package com.skin.rbx.clothes.makek.ui.add_character.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.data.model.AddCharacterCategoryModel
import java.util.zip.Inflater
import com.skin.rbx.clothes.makek.databinding.ItemCategoryBinding


class AddCharacterCategoryAdapter : RecyclerView.Adapter<AddCharacterCategoryAdapter.CategoryViewHolder>() {

    private val items = arrayListOf<AddCharacterCategoryModel>()

    var onCategoryClick: ((Int) -> Unit) = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder,position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<AddCharacterCategoryModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun submitItem(position: Int, list:
    List<AddCharacterCategoryModel>) {
        val oldSelected = items.indexOfFirst { it.isSelected }

        items.clear()
        items.addAll(list)

        if (oldSelected >= 0) notifyItemChanged(oldSelected)
        notifyItemChanged(position)
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AddCharacterCategoryModel, position: Int) {
            binding.apply {
                tvCategory.text = item.name
                tvCategory.isSelected = true
                tvCategory.setBackgroundResource(
                    if (item.isSelected) R.drawable.selected_tab
                    else R.drawable.un_selected_tab
                )

                root.tap {
                    onCategoryClick.invoke(position)
                }
            }
        }
    }
}
