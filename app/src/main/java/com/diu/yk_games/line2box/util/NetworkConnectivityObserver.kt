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

object ConnectivityObserver {
    var isConnected = false
    fun initialize(context: Context){
        CoroutineScope(Dispatchers.Main).launch{
            observer(context).collect()
        }
    }

    fun observer(context: Context): Flow<Status> {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(Status.Available) }
                    isConnected = true
                    Log.d("TAG", "onAvailable: $isConnected")
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
                    Log.d("TAG", "onLost: $isConnected")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(Status.Unavailable) }
                    isConnected = false
                    Log.d("TAG", "onUnavailable: $isConnected")
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }//.distinctUntilChanged()
    }
    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}