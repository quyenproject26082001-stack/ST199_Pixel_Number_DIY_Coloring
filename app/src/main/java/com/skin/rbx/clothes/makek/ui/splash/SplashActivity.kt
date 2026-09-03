package com.skin.rbx.clothes.makek.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkShowSplashWhenFail
import com.skin.rbx.clothes.makek.core.extensions.loadNativeCollabAds
import com.skin.rbx.clothes.makek.core.extensions.loadSplashInterAds
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.databinding.ActivitySplashBinding
import com.skin.rbx.clothes.makek.ui.intro.IntroActivity
import com.skin.rbx.clothes.makek.ui.language.LanguageActivity
import com.skin.rbx.clothes.makek.ui.home.DataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    var intentActivity: Intent? = null
    private val dataViewModel: DataViewModel by viewModels()
    var interCallBack: InterCallback? = null

    private val MIN_SPLASH_MS = 2000L  // Reduced from 3000ms to 1500ms for faster startup
    private val FINISH_LOADING_MS = 500L
    private val DATA_FALLBACK_MS = 8000L
    private val ADS_FALLBACK_MS = 12000L
    private var minTimePassed = false
    private var dataReady = false
    private var triggered = false
    private var navigated = false

    override fun setViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        if (!isTaskRoot &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.action != null &&
            intent.action.equals(Intent.ACTION_MAIN)) {
            finish(); return
        }

        intentActivity = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }
        startLoadingSplashRotation()
//        Admob.getInstance().setTimeLimitShowAds(30000)
//        Admob.getInstance().setTimeCountdownNativeCollab(20000)
//        Admob.getInstance().setOpenShowAllAds(false)
        interCallBack = object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                navigateNext()
            }
        }
        dataViewModel.saveAndReadData(this)

        lifecycleScope.launch {
            kotlinx.coroutines.delay(MIN_SPLASH_MS)
            minTimePassed = true
            tryProceed()
        }

        lifecycleScope.launch {
            delay(DATA_FALLBACK_MS)
            dataReady = true
            tryProceed()
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.dataLoadFinished.collect { finished ->
                if (finished) {
                    // Data is ready, no need to call API again
                    // (API already called in saveAndReadData if needed)
                    dataReady = true
                    tryProceed()
                }
            }
        }
    }

    private fun tryProceed() {
        if (triggered) return
        if (!minTimePassed || !dataReady) return

        triggered = true

//        loadSplashInterAds(getString(R.string.inter_splash), 30000, 2000, interCallBack)
        binding.guideLoading.finishLoading(FINISH_LOADING_MS) {
            navigateNext()
        }

        lifecycleScope.launch {
            delay(ADS_FALLBACK_MS)
            navigateNext()
        }
    }

    private fun navigateNext() {
        if (navigated || isFinishing || isDestroyed) return
        navigated = true
        startActivity(intentActivity)
        finishAffinity()
    }

    override fun viewListener() {
    }

    override fun initText() {}

    override fun initActionBar() {}

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {}

//    override fun initAds() {
//        initNativeCollab()
//    }

//    fun initNativeCollab() {
//
//        loadNativeCollabAds(R.string.native_splash, binding.flNativeCollab)
//
//
//    }

    override fun onResume() {
        super.onResume()
        startLoadingSplashRotation()
//        checkShowSplashWhenFail(interCallBack, 1000)
    }

    override fun shouldPlayBackgroundMusic(): Boolean = false

    private fun startLoadingSplashRotation() {
        if (binding.loadingSplash.animation == null) {
            binding.loadingSplash.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.rotate_loading)
            )
        }
    }
}
