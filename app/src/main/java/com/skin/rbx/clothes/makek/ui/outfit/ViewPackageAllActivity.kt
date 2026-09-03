package com.skin.rbx.clothes.makek.ui.outfit

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.core.helper.LanguageHelper
import com.skin.rbx.clothes.makek.databinding.ActivityViewPackageAllBinding
import com.skin.rbx.clothes.makek.databinding.ItemViewPackageAllBinding
import com.skin.rbx.clothes.makek.dialog.YesNoDialog
import kotlinx.coroutines.launch
import java.io.File

class ViewPackageAllActivity : BaseActivity<ActivityViewPackageAllBinding>() {
    private val packageId by lazy { intent.getStringExtra(EXTRA_PACKAGE_ID).orEmpty() }
    private val adapter = PackageItemAdapter { entry ->
        startActivity(
            Intent(this, ViewPackageActivity::class.java)
                .putExtra(ViewPackageActivity.EXTRA_PACKAGE_ID, packageId)
                .putExtra(ViewPackageActivity.EXTRA_ENTRY, Gson().toJson(entry))
        )
    }

    override fun setViewBinding() = ActivityViewPackageAllBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        binding.rcvItems.apply {
            adapter = this@ViewPackageAllActivity.adapter
            itemAnimator = null
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                OutfitPackageRepository
                    .observePackage(this@ViewPackageAllActivity, packageId)
                    .collect { item ->
                        if (item == null) {
                            if (!isFinishing) finish()
                        } else {
                            adapter.submit(item.entries)
                        }
                    }
            }
        }
    }

    override fun initActionBar() = with(binding.actionBar) {
        btnActionBarLeft.visible()
        btnActionBarRight.visible()
        btnActionBarLeft.setImageResource(R.drawable.ic_back)
        btnActionBarRight.setImageResource(R.drawable.ic_delete)
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
        binding.actionBar.btnActionBarRight.tap {
            confirmDelete()
        }
    }

    private fun confirmDelete() {
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
                OutfitPackageRepository.deletePackages(
                    this@ViewPackageAllActivity,
                    listOf(packageId),
                )
                finish()
            }
        }
    }

    private class PackageItemAdapter(
        private val onClick: (DownloadEntry) -> Unit,
    ) : RecyclerView.Adapter<PackageItemAdapter.Holder>() {
        private var items = emptyList<DownloadEntry>()
        fun submit(value: List<DownloadEntry>) {
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemViewPackageAllBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], onClick)

        class Holder(private val binding: ItemViewPackageAllBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: DownloadEntry, onClick: (DownloadEntry) -> Unit) {
                binding.tvName.text = item.type.replaceFirstChar { it.uppercase() }
                binding.tvFileType.text = if (item.type == OutfitViewerActivity.TYPE_SHIRT || item.type == OutfitViewerActivity.TYPE_PANT) "PNG" else "GLB"
                val file = OutfitUrls.localFileName(item.previewUrl)?.let { File(binding.root.context.filesDir, "outfit/$it") }
                Glide.with(binding.root)
                    .load(file ?: OutfitUrls.glideSource(item.previewUrl, binding.root.context.filesDir))
                    .into(binding.imvPreview)
                binding.root.tap { onClick(item) }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_ID = "extra_package_id"
    }
}
