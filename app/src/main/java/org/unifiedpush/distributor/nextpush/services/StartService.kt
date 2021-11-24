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
import com.nextcloud.android.sso.ui.UiExceptionManager

import org.unifiedpush.distributor.nextpush.api.ApiUtils
import org.unifiedpush.distributor.nextpush.account.ssoAccount

import android.net.Network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import java.lang.Exception


private const val TAG = "StartService"
var isServiceStarted = false

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

    private var wakeLock: PowerManager.WakeLock? = null
    private var api = ApiUtils()

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
        registerNetworkCallback()
        startService()
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onDestroy() {
        api.destroy()
        super.onDestroy()
    }

    private fun startService() {
        if (isServiceStarted) return
        isServiceStarted = true

        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(this)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            UiExceptionManager.showDialogForException(this, e)
        } catch (e: NoCurrentAccountSelectedException) {
            return
        }

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                acquire()
            }
        }

        api.sync(this)
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network is CONNECTED")
                    startService()
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

