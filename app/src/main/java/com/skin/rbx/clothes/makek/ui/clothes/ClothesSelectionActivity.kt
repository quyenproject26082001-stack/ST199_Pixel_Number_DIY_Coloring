package com.skin.rbx.clothes.makek.ui.clothes

import android.content.Intent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ActivityClothesSelectionBinding
import com.skin.rbx.clothes.makek.ui.clothes.ClothesMode
import com.skin.rbx.clothes.makek.ui.outfit.OutfitViewerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ClothesSelectionActivity : BaseActivity<ActivityClothesSelectionBinding>() {

    private val clothesAdapter = ClothesAdapter()
    private val accessoryAdapter = AccessoryClothesAdapter()
    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: ClothesMode.BASIC_OUTFIT }

    override fun setViewBinding(): ActivityClothesSelectionBinding {
        return ActivityClothesSelectionBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        val list = if (mode == ClothesMode.ACCESSORY) binding.rcvAccessory else binding.rcvClothes
        binding.rcvClothes.visibility =
            if (mode == ClothesMode.ACCESSORY) android.view.View.GONE else android.view.View.VISIBLE
        binding.rcvAccessory.visibility =
            if (mode == ClothesMode.ACCESSORY) android.view.View.VISIBLE else android.view.View.GONE
        val spanCount = if (mode == ClothesMode.ACCESSORY) 3 else 2
        clothesAdapter.useAccessoryStyle = mode == ClothesMode.ACCESSORY
        accessoryAdapter.useAccessoryStyle = mode == ClothesMode.ACCESSORY
        list.apply {
            adapter = if (mode == ClothesMode.ACCESSORY) accessoryAdapter else clothesAdapter
            itemAnimator = null
            (layoutManager as? GridLayoutManager)?.spanCount = spanCount
        }

        lifecycleScope.launch {
            showLoading()
            val items = withContext(Dispatchers.IO) { loadItems() }
            dismissLoading()
            if (mode == ClothesMode.ACCESSORY) accessoryAdapter.submitList(items) else clothesAdapter.submitList(
                items
            )
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        clothesAdapter.onItemClick = { item -> checkInternet { openViewer(item) } }
        accessoryAdapter.onItemClick = { item -> checkInternet { openViewer(item) } }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.visible()
            when (mode) {
                ClothesMode.SPECIAL -> tvCenter.setText(R.string.special_clothes1)
                ClothesMode.ACCESSORY -> tvCenter.setText(R.string.assessory)
                ClothesMode.BASIC_SHIRT -> tvCenter.setText(R.string.shirt1)
                ClothesMode.BASIC_PANT -> tvCenter.setText(R.string.pant1)
                ClothesMode.BASIC_OUTFIT -> tvCenter.setText(R.string.outfit)
                else -> tvCenter.setText(R.string.outfit)

            }
            tvCenter.visible()
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        }
    }

    private fun loadItems(): List<ClothesListItem> {
        return runCatching {
            when (mode) {
                ClothesMode.SPECIAL -> loadSpecialItems()
                ClothesMode.ACCESSORY -> loadAccessoryItems()
                ClothesMode.BASIC_SHIRT -> loadLocalItems(BASIC_SHIRT_PATH, ClothesMode.BASIC_SHIRT)
                ClothesMode.BASIC_PANT -> loadLocalItems(BASIC_PANT_PATH, ClothesMode.BASIC_PANT)
                else -> loadLocalItems(COMBO_SHIRT_PATH, ClothesMode.BASIC_OUTFIT)
            }
        }.getOrElse { emptyList() }
    }

    private fun loadLocalItems(path: String, itemMode: String): List<ClothesListItem> {
        return assets.list(path)
            .orEmpty()
            .sortedBy { it.substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
            .map { fileName ->
                val previewUrl = "$ASSET_PREVIEW_URL/$path/$fileName"
                val webUrl = "$ASSET_WEB_URL/$path/$fileName"
                when (itemMode) {
                    ClothesMode.BASIC_SHIRT -> ClothesListItem(previewUrl, shirtUrl = webUrl)
                    ClothesMode.BASIC_PANT -> ClothesListItem(previewUrl, pantUrl = webUrl)
                    else -> ClothesListItem(
                        previewUrl = previewUrl,
                        shirtUrl = webUrl,
                        pantUrl = "$ASSET_WEB_URL/$COMBO_PANT_PATH/$fileName",
                    )
                }
            }
    }

    private fun loadSpecialItems(): List<ClothesListItem> {
        val catalog = loadCatalog()
        val quantity = catalog.folders.firstOrNull { it.category == "special" }?.quantity ?: 0
        return (1..quantity).map { index ->
            val imageUrl = "$ST253_PUBLIC_URL/special/$index.png"
            ClothesListItem(previewUrl = imageUrl, shirtUrl = imageUrl, pantUrl = imageUrl)
        }
    }

    private fun loadAccessoryItems(): List<ClothesListItem> {
        return loadCatalog().models.flatMap { (type, names) ->
            names.map { name ->
                ClothesListItem(
                    previewUrl = "$ST253_PUBLIC_URL/2D/$type/$name.png",
                    accessoryType = type,
                    accessoryUrl = "$ST253_PUBLIC_URL/3D/$type/$name.glb",
                )
            }
        }
    }

    private fun loadCatalog(): ClothesCatalogResponse {
        val connection = URL(ST253_API_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.inputStream.bufferedReader().use {
                Gson().fromJson(it, ClothesCatalogResponse::class.java)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openViewer(item: ClothesListItem) {
        startActivity(Intent(this, OutfitViewerActivity::class.java).apply {
            putExtra(OutfitViewerActivity.EXTRA_MODE, mode)
            putExtra(OutfitViewerActivity.EXTRA_SHIRT_URL, item.shirtUrl)
            putExtra(OutfitViewerActivity.EXTRA_PANT_URL, item.pantUrl)
            putExtra(OutfitViewerActivity.EXTRA_ACCESSORY_TYPE, item.accessoryType)
            putExtra(OutfitViewerActivity.EXTRA_ACCESSORY_URL, item.accessoryUrl)
        })
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    companion object {
        const val EXTRA_MODE = "clothes_mode"

        private const val BASIC_SHIRT_PATH = "skin/basic/shirt"
        private const val BASIC_PANT_PATH = "skin/basic/pant"
        private const val COMBO_SHIRT_PATH = "skin/combo/shirt"
        private const val COMBO_PANT_PATH = "skin/combo/pant"
        private const val ASSET_PREVIEW_URL = "file:///android_asset"
        private const val ASSET_WEB_URL = "https://appassets.androidplatform.net/assets"
        private const val ST253_API_URL =
            "https://lvtglobal.tech/api/ST253_Clothes_Skins_Maker_for_RBX_v2"
        private const val ST253_PUBLIC_URL =
            "https://lvtglobal.tech/public/app/ST253_ClothesSkinsMakerforRBX_v2"
    }
}
