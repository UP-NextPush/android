package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.distributor.*
import org.unifiedpush.distributor.nextpush.api.ApiUtils
import org.unifiedpush.distributor.nextpush.api.createQueue
import org.unifiedpush.distributor.nextpush.api.delQueue

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

private const val TAG = "RegisterBroadcastReceiver"

class RegisterBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            ACTION_REGISTER ->{
                Log.i(TAG,"REGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                if (application.isBlank()) {
                    Log.w(TAG,"Trying to register an app without packageName")
                    return
                }
                if (connectorToken !in createQueue) {
                    createQueue.add(connectorToken)
                    ApiUtils().createApp(
                        context!!.applicationContext,
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
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: return
                if (application.isBlank()) {
                    return
                }
                if (connectorToken !in delQueue) {
                    delQueue.add(connectorToken)
                    sendUnregistered(context!!.applicationContext, connectorToken)
                    ApiUtils().deleteApp(context.applicationContext, connectorToken) {
                        val db = getDb(context.applicationContext)
                        db.unregisterApp(connectorToken)
                        Log.d(TAG, "Unregistration is finished")
                    }
                } else {
                    Log.d(TAG, "Already deleting $connectorToken")
                }
            }
        }
    }
}