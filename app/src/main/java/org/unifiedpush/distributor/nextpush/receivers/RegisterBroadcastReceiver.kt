package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.isConnected

import org.unifiedpush.distributor.nextpush.distributor.*
import org.unifiedpush.distributor.nextpush.api.createQueue
import org.unifiedpush.distributor.nextpush.api.delQueue
import org.unifiedpush.distributor.nextpush.api.apiCreateApp
import org.unifiedpush.distributor.nextpush.api.apiDeleteApp
import org.unifiedpush.distributor.nextpush.services.wakeLock
import java.lang.Exception

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

private const val TAG = "RegisterBroadcastReceiver"

class RegisterBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        wakeLock?.acquire(10000L /*10 secs*/)
        when (intent!!.action) {
            ACTION_REGISTER ->{
                Log.i(TAG,"REGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                if (application.isBlank()) {
                    Log.w(TAG,"Trying to register an app without packageName")
                    return
                }
                if (!isConnected(context!!, showDialog = false)) {
                    sendRegistrationFailed(
                        context,
                        application,
                        connectorToken,
                        message = "NextPush is not connected"
                    )
                    return
                }
                if (!isTokenOk(context, connectorToken, application)) {
                    sendRegistrationFailed(
                        context,
                        application,
                        connectorToken
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
            ACTION_UNREGISTER ->{
                Log.i(TAG,"UNREGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = getDb(context!!).getPackageName(connectorToken)
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