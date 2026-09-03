package com.skin.rbx.clothes.makek.ui.cosplay
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import androidx.constraintlayout.widget.ConstraintSet
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkPermissions
import com.skin.rbx.clothes.makek.core.extensions.goToSettings
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.loadImage
import com.skin.rbx.clothes.makek.core.extensions.loadNativeCollabAds
import com.skin.rbx.clothes.makek.core.extensions.requestPermission
import com.skin.rbx.clothes.makek.core.extensions.showInterAll
import com.skin.rbx.clothes.makek.core.extensions.startIntentWithClearTop
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.core.utils.key.IntentKey
import com.skin.rbx.clothes.makek.core.utils.key.RequestKey
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.databinding.ActivityCosplaySuccessfulBinding
import com.skin.rbx.clothes.makek.ui.home.HomeActivity
import com.skin.rbx.clothes.makek.ui.permission.PermissionViewModel
import com.skin.rbx.clothes.makek.ui.success.SuccessViewModel
import com.lvt.ads.util.Admob
import com.skin.rbx.clothes.makek.ui.my_creation.MyCreationActivity
import kotlinx.coroutines.launch

class CosplaySuccessfulActivity : BaseActivity<ActivityCosplaySuccessfulBinding>() {
    private var characterPosition = 0

    private val viewModel: SuccessViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private var finalProgress = 0
    private var targetImagePath = ""

    override fun setViewBinding(): ActivityCosplaySuccessfulBinding {
        return ActivityCosplaySuccessfulBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        val path = intent.getStringExtra(IntentKey.COSPLAY_RESULT_PATH_KEY) ?: ""
        finalProgress = intent.getIntExtra(IntentKey.COSPLAY_PROGRESS_KEY, 0)
        targetImagePath =
            intent.getStringExtra(IntentKey.COSPLAY_TARGET_IMAGE_PATH_KEY) ?: ""
        characterPosition = intent.getIntExtra(
            IntentKey.COSPLAY_CHARACTER_POSITION_KEY,
            0
        )

        binding.includeLayoutBottom.tvShare.isSelected = true
        viewModel.setPath(path)
        updateProgressDisplay()
        if (targetImagePath.isNotEmpty()) {
            loadImage(this, targetImagePath, binding.btnSamplePhoto)
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pathInternal.collect { path ->
                    if (path.isNotEmpty()) {
                        loadImage(this@CosplaySuccessfulActivity, path, binding.imageSS)
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap {
                showInterAll {
                    startIntentWithClearTop(HomeActivity::class.java)
                }

            }
            actionBar.btnActionBarLeft.tap {
                handleBackLeftToRight()
            }
            includeLayoutBottom.btnWhatsapp.tap(800) {
                showInterAll {
                    val intent = Intent(
                        this@CosplaySuccessfulActivity,
                        CosplayCustomizeActivity::class.java
                    ).apply {
                        putExtra(
                            IntentKey.COSPLAY_CHARACTER_POSITION_KEY,
                            characterPosition
                        )
                    }

                    startActivity(intent)
                    finish()
                }
            }

            actionBar.apply {
                btnActionBarNextRight.tap(1000){
                    val intent = Intent(this@CosplaySuccessfulActivity, MyCreationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                btnActionBarNextRight1.tap(1000){
                    viewModel.shareFiles(this@CosplaySuccessfulActivity)
                }
            }

            btnRetry.tap(1000){
                val intent = Intent(this@CosplaySuccessfulActivity, CosplayCustomizeActivity::class.java).apply {
                    putExtra(
                        IntentKey.COSPLAY_CHARACTER_POSITION_KEY,
                        characterPosition
                    )
                }
                startActivity(intent)
                finish()

            }

            btnDownload.tap(1000){
                checkStoragePermission()
            }



            includeLayoutBottom.btnTelegram.tap(2000) {
                viewModel.saveToAvatar(this@CosplaySuccessfulActivity) {
                    showToast(R.string.image_has_been_saved_successfully)
                }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarRight.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_home)
           // tvCenter.visible()
            btnActionBarNextRight.setImageResource(R.drawable.ic_creation)
            btnActionBarNextRight.visible()
            btnActionBarNextRight1.setImageResource(R.drawable.ic_share)
            btnActionBarNextRight1.visible()
           // tvCenter.setText(R.string.successfully1)
        }
    }

    private fun updateProgressDisplay() {
        binding.progressContainer.post {
            val progress = finalProgress.coerceIn(0, 100)
            val revealedWidth = binding.progressBar.width * progress / 100
            binding.progressBar.clipBounds = Rect(
                0,
                0,
                revealedWidth,
                binding.progressBar.height
            )

            if (finalProgress == 100) {
                binding.icThumb.translationX = 0f
                val cs = ConstraintSet()
                cs.clone(binding.main)
                cs.connect(
                    R.id.ic_thumb,
                    ConstraintSet.START,
                    R.id.progressContainer,
                    ConstraintSet.END
                )
                cs.connect(
                    R.id.ic_thumb,
                    ConstraintSet.END,
                    R.id.progressContainer,
                    ConstraintSet.END
                )
                cs.applyTo(binding.main)
            } else {
                val containerW = binding.progressContainer.width
                val thumbW = binding.icThumb.width
                val maxX = (containerW - thumbW).toFloat().coerceAtLeast(0f)
                val offsetPx = UnitHelper.dpToPx(resources, 5f)
                binding.icThumb.translationX = (finalProgress / 100f) * maxX - offsetPx
            }
            binding.tvProgress.text = "$finalProgress/100"
            binding.tvRef.text = getString(R.string.score) + "$finalProgress"
        }
        val starRes = when {
            finalProgress < 20 -> R.drawable.one_star_ss
            finalProgress < 40 -> R.drawable.two_star_ss
            finalProgress < 60 -> R.drawable.three_star_ss
            finalProgress < 80 -> R.drawable.four_star_ss
            else -> R.drawable.five_star_ss
        }
        binding.starSS.setImageResource(starRes)
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
            viewModel.downloadFiles(this@CosplaySuccessfulActivity).collect { state ->
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackLeftToRight()
    }


    fun initNativeCollab() {
//        loadNativeCollabAds(R.string.native_cl_cosplayDone, binding.flNativeCollab)
    }

    override fun initAds() {
//        initNativeCollab()
    }

    override fun onRestart() {
        super.onRestart()
    }


}
