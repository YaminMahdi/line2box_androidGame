package com.diu.yk_games.line2box.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object ConnectivityObserver {
    var isConnected = false
    private var isCallbackRegistered = AtomicBoolean(false)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            observer(context).collect()
        }
    }

    fun observer(context: Context): Flow<Status> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (isCallbackRegistered.compareAndSet(false, true)) {
            callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(Status.Available) }
                    isConnected = true
                    Log.d("TAG", "onAvailable: true")
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch { send(Status.Losing) }
                    Log.d("TAG", "onLosing: $isConnected")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(Status.Lost) }
                    isConnected = false
                    Log.d("TAG", "onLost: false")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(Status.Unavailable) }
                    isConnected = false
                    Log.d("TAG", "onUnavailable: false")
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback!!)
        }
        awaitClose {
            if (isCallbackRegistered.compareAndSet(true, false)) {
                callback?.let {
                    connectivityManager.unregisterNetworkCallback(it)
                    callback = null
                }
            }
        }
    }

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}