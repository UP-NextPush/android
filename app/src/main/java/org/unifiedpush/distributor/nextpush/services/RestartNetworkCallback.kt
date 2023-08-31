package org.unifiedpush.distributor.nextpush.services

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import org.unifiedpush.distributor.nextpush.AppCompanion
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class RestartNetworkCallback(val context: Context) : ConnectivityManager.NetworkCallback() {
    private val connectivityManager: AtomicReference<ConnectivityManager?> = AtomicReference(null)

    override fun onAvailable(network: Network) {
        Log.d(TAG, "Network is CONNECTED")
        if (FailureHandler.hasFailed(orNeverStart = false)) {
            Log.d(TAG, "Available: restarting worker")
            RestartWorker.run(context, delay = 0)
        }
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            if (AppCompanion.hasInternet.getAndSet(true)) {
                Log.d(TAG, "Network Capabilities changed")
                if (FailureHandler.hasFailed(orNeverStart = false)) {
                    Log.d(TAG, "Internet Cap: restarting worker")
                    RestartWorker.run(context, delay = 0)
                } // else, it retries in max 2sec
            }
        }
    }

    override fun onLost(network: Network) {
        Log.d(TAG, "Network unavailable")
        AppCompanion.hasInternet.set(false)
    }

    fun register() {
        if (!registered.getAndSet(true)) {
            connectivityManager.get()?.let {
                Log.d(TAG, "ConnectivityManager already registered")
            } ?: run {
                Log.d(TAG, "Registering new ConnectivityManager")
                try {
                    connectivityManager.set(
                        (
                            context.getSystemService(Service.CONNECTIVITY_SERVICE)
                                as ConnectivityManager
                            ).apply {
                            registerDefaultNetworkCallback(this@RestartNetworkCallback)
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun unregister() {
        Log.d(TAG, "Unregistering ConnectivityManager")
        connectivityManager.getAndSet(null)?.unregisterNetworkCallback(this)
        registered.set(false)
        AppCompanion.hasInternet.set(true) // reset default value
    }

    companion object {
        private val registered = AtomicBoolean(false)
    }
}
