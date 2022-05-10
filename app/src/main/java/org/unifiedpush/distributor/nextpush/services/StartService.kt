package org.unifiedpush.distributor.nextpush.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper

import org.unifiedpush.distributor.nextpush.account.AccountUtils.ssoAccount
import org.unifiedpush.distributor.nextpush.account.AccountUtils.nextcloudAppNotInstalledDialog

import android.net.Network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.NetworkCapabilities
import org.unifiedpush.distributor.nextpush.api.ApiUtils.apiDestroy
import org.unifiedpush.distributor.nextpush.api.ApiUtils.apiSync
import org.unifiedpush.distributor.nextpush.services.NotificationUtils.createForegroundNotification
import java.lang.Exception

private const val TAG = "StartService"

class StartService: Service(){

    companion object {
        const val WAKE_LOCK_TAG = "NextPush:StartService:lock"
        const val SERVICE_STOPPED_ACTION = "org.unifiedpush.distributor.nextpush.services.STOPPED"
        var isServiceStarted = false
        var nFails = 0
        var wakeLock: PowerManager.WakeLock? = null
        private var isCallbackRegistered = false

        fun startListener(context: Context){
            if (isServiceStarted && nFails == 0) return
            Log.d(TAG, "Starting the Listener")
            Log.d(TAG, "Service is started: $isServiceStarted")
            Log.d(TAG, "nFails: $nFails")
            val serviceIntent = Intent(context, StartService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            }else{
                context.startService(serviceIntent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate(){
        super.onCreate()
        Log.i(TAG,"StartService created")
        val notification = createForegroundNotification(this)
        startForeground(NOTIFICATION_ID_FOREGROUND, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (!isCallbackRegistered) {
            isCallbackRegistered = true
            registerNetworkCallback()
        }
        startService()
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroyed")
        if (isServiceStarted) {
            apiDestroy()
            RestartWorker.start(this, delay = 0)
        } else {
            stopService()
        }
    }

    private fun stopService() {
        Log.d(TAG, "Stopping Service")
        isServiceStarted = false
        nFails = 0
        apiDestroy()
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        isCallbackRegistered = false
        wakeLock?.let {
            while (it.isHeld) {
                it.release()
            }
        }
        val i = Intent(SERVICE_STOPPED_ACTION)
        sendBroadcast(i)
        stopSelf()
    }

    private fun startService() {
        if (isServiceStarted && nFails == 0) return
        isServiceStarted = true

        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(this)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            nextcloudAppNotInstalledDialog(this)
        } catch (e: NoCurrentAccountSelectedException) {
            return
        }

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire(10000L /*10 secs*/)
            }
        }

        apiSync(this)
    }

    private var connectivityManager = null as ConnectivityManager?

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network is CONNECTED")
            if (nFails > 1) {
                RestartWorker.start(this@StartService, delay = 0)
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.d(TAG, "Network Capabilities changed")
            if (nFails > 1) {
                RestartWorker.start(this@StartService, delay = 0)
            } // else, it retries in max 2sec
        }
    }

    private fun registerNetworkCallback() {
        Log.d(TAG, "Registering Network Callback")
        try {
            connectivityManager = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager!!.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

