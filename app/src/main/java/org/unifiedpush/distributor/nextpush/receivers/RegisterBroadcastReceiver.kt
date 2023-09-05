package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import org.unifiedpush.distributor.nextpush.Database.Companion.getDb
import org.unifiedpush.distributor.nextpush.account.Account.isConnected
import org.unifiedpush.distributor.nextpush.distributor.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_NEW
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_NOK
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_REGISTERED_OK
import org.unifiedpush.distributor.nextpush.distributor.Distributor.checkToken
import org.unifiedpush.distributor.nextpush.distributor.Distributor.createApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendEndpoint
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendRegistrationFailed
import org.unifiedpush.distributor.nextpush.utils.TAG
import org.unifiedpush.distributor.nextpush.utils.getApplicationName
import java.lang.Exception
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

private const val WAKE_LOCK_TAG = "NextPush:RegisterBroadcastReceiver:lock"

class RegisterBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(rContext: Context, intent: Intent?) {
        val context = rContext.applicationContext
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }
        wakeLock?.acquire(30000L /*30 secs*/)
        when (intent?.action) {
            ACTION_REGISTER -> {
                Log.i(TAG, "REGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN) ?: run {
                    Log.w(TAG, "Trying to register an app without connector token")
                    return
                }
                val application = intent.getStringExtra(EXTRA_APPLICATION) ?: run {
                    Log.w(TAG, "Trying to register an app without packageName")
                    return
                }
                if (!createQueue.containsTokenElseAdd(connectorToken)) {
                    when (checkToken(context, connectorToken, application)) {
                        TOKEN_REGISTERED_OK -> {
                            sendEndpoint(context, connectorToken)
                            createQueue.removeToken(connectorToken)
                        }
                        TOKEN_NOK -> {
                            sendRegistrationFailed(
                                context,
                                application,
                                connectorToken
                            )
                            createQueue.removeToken(connectorToken)
                        }
                        TOKEN_NEW -> {
                            val appName = context.getApplicationName(application) ?: application
                            if (!isConnected(context)) {
                                sendRegistrationFailed(
                                    context,
                                    application,
                                    connectorToken,
                                    message = "NextPush is not connected"
                                )
                                Toast.makeText(
                                    context,
                                    "Cannot register $appName, NextPush is not connected.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                            createApp(
                                context,
                                application,
                                connectorToken
                            ) {
                                sendEndpoint(context, connectorToken)
                                createQueue.removeToken(connectorToken)
                                Toast.makeText(
                                    context,
                                    "$appName registered.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Already registering this token")
                }
            }
            ACTION_UNREGISTER -> {
                Log.i(TAG, "UNREGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN) ?: ""
                getDb(context).getPackageName(connectorToken) ?: return

                if (!delQueue.containsTokenElseAdd(connectorToken)) {
                    try {
                        deleteApp(context, connectorToken) {
                            Log.d(TAG, "Unregistration is finished")
                            delQueue.removeToken(connectorToken)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not delete app")
                    }
                } else {
                    Log.d(TAG, "Already deleting this token")
                }
            }
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun MutableList<String>.containsTokenElseAdd(connectorToken: String): Boolean {
        return synchronized(this) {
            if (connectorToken !in this) {
                Log.d(TAG, "Token: $this")
                this.add(connectorToken)
                delayRemove(this, connectorToken)
                false
            } else {
                true
            }
        }
    }

    private fun MutableList<String>.removeToken(connectorToken: String) {
        synchronized(this) {
            this.remove(connectorToken)
        }
    }

    private fun delayRemove(list: MutableList<String>, token: String) {
        Timer().schedule(1_000L /* 1sec */) {
            list.removeToken(token)
        }
    }

    companion object {
        private val createQueue = emptyList<String>().toMutableList()
        private val delQueue = emptyList<String>().toMutableList()
    }
}
