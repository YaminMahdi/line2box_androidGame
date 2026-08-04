package com.diu.yk_games.line2box.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ConnectivityObserver {
    val status: StateFlow<Status>
        field = MutableStateFlow(Status.Unknown)

    val isConnected: Boolean
        get() = status.value == Status.Available

    private var connectivityManager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    fun initialize(context: Context) {
        if (callback != null) return // already initialized, avoid double registration

        val cm = ContextCompat.getSystemService(
            context.applicationContext,
            ConnectivityManager::class.java
        ) ?: return
        connectivityManager = cm

        status.value = currentStatus(cm)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                update(cm.getNetworkCapabilities(network))
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                status.value = Status.Losing
            }

            override fun onLost(network: Network) {
                status.value = Status.Lost
            }

            override fun onUnavailable() {
                status.value = Status.Unavailable
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                update(networkCapabilities)
            }
        }
        callback = networkCallback
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun teardown() {
        callback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        callback = null
        connectivityManager = null
        status.value = Status.Unknown
    }

    private fun update(caps: NetworkCapabilities?) {
        val hasInternet =
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        status.value = if (hasInternet) Status.Available else Status.Lost
    }

    private fun currentStatus(cm: ConnectivityManager): Status {
        val network = cm.activeNetwork ?: return Status.Lost
        val caps = cm.getNetworkCapabilities(network) ?: return Status.Lost
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (hasInternet) Status.Available else Status.Lost
    }

    enum class Status { Unknown, Available, Losing, Lost, Unavailable }
}