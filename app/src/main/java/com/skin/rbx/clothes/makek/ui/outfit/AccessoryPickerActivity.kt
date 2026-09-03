package com.skin.rbx.clothes.makek.ui.outfit

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ActivityAccessoryPickerBinding
import com.skin.rbx.clothes.makek.databinding.ItemAccessoryCategoryBinding
import com.skin.rbx.clothes.makek.databinding.ItemAccessoryChoiceBinding
import com.skin.rbx.clothes.makek.ui.clothes.ClothesCatalogResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AccessoryPickerActivity : BaseActivity<ActivityAccessoryPickerBinding>() {

    private val pickType by lazy { intent.getStringExtra(EXTRA_PICK_TYPE) ?: OutfitViewerActivity.TYPE_ACCESSORY }
    private val selectedAccessories = linkedMapOf<String, String>()
    private var selectedClothes = ""
    private var catalog = emptyMap<String, List<String>>()
    private var selectedCategory = ""
    private val categoryAdapter = CategoryAdapter(::selectCategory)
    private val accessoryAdapter = ChoiceAdapter(::selectAccessory)
    private val clothesAdapter = ClothesAdapter(::selectClothes)

    override fun setViewBinding() = ActivityAccessoryPickerBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        selectedClothes = intent.getStringExtra(EXTRA_SELECTED_URL).orEmpty()
        parseSelectedAccessories(intent.getStringExtra(EXTRA_SELECTED_JSON).orEmpty())
        binding.rcvClothes.adapter = clothesAdapter
        binding.rcvCategoryAccessory.adapter = categoryAdapter
        binding.rcvAccessory.adapter = accessoryAdapter
        lifecycleScope.launch {
            showLoading()
            val response = withContext(Dispatchers.IO) { loadCatalog() }
            dismissLoading()
            catalog = response.models
            if (pickType == OutfitViewerActivity.TYPE_ACCESSORY) setupAccessoryUi() else setupClothesUi(response)
        }
    }

    override fun viewListener() = with(binding.actionBar) {
        btnActionBarLeft.tap { handleBackLeftToRight() }
        btnActionBarRight.tap { returnSelection() }
    }

    override fun initActionBar() = with(binding.actionBar) {
        btnActionBarLeft.visible()
        btnActionBarRight.visible()
        setImageActionBar(btnActionBarLeft, R.drawable.c253_ic_back)
        setImageActionBar(btnActionBarRight, R.drawable.c253_ic_done)
    }

    private fun setupClothesUi(response: ClothesCatalogResponse) = with(binding) {
        rcvClothes.visibility = View.VISIBLE
        lnlAccessory.visibility = View.GONE
        val quantity = response.folders.firstOrNull { it.category == "special" }?.quantity ?: 0
        clothesAdapter.submit((1..quantity).map { "$SPECIAL_ROOT/special/$it.png" }, selectedClothes)
    }

    private fun setupAccessoryUi() = with(binding) {
        rcvClothes.visibility = View.GONE
        lnlAccessory.visibility = View.VISIBLE
        val categories = CATEGORY_ORDER.filter(catalog::containsKey) + catalog.keys.filterNot(CATEGORY_ORDER::contains)
        categoryAdapter.submit(categories, catalog, categories.firstOrNull().orEmpty())
        categories.firstOrNull()?.let(::selectCategory)
    }

    private fun selectClothes(path: String) {
        selectedClothes = path
        clothesAdapter.selected = path
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        categoryAdapter.selected = category
        accessoryAdapter.submit(category, catalog[category].orEmpty(), selectedAccessories[category])
    }

    private fun selectAccessory(name: String?) {
        if (name == null) selectedAccessories.remove(selectedCategory)
        else selectedAccessories[selectedCategory] = "$SPECIAL_ROOT/3D/$selectedCategory/$name.glb"
        accessoryAdapter.selectedName = name
    }

    private fun returnSelection() {
        val result = Intent().putExtra(EXTRA_RESULT_TYPE, pickType)
        if (pickType == OutfitViewerActivity.TYPE_ACCESSORY) {
            result.putExtra(EXTRA_RESULT_JSON, JSONObject().also { json -> selectedAccessories.forEach(json::put) }.toString())
        } else {
            result.putExtra(EXTRA_RESULT_URL, selectedClothes)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun parseSelectedAccessories(json: String) {
        runCatching {
            val source = JSONObject(json)
            source.keys().forEach { key -> selectedAccessories[key] = source.getString(key) }
        }
    }

    private fun loadCatalog(): ClothesCatalogResponse = runCatching {
        val connection = URL(OutfitUrls.ST253_API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.inputStream.bufferedReader().use { Gson().fromJson(it, ClothesCatalogResponse::class.java) }
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(ClothesCatalogResponse())

    private class ClothesAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<ChoiceHolder>() {
        private var items = emptyList<String>()
        var selected = ""
            set(value) { field = value; notifyDataSetChanged() }
        fun submit(value: List<String>, selectedValue: String) { items = value; selected = selectedValue }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChoiceHolder(ItemAccessoryChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ChoiceHolder, position: Int) {
            val item = items[position]
            holder.bind(item, item == selected, onClick)
        }
    }

    private class ChoiceHolder(private val binding: ItemAccessoryChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(path: String, isSelected: Boolean, onClick: (String) -> Unit) {
            binding.vFocus.visibility = if (isSelected) View.VISIBLE else View.GONE
            loadImage(binding.root.context, path, binding.imvImage)
            binding.root.setOnClickListener { onClick(path) }
        }
    }

    private class CategoryAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<CategoryAdapter.Holder>() {
        private var items = emptyList<String>()
        private var models = emptyMap<String, List<String>>()
        var selected = ""
            set(value) { field = value; notifyDataSetChanged() }
        fun submit(value: List<String>, source: Map<String, List<String>>, selectedValue: String) {
            items = value; models = source; selected = selectedValue
        }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemAccessoryCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val category = items[position]
            holder.bind(category, models[category]?.firstOrNull(), category == selected, onClick)
        }
        class Holder(private val binding: ItemAccessoryCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(category: String, preview: String?, selected: Boolean, onClick: (String) -> Unit) {
                binding.vFocus.visibility = if (selected) View.VISIBLE else View.GONE
                if (preview == null) {
                    binding.imvImage.setImageDrawable(null)
                } else {
                    loadImage(binding.root.context, "$SPECIAL_ROOT/2D/$category/$preview.png", binding.imvImage)
                }
                binding.root.setOnClickListener { onClick(category) }
            }
        }
    }

    private inner class ChoiceAdapter(private val onClick: (String?) -> Unit) : RecyclerView.Adapter<ChoiceAdapter.Holder>() {
        private var category = ""
        private var items = emptyList<String?>()
        var selectedName: String? = null
            set(value) { field = value; notifyDataSetChanged() }
        fun submit(type: String, names: List<String>, selectedUrl: String?) {
            category = type
            items = listOf(null) + names
            selectedName = selectedUrl?.substringAfterLast('/')?.removeSuffix(".glb")
        }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemAccessoryChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
        inner class Holder(private val binding: ItemAccessoryChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(name: String?) {
                binding.vFocus.visibility = if (name == selectedName) View.VISIBLE else View.GONE
                if (name == null) {
                    Glide.with(binding.root).load(R.drawable.ic_none_accessory).into(binding.imvImage)
                } else {
                    loadImage(binding.root.context, "$SPECIAL_ROOT/2D/$category/$name.png", binding.imvImage)
                }
                binding.root.setOnClickListener { onClick(name) }
            }
        }
    }

    companion object {
        const val EXTRA_PICK_TYPE = "pick_type"
        const val EXTRA_SELECTED_URL = "selected_url"
        const val EXTRA_SELECTED_JSON = "selected_accessories"
        const val EXTRA_RESULT_TYPE = "result_type"
        const val EXTRA_RESULT_URL = "result_url"
        const val EXTRA_RESULT_JSON = "result_accessories"
        private const val SPECIAL_ROOT = "https://lvtglobal.tech/public/app/ST253_ClothesSkinsMakerforRBX_v2"
        private val CATEGORY_ORDER = listOf("glasses", "hair", "hat", "lefthand", "neck", "righthand", "shoulder", "waist", "wing")
    }
}
