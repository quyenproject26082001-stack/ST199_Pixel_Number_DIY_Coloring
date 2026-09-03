package com.skin.rbx.clothes.makek.core.extensions

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.helper.InternetHelper
import com.skin.rbx.clothes.makek.core.utils.DataLocal
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.dialog.YesNoDialog


fun Context.initNetworkMonitor() {
    val appContext = applicationContext
    val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    DataLocal.isConnectInternet.value = InternetHelper.isInternetAvailable(appContext)

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            DataLocal.isConnectInternet.postValue(
                InternetHelper.isInternetAvailable(appContext)
            )
        }

        override fun onLost(network: Network) {
            DataLocal.isConnectInternet.postValue(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            DataLocal.isConnectInternet.postValue(
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            )
        }
    }
    connectivityManager.registerDefaultNetworkCallback(networkCallback)
}


fun Activity.checkInternet(
    onFailureConfirmed: (() -> Unit)? = null,
    action: () -> Unit,
) {
    InternetHelper.checkInternet(this) { result ->
        if (result == HandleState.SUCCESS) {
            action.invoke()
        } else {
            val dialog = YesNoDialog(
                this,
                R.string.no_internet,
                R.string.please_check_your_internet,
                isError = true
            )
            dialog.show()
            dialog.onYesClick = {
                dialog.dismiss()
                hideNavigation()
                onFailureConfirmed?.invoke()
            }
        }
    }
}
