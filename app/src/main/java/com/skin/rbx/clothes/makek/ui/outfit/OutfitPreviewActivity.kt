package com.skin.rbx.clothes.makek.ui.outfit

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ActivityOutfitPreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class OutfitPreviewActivity : BaseActivity<ActivityOutfitPreviewBinding>() {
    private val entry by lazy { Gson().fromJson(intent.getStringExtra(EXTRA_ENTRY), DownloadEntry::class.java) }
    override fun setViewBinding() = ActivityOutfitPreviewBinding.inflate(LayoutInflater.from(this))
    override fun initView() {
        binding.tvType.text = entry.type.replaceFirstChar { it.uppercase() }
        val file = OutfitUrls.localFileName(entry.previewUrl)?.let { File(filesDir, "outfit/$it") }
        Glide.with(this).load(file ?: OutfitUrls.glideSource(entry.previewUrl, filesDir)).into(binding.imvPreview)
    }
    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        binding.btnDownload.tap {
            lifecycleScope.launch {
                showLoading()
                val success = withContext(Dispatchers.IO) { OutfitDownloadHelper.save(this@OutfitPreviewActivity, entry) }
                dismissLoading()
                showToast(if (success) R.string.download_success else R.string.download_failed_please_try_again_later)
            }
        }
    }
    override fun initActionBar() = with(binding.actionBar) {
        btnActionBarLeft.visible(); tvCenter.visible()
        setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        setTextActionBar(tvCenter, getString(R.string.preview))
    }
    companion object { const val EXTRA_ENTRY = "preview_entry" }
}
