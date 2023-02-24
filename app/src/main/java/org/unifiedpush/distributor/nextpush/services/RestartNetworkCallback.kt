package org.unifiedpush.distributor.nextpush.services

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception

class RestartNetworkCallback(val context: Context) : ConnectivityManager.NetworkCallback() {
    var connectivityManager: ConnectivityManager? = null

    override fun onAvailable(network: Network) {
        Log.d(TAG, "Network is CONNECTED")
        if (StartService.hasFailed(twice = true, orNeverStart = false)) {
            Log.d(TAG, "networkCallback: restarting worker")
            RestartWorker.start(context, delay = 0)
        }
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        Log.d(TAG, "Network Capabilities changed")
        if (StartService.hasFailed(twice = true, orNeverStart = false)) {
            Log.d(TAG, "networkCallback: restarting worker")
            RestartWorker.start(context, delay = 0)
        } // else, it retries in max 2sec
    }

    fun register() {
        Log.d(TAG, "Registering Network Callback")
        connectivityManager ?: run {
            try {
                connectivityManager = (
                    context.getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ).apply {
                    registerDefaultNetworkCallback(this@RestartNetworkCallback)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unregister() {
        connectivityManager?.unregisterNetworkCallback(this)
        connectivityManager = null
    }
}
