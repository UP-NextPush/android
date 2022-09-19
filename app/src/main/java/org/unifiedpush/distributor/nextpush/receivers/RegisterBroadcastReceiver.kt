package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.AccountUtils.isConnected
import org.unifiedpush.distributor.nextpush.api.ApiUtils.apiCreateApp
import org.unifiedpush.distributor.nextpush.api.ApiUtils.apiDeleteApp
import org.unifiedpush.distributor.nextpush.api.ApiUtils.createQueue
import org.unifiedpush.distributor.nextpush.api.ApiUtils.delQueue

import org.unifiedpush.distributor.nextpush.distributor.*
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.TOKEN_NEW
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.TOKEN_NOK
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.TOKEN_REGISTERED_OK
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.checkToken
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.getDb
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.sendEndpoint
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.sendRegistrationFailed
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.sendUnregistered
import java.lang.Exception

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

private const val TAG = "RegisterBroadcastReceiver"

class RegisterBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val WAKE_LOCK_TAG = "NextPush:RegisterBroadcastReceiver:lock"
        private var wakeLock : PowerManager.WakeLock? = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }
        wakeLock?.acquire(30000L /*30 secs*/)
        when (intent?.action) {
            ACTION_REGISTER ->{
                Log.i(TAG,"REGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                if (application.isBlank()) {
                    Log.w(TAG,"Trying to register an app without packageName")
                    return
                }
                when (checkToken(context, connectorToken, application)) {
                    TOKEN_REGISTERED_OK -> sendEndpoint(context.applicationContext, connectorToken)
                    TOKEN_NOK -> sendRegistrationFailed(
                        context,
                        application,
                        connectorToken
                    )
                    TOKEN_NEW -> {
                        if (!isConnected(context, showDialog = false)) {
                            sendRegistrationFailed(
                                context,
                                application,
                                connectorToken,
                                message = "NextPush is not connected"
                            )
                            return
                        }
                        if (connectorToken !in createQueue) {
                            createQueue.add(connectorToken)
                            apiCreateApp(
                                context.applicationContext,
                                application,
                                connectorToken
                            ) {
                                sendEndpoint(context.applicationContext, connectorToken)
                            }
                        } else {
                            Log.d(TAG, "Already registering $connectorToken")
                        }
                    }
                }
            }
            ACTION_UNREGISTER ->{
                Log.i(TAG,"UNREGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = getDb(context).getPackageName(connectorToken)
                if (application.isBlank()) {
                    return
                }
                if (connectorToken !in delQueue) {
                    delQueue.add(connectorToken)
                    sendUnregistered(context.applicationContext, connectorToken)
                    try {
                        apiDeleteApp(context.applicationContext, connectorToken) {
                            val db = getDb(context.applicationContext)
                            db.unregisterApp(connectorToken)
                            Log.d(TAG, "Unregistration is finished")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not delete app")
                    }
                } else {
                    Log.d(TAG, "Already deleting $connectorToken")
                }
            }
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}