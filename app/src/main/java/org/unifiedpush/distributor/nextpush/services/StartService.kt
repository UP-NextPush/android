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

import org.unifiedpush.distributor.nextpush.account.ssoAccount
import org.unifiedpush.distributor.nextpush.account.nextcloudAppNotInstalledDialog

import android.net.Network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import org.unifiedpush.distributor.nextpush.api.apiDestroy
import org.unifiedpush.distributor.nextpush.api.apiSync
import java.lang.Exception

private const val TAG = "StartService"
const val WAKE_LOCK_TAG = "NextPush:StartService:lock"
var isServiceStarted = false
var nFails = 0
var wakeLock: PowerManager.WakeLock? = null

fun startListener(context: Context){
    Log.d(TAG, "Starting the Listener")
    val serviceIntent = Intent(context, StartService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    }else{
        context.startService(serviceIntent)
    }
}

class StartService: Service(){

    private var isCallbackRegistered = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate(){
        super.onCreate()
        Log.i(TAG,"Starting")
        val notification = createForegroundNotification(this)
        startForeground(NOTIF_ID_FOREGROUND, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            startListener(this)
        } else {
            stopService()
        }
    }

    private fun stopService() {
        Log.d(TAG, "Stopping Service")
        apiDestroy()
        wakeLock?.let {
            while (it.isHeld) {
                it.release()
            }
        }
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

    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network is CONNECTED")
                    startListener(this@StartService)
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network is DISCONNECTED")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

