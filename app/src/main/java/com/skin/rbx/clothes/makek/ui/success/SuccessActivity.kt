package com.skin.rbx.clothes.makek.ui.success

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lvt.ads.util.Admob
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkPermissions
import com.skin.rbx.clothes.makek.core.extensions.goToSettings
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.invisible
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.loadNativeCollabAds
import com.skin.rbx.clothes.makek.core.extensions.requestPermission
import com.skin.rbx.clothes.makek.core.extensions.select
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.shareImagePathToPackage
import com.skin.rbx.clothes.makek.core.extensions.showInterAll
import com.skin.rbx.clothes.makek.core.extensions.startIntentRightToLeft
import com.skin.rbx.clothes.makek.core.extensions.startIntentWithClearTop
import com.skin.rbx.clothes.makek.core.extensions.strings
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.core.helper.UnitHelper
import com.skin.rbx.clothes.makek.core.utils.key.IntentKey
import com.skin.rbx.clothes.makek.core.utils.key.RequestKey
import com.skin.rbx.clothes.makek.core.utils.key.ValueKey
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.databinding.ActivitySuccessBinding
import com.skin.rbx.clothes.makek.ui.home.HomeActivity
import com.skin.rbx.clothes.makek.ui.my_creation.MyCreationActivity
import com.skin.rbx.clothes.makek.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch

class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {
    private val viewModel: SuccessViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    override fun setViewBinding(): ActivitySuccessBinding {
        return ActivitySuccessBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY) ?: "")
      //  setButtonBackgrounds()
//        binding.includeLayoutBottom.tvIns.isSelected =true
//        binding.includeLayoutBottom.tvFB.isSelected =true
    }

//    private fun setButtonBackgrounds() {
//        binding.includeLayoutBottom.apply {
//
//            tvDownload.select()
//            tvShare.select()
//
//        }
//    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pathInternal.collect { path ->
                        if (path.isNotEmpty()) {
                            loadImage(this@SuccessActivity, path, binding.imvImage)
                        }
                    }
                }
            }
        }
    }

    private fun handleBack() {
        handleBackLeftToRight()
    }
    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap {  handleBack()  }

                btnActionBarNextRight.tap(2000){
                    showInterAll {  startIntentWithClearTop(HomeActivity::class.java)}

                }
                btnActionBarRight.tap(2000) {
                    showInterAll {  startIntentWithClearTop(HomeActivity::class.java)}

                }

                btnCreation.tap(2590) {
                    showInterAll {
                        startIntentRightToLeft(MyCreationActivity::class.java, IntentKey.TAB_KEY, ValueKey.MY_DESIGN_TYPE)
                        finish()
                    }
                }
            }
            btnShareSuccess.tap(1000){
                //checkStoragePermission()
                viewModel.shareFiles(this@SuccessActivity)

            }

            // My Album button


            // Download button
//            includeLayoutBottom.btnTelegram.tap(2000) {
//                checkStoragePermission()
//            }
//            includeLayoutBottom.btnFBShare.tap(2000) {
//                shareCurrentImageTo(FACEBOOK_PACKAGE)
//            }


        }
    }

    private fun shareCurrentImageTo(targetPackage: String) {
        val shared = shareImagePathToPackage(
            viewModel.pathInternal.value,
            targetPackage
        )
        if (!shared) {
            showToast(R.string.no_app_found_to_handle_this_action)
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {

            btnActionBarLeft.visible()
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarNextRight.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_home)
            tvCenter.visible()
            tvCenter.setText(R.string.successfully)
            tvCenter.updateLayoutParams {

            }
            imgCenter.gone()



        }
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
        lifecycleScope.launch {
            viewModel.downloadFiles(this@SuccessActivity).collect { state ->
                when (state) {
                    HandleState.LOADING -> showLoading()
                    HandleState.SUCCESS -> {
                        dismissLoading()
                        showToast(R.string.download_success)
                    }
                    else -> {
                        dismissLoading()
                        showToast(R.string.download_failed_please_try_again_later)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                handleDownload()
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
            }
        }
    }

    override fun initAds() {
//        initNativeCollab()
    }

    fun initNativeCollab() {
//        Admob.getInstance().loadNativeAd(this@SuccessActivity, getString(R.string.native_success), binding.nativeAds, R.layout.ads_native_big_btn_top)
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackLeftToRight()
    }

    private companion object {
        const val FACEBOOK_PACKAGE = "com.facebook.katana"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }
}
