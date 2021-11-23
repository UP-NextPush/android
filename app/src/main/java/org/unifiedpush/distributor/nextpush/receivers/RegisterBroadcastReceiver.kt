package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.distributor.*
import org.unifiedpush.distributor.nextpush.api.ApiUtils

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
                ApiUtils().createApp(context!!.applicationContext, application, connectorToken) {
                    sendEndpoint(context.applicationContext, connectorToken)
                }
            }
            ACTION_UNREGISTER ->{
                Log.i(TAG,"UNREGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: return
                if (application.isBlank()) {
                    return
                }
                sendUnregistered(context!!.applicationContext, connectorToken)
                val db = MessagingDatabase(context.applicationContext)
                val appToken = db.getAppToken(connectorToken)
                db.unregisterApp(connectorToken)
                db.close()
                ApiUtils().deleteApp(context.applicationContext, appToken) {
                    Log.d(TAG,"Unregistration is finished")
                }
            }
        }
    }
}