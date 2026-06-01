package com.sdk.voiceai.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Boolean
        get() {
            val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

    fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("NetworkMonitor: network available")
                trySend(true)
            }
            override fun onLost(network: Network) {
                Timber.d("NetworkMonitor: network lost")
                trySend(false)
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Timber.d("NetworkMonitor: capabilities changed connected=$connected")
                trySend(connected)
            }
            override fun onUnavailable() {
                Timber.d("NetworkMonitor: network unavailable")
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Seed with current state before registering callback to avoid gap
        trySend(isOnline)
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            Timber.d("NetworkMonitor: unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
