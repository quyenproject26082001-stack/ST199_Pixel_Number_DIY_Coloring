package com.skin.rbx.clothes.makek.core.utils

import android.content.Context
import com.lvt.ads.util.AdsApplication
import com.lvt.ads.util.AppOpenManager
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.extensions.initNetworkMonitor
import com.skin.rbx.clothes.makek.ui.splash.SplashActivity
import kotlin.jvm.java

class App : AdsApplication() {


    override fun onCreate() {
        super.onCreate()
        initNetworkMonitor()
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
    }

    override fun enableAdsResume(): Boolean {
        return false
    }

    override fun getListTestDeviceId(): MutableList<String>? {
        return null
    }

    override fun getResumeAdId(): String {
        return getString(R.string.next)

        //getString(R.string.open_resume)
    }

    override fun buildDebug(): Boolean {
        return true
    }

}
