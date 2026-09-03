package com.skin.rbx.clothes.makek.ui.outfit

import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkPermissions
import com.skin.rbx.clothes.makek.core.extensions.goToSettings
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.requestPermission
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.core.helper.LanguageHelper
import com.skin.rbx.clothes.makek.core.utils.key.RequestKey
import com.skin.rbx.clothes.makek.databinding.ActivityViewPackageBinding
import com.skin.rbx.clothes.makek.dialog.YesNoDialog
import com.skin.rbx.clothes.makek.ui.permission.PermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ViewPackageActivity : BaseActivity<ActivityViewPackageBinding>() {
    private val permissionViewModel: PermissionViewModel by viewModels()
    private val packageId by lazy { intent.getStringExtra(EXTRA_PACKAGE_ID).orEmpty() }
    private val entry by lazy {
        runCatching { Gson().fromJson(intent.getStringExtra(EXTRA_ENTRY), DownloadEntry::class.java) }.getOrNull()
    }

    override fun setViewBinding() = ActivityViewPackageBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        val item = entry ?: run {
            finish()
            return
        }
        binding.tvName.text = item.type.replaceFirstChar { it.uppercase() }
        binding.tvFileType.text = if (item.type == OutfitViewerActivity.TYPE_SHIRT || item.type == OutfitViewerActivity.TYPE_PANT) "PNG" else "GLB"
        val file = OutfitUrls.localFileName(item.previewUrl)?.let { File(filesDir, "outfit/$it") }
        Glide.with(this)
            .load(file ?: OutfitUrls.glideSource(item.previewUrl, filesDir))
            .into(binding.imvPreview)
    }

    override fun initActionBar() = with(binding.actionBar) {
        btnActionBarLeft.visible()
        btnActionBarNextRight.visible()
        btnActionBarRight.visible()
        btnActionBarLeft.setImageResource(R.drawable.ic_back)
        btnActionBarNextRight.setImageResource(R.drawable.ic_delete)
        btnActionBarRight.setImageResource(R.drawable.ic_download)
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        binding.actionBar.btnActionBarRight.tap {
            checkStoragePermission()
        }
        binding.actionBar.btnActionBarNextRight.tap {
            confirmDelete()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload()
            return
        }

        val permissions = permissionViewModel.getStoragePermissions()
        when {
            checkPermissions(permissions) -> handleDownload()
            permissionViewModel.needGoToSettings(sharePreference, true) -> goToSettings()
            else -> requestPermission(permissions, RequestKey.STORAGE_PERMISSION_CODE)
        }
    }

    private fun handleDownload() {
        val item = entry ?: return
        lifecycleScope.launch {
            showLoading()
            val success = withContext(Dispatchers.IO) {
                OutfitDownloadHelper.save(this@ViewPackageActivity, item)
            }
            dismissLoading()
            showToast(
                if (success) R.string.download_success
                else R.string.download_failed_please_try_again_later
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != RequestKey.STORAGE_PERMISSION_CODE) return

        if (grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            permissionViewModel.updateStorageGranted(sharePreference, true)
            handleDownload()
        } else {
            permissionViewModel.updateStorageGranted(sharePreference, false)
        }
    }

    private fun confirmDelete() {
        val item = entry ?: return
        val dialog = YesNoDialog(
            this,
            R.string.delete,
            R.string.are_you_sure_want_to_delete_this_item,
        )
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onYesClick = {
            dialog.dismiss()
            lifecycleScope.launch {
                val pkg = OutfitPackageRepository.getPackage(this@ViewPackageActivity, packageId)
                    ?: return@launch
                val updatedEntries = pkg.entries.filterNot {
                    it.type == item.type && it.sourceUrl == item.sourceUrl
                }
                OutfitPackageRepository.updatePackage(
                    this@ViewPackageActivity,
                    pkg.copy(entries = updatedEntries),
                )
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_ID = "extra_package_id"
        const val EXTRA_ENTRY = "extra_entry"
    }
}
