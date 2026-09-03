package com.skin.rbx.clothes.makek.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.rateApp
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.showInterAll
import com.skin.rbx.clothes.makek.core.extensions.startIntentRightToLeft
import com.skin.rbx.clothes.makek.core.helper.LanguageHelper
import com.skin.rbx.clothes.makek.core.utils.state.RateState
import com.skin.rbx.clothes.makek.databinding.ActivityHomeBinding
import com.skin.rbx.clothes.makek.ui.SettingsActivity
import com.skin.rbx.clothes.makek.ui.my_creation.MyCreationActivity
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.strings
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.ui.cosplay.CosplayRandomActivity
import com.skin.rbx.clothes.makek.ui.clothes.ClothesMode
import com.skin.rbx.clothes.makek.ui.clothes.ClothesSelectionActivity
import com.skin.rbx.clothes.makek.ui.outfit.OutfitHowToActivity
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelGalleryActivity
import com.skin.rbx.clothes.makek.ui.trending.TrendingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        sharePreference.setCountBack(sharePreference.getCountBack() + 1)
        binding.tv1.isSelected = true
        //  binding.tv3.isSelected = true
        binding.tv2.isSelected = true

        binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_setting)
        // Apply elastic bounce animation to app name
        val elasticBounce = AnimationUtils.loadAnimation(this, R.anim.elastic_bounce)
        // binding.imvAppName.startAnimation(elasticBounce)
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap(800) { startIntentRightToLeft(SettingsActivity::class.java) }
            actionBar.btnActionBarLeft.tap(800) { showInterAll { startIntentRightToLeft(MyCreationActivity::class.java) } }
            btn1.tap(800) { startIntentRightToLeft(PixelGalleryActivity::class.java) }
            btn2.tap(800) { startIntentRightToLeft(MyCreationActivity::class.java) }

        }
    }

    override fun initText() {
        super.initText()
        //binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
      binding.actionBar.apply {
        setImageActionBar(btnActionBarRight, R.drawable.ic_setting)
          btnActionBarLeft.setImageResource(R.drawable.ic_creation)
          btnActionBarLeft.visible()
          btnActionBarRight.visible()
      }
    }

    // Enable background music for HomeActivity
    override fun shouldPlayBackgroundMusic(): Boolean = false

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!sharePreference.getIsRate(this) && sharePreference.getCountBack() % 2 == 0) {
            rateApp(sharePreference) { state ->
                if (state != RateState.CANCEL) {
                    showToast(R.string.have_rated)
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        delay(1000)
                        exitProcess(0)
                    }
                }
            }
        } else {
            exitProcess(0)
        }
    }

    private fun updateText() {
        binding.apply {
            tv1.text = strings(R.string.color_pixel)
            tv2.text = strings(R.string.my_work)
        }
    }

    private fun openClothes(mode: String) {
        startActivity(Intent(this, ClothesSelectionActivity::class.java).apply {
            putExtra(ClothesSelectionActivity.EXTRA_MODE, mode)
        })
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onRestart() {
        super.onRestart()
        LanguageHelper.setLocale(this)
        updateText()
//        initNativeCollab()
    }

//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        if (hasFocus) {
//            startStaggeredAnimations()
//        }
//    }

//    private fun startStaggeredAnimations() {
//        // Card 1: Slide from right (no delay)
//        val slideFromRight1 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
//        binding.btnCosPlay.startAnimation(slideFromRight1)
//        binding.tv1.startAnimation(slideFromRight1)
//
//
//        // Card 2: Slide from left (200ms delay)
//        val slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_home)
//        binding.btnRandom.postDelayed({
//            binding.btnRandom.startAnimation(slideFromLeft)
//            binding.tv2.startAnimation(slideFromLeft)
//        }, 200)
//
//        // Card 3: Slide from right (400ms delay)
//        val slideFromRight2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
//        binding.btnMyCreation.postDelayed({
//            binding.btnMyCreation.startAnimation(slideFromRight2)
//            binding.tv3.startAnimation(slideFromRight2)
//        }, 400)
//    }

    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(
//            this,
//            getString(R.string.native_cl_home),
//            binding.flNativeCollab
//        )
    }

    override fun initAds() {
//        initNativeCollab()
        //Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
//        Admob.getInstance().loadNativeAll(this, getString(R.string.native_all))
    }
}
