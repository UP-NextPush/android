package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.distributor.*
import kotlin.concurrent.thread

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

class RegisterBroadcastReceiver : BroadcastReceiver() {

    private fun unregisterApp(db: MessagingDatabase, application: String, token: String) {
        Log.i("RegisterService","Unregistering $application token: $token")
        db.unregisterApp(token)
    }

    private fun registerApp(context: Context?, db: MessagingDatabase, application: String, token: String) {
        if (application.isBlank()) {
            Log.w("RegisterService","Trying to register an app without packageName")
            return
        }
        Log.i("RegisterService","registering $application token: $token")
        // The app is registered with the same token : we re-register it
        // the client may need its endpoint again
        if (db.isRegistered(application, token)) {
            Log.i("RegisterService","$application already registered")
            return
        }

        db.registerApp(application, token)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            ACTION_REGISTER ->{
                Log.i("Register","REGISTER")
                val token = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                thread(start = true) {
                    val db = MessagingDatabase(context!!)
                    registerApp(context, db, application, token)
                    db.close()
                    Log.i("RegisterService","Registration is finished")
                }.join()
                sendEndpoint(context!!, token, getEndpoint(context, token))
            }
            ACTION_UNREGISTER ->{
                Log.i("Register","UNREGISTER")
                val token = intent.getStringExtra(EXTRA_TOKEN)?: ""
                val application = intent.getStringExtra(EXTRA_APPLICATION)?: ""
                thread(start = true) {
                    val db = MessagingDatabase(context!!)
                    unregisterApp(db,application, token)
                    db.close()
                    Log.i("RegisterService","Unregistration is finished")
                }
                sendUnregistered(context!!, token)
            }
        }
    }
}