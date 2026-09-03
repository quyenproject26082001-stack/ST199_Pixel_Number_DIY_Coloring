package com.skin.rbx.clothes.makek.ui.outfit

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkPermissions
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.goToSettings
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.requestPermission
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ActivityOutfitDownloadBinding
import com.skin.rbx.clothes.makek.databinding.ItemOutfitDownloadBinding
import com.skin.rbx.clothes.makek.core.utils.key.RequestKey
import com.skin.rbx.clothes.makek.ui.home.HomeActivity
import com.skin.rbx.clothes.makek.ui.permission.PermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class OutfitDownloadActivity : BaseActivity<ActivityOutfitDownloadBinding>() {
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var pendingDownloadEntry: DownloadEntry? = null

    private val entries by lazy {
        runCatching { Gson().fromJson(intent.getStringExtra(EXTRA_ENTRIES), Array<DownloadEntry>::class.java).toList() }
            .getOrDefault(emptyList())
    }
    private val adapter = DownloadAdapter(::openPreview, ::download)

    override fun setViewBinding() = ActivityOutfitDownloadBinding.inflate(LayoutInflater.from(this))
    override fun initView() { binding.rcvDownload.adapter = adapter; adapter.submit(entries) }
    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap {
           // startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
    }
    override fun initActionBar() = with(binding.actionBar) {
        btnActionBarLeft.visible(); btnActionBarRight.visible()
        tvCenter.visible()
        tvCenter.setText(R.string.successfully)
        setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        setImageActionBar(btnActionBarRight, R.drawable.ic_htu)
        btnActionBarRight.tap { startActivity(Intent(this@OutfitDownloadActivity, OutfitHowToActivity::class.java)) }
    }

    private fun openPreview(entry: DownloadEntry) {
        startActivity(Intent(this, OutfitPreviewActivity::class.java).putExtra(OutfitPreviewActivity.EXTRA_ENTRY, Gson().toJson(entry)))
    }

    private fun download(entry: DownloadEntry) {
        pendingDownloadEntry = entry
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload()
        } else {
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload()
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleDownload() {
        val entry = pendingDownloadEntry ?: return
        pendingDownloadEntry = null
        lifecycleScope.launch {
            showLoading()
            val success = withContext(Dispatchers.IO) { OutfitDownloadHelper.save(this@OutfitDownloadActivity, entry) }
            dismissLoading()
            showToast(if (success) R.string.download_success else R.string.download_failed_please_try_again_later)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != RequestKey.STORAGE_PERMISSION_CODE) return

        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            permissionViewModel.updateStorageGranted(sharePreference, true)
            handleDownload()
        } else {
            permissionViewModel.updateStorageGranted(sharePreference, false)
            pendingDownloadEntry = null
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() = Unit

    private class DownloadAdapter(
        private val onPreview: (DownloadEntry) -> Unit,
        private val onDownload: (DownloadEntry) -> Unit,
    ) : RecyclerView.Adapter<DownloadAdapter.Holder>() {
        private var items = emptyList<DownloadEntry>()
        fun submit(value: List<DownloadEntry>) { items = value; notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemOutfitDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], onPreview, onDownload)
        class Holder(private val binding: ItemOutfitDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: DownloadEntry, onPreview: (DownloadEntry) -> Unit, onDownload: (DownloadEntry) -> Unit) {
                binding.tvTypeClothes.text = item.type.replaceFirstChar { it.uppercase() }
                binding.tvExtension.text = if (item.type == "shirt" || item.type == "pant") "PNG" else "GLB"
                val file = OutfitUrls.localFileName(item.previewUrl)?.let { File(binding.root.context.filesDir, "outfit/$it") }
                Glide.with(binding.root).load(file ?: OutfitUrls.glideSource(item.previewUrl, binding.root.context.filesDir)).into(binding.imvThumb)
                binding.btnDownload.setOnClickListener { onDownload(item) }
            }
        }
    }

    companion object { const val EXTRA_ENTRIES = "download_entries" }
}
