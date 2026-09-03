package com.skin.rbx.clothes.makek.ui.outfit
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.databinding.BottomSheetAccessoryPickerBinding
import com.skin.rbx.clothes.makek.databinding.ItemAccessorySheetCategoryBinding
import com.skin.rbx.clothes.makek.databinding.ItemAccessorySheetChoiceBinding
import com.skin.rbx.clothes.makek.ui.clothes.ClothesCatalogResponse

class AccessoryPickerBottomSheet(
    private val host: Activity,
    private val pickType: String,
    private val response: ClothesCatalogResponse,
    selectedClothesUrl: String,
    selectedAccessoryUrls: Map<String, String>,
    private val onClothesSelected: (type: String, url: String) -> Unit,
    private val onAccessoriesChanged: (Map<String, String>) -> Unit,
) : BottomSheetDialog(host) {

    private lateinit var binding: BottomSheetAccessoryPickerBinding
    private val selectedAccessories = linkedMapOf<String, String>().apply { putAll(selectedAccessoryUrls) }
    private var selectedClothes = selectedClothesUrl
    private var selectedCategory = ""
    private val categoryAdapter = CategoryAdapter { category ->
        host.checkInternet { selectCategory(category) }
    }
    private val accessoryAdapter = AccessoryAdapter { accessory ->
        host.checkInternet { selectAccessory(accessory) }
    }
    private val clothesAdapter = ClothesAdapter { clothes ->
        host.checkInternet { selectClothes(clothes) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BottomSheetAccessoryPickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.setDimAmount(0f)
        setCanceledOnTouchOutside(true)
        setOnShowListener {
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
                setBackgroundColor(Color.parseColor("#E8E8E8"))
                updateLayoutParams { height = ViewGroup.LayoutParams.MATCH_PARENT }
                BottomSheetBehavior.from(this).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = false
                }
            }
        }
        initList()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.heightPixels * SHEET_HEIGHT_RATIO).toInt()
            )
            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            dismiss()
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun initList() = with(binding) {
        rcvItem.layoutManager = GridLayoutManager(host, GRID_SPAN_COUNT)
        if (pickType == OutfitViewerActivity.TYPE_ACCESSORY) {
            rcvCategory.visibility = View.VISIBLE
            rcvCategory.layoutManager = LinearLayoutManager(host, RecyclerView.HORIZONTAL, false)
            rcvCategory.adapter = categoryAdapter
            rcvItem.adapter = accessoryAdapter

            val categories = CATEGORY_ORDER.filter(response.models::containsKey) +
                response.models.keys.filterNot(CATEGORY_ORDER::contains)
            categoryAdapter.submit(categories, response.models, categories.firstOrNull().orEmpty())
            categories.firstOrNull()?.let(::selectCategory)
        } else {
            rcvCategory.visibility = View.GONE
            rcvItem.adapter = clothesAdapter
            val quantity = response.folders.firstOrNull { it.category == "special" }?.quantity ?: 0
            clothesAdapter.submit((1..quantity).map { "${OutfitUrls.ST253_PUBLIC}/special/$it.png" }, selectedClothes)
        }
    }

    private fun selectClothes(url: String) {
        selectedClothes = url
        clothesAdapter.selected = url
        onClothesSelected(pickType, url)
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        categoryAdapter.selected = category
        accessoryAdapter.submit(category, response.models[category].orEmpty(), selectedAccessories[category])
    }

    private fun selectAccessory(name: String?) {
        if (name == null) selectedAccessories.remove(selectedCategory)
        else selectedAccessories[selectedCategory] = "${OutfitUrls.ST253_PUBLIC}/3D/$selectedCategory/$name.glb"
        accessoryAdapter.selectedName = name
        onAccessoriesChanged(selectedAccessories)
    }

    private class ClothesAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<ChoiceHolder>() {
        private var items = emptyList<String>()
        var selected = ""
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(value: List<String>, selectedValue: String) {
            items = value
            selected = selectedValue
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ChoiceHolder(ItemAccessorySheetChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ChoiceHolder, position: Int) {
            val item = items[position]
            holder.bind(item, item == selected) { onClick(item) }
        }
    }

    private class ChoiceHolder(private val binding: ItemAccessorySheetChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(source: Any, selected: Boolean, onClick: () -> Unit) = with(binding) {
            cardItem.strokeWidth = if (selected) UnitHelper.dpToPxInt(root.resources, 2f) else 0
            cardItem.strokeColor = Color.BLACK
            cardItem.cardElevation = UnitHelper.dpToPx(root.resources, 1f)
            imvNoneAccessory.visibility = View.GONE
            imvImage.visibility = View.VISIBLE
            loadImage(
                source,
                imvImage,
                onShowLoading = {
                    sflShimmer.visibility = View.VISIBLE
                    sflShimmer.startShimmer()
                },
                onDismissLoading = {
                    sflShimmer.stopShimmer()
                    sflShimmer.visibility = View.GONE
                }
            )
            root.setOnClickListener { onClick() }
        }
    }

    private class CategoryAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<CategoryAdapter.Holder>() {
        private var items = emptyList<String>()
        private var models = emptyMap<String, List<String>>()
        var selected = ""
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(value: List<String>, source: Map<String, List<String>>, selectedValue: String) {
            items = value
            models = source
            selected = selectedValue
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemAccessorySheetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val category = items[position]
            holder.bind(category, models[category]?.firstOrNull(), category == selected, onClick)
        }

        class Holder(private val binding: ItemAccessorySheetCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(category: String, preview: String?, selected: Boolean, onClick: (String) -> Unit) = with(binding) {
                cardItem.strokeWidth = if (selected) UnitHelper.dpToPxInt(root.resources, 2f) else 0
                cardItem.strokeColor = Color.BLACK
                if (preview == null) {
                    imvImage.setImageDrawable(null)
                } else {
                    loadImage(root.context, "${OutfitUrls.ST253_PUBLIC}/2D/$category/$preview.png", imvImage)
                }
                root.setOnClickListener { onClick(category) }
            }
        }
    }

    private inner class AccessoryAdapter(private val onClick: (String?) -> Unit) :
        RecyclerView.Adapter<AccessoryAdapter.Holder>() {
        private var category = ""
        private var items = emptyList<String?>()
        var selectedName: String? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(type: String, names: List<String>, selectedUrl: String?) {
            category = type
            items = listOf(null) + names
            selectedName = selectedUrl?.substringAfterLast('/')?.removeSuffix(".glb")
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemAccessorySheetChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

        inner class Holder(private val binding: ItemAccessorySheetChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(name: String?) = with(binding) {
                cardItem.strokeWidth = if (name == selectedName) UnitHelper.dpToPxInt(root.resources, 2f) else 0
                cardItem.strokeColor = Color.BLACK
                cardItem.cardElevation = UnitHelper.dpToPx(root.resources, 1f)
                if (name == null) {
                    imvImage.visibility = View.GONE
                    imvNoneAccessory.visibility = View.VISIBLE
                    sflShimmer.stopShimmer()
                    sflShimmer.visibility = View.GONE
                } else {
                    imvNoneAccessory.visibility = View.GONE
                    imvImage.visibility = View.VISIBLE
                    loadImage(
                        "${OutfitUrls.ST253_PUBLIC}/2D/$category/$name.png",
                        imvImage,
                        onShowLoading = {
                            sflShimmer.visibility = View.VISIBLE
                            sflShimmer.startShimmer()
                        },
                        onDismissLoading = {
                            sflShimmer.stopShimmer()
                            sflShimmer.visibility = View.GONE
                        }
                    )
                }
                root.setOnClickListener { onClick(name) }
            }
        }
    }
    companion object {
        private const val SHEET_HEIGHT_RATIO = 0.35f
        private const val GRID_SPAN_COUNT = 4
        private val CATEGORY_ORDER = listOf("glasses", "hair", "hat", "lefthand", "neck", "righthand", "shoulder", "waist", "wing")
    }
}
