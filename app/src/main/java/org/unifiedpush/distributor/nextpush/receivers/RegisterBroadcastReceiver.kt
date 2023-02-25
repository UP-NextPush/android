package org.unifiedpush.distributor.nextpush.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.AccountUtils.isConnected
import org.unifiedpush.distributor.nextpush.distributor.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_NEW
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_NOK
import org.unifiedpush.distributor.nextpush.distributor.Distributor.TOKEN_REGISTERED_OK
import org.unifiedpush.distributor.nextpush.distributor.Distributor.checkToken
import org.unifiedpush.distributor.nextpush.distributor.Distributor.createApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.getDb
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendEndpoint
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendRegistrationFailed
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * THIS SERVICE IS USED BY OTHER APPS TO REGISTER
 */

private val createQueue = emptyList<String>().toMutableList()
private val delQueue = emptyList<String>().toMutableList()

class RegisterBroadcastReceiver : BroadcastReceiver() {

    private val WAKE_LOCK_TAG = "NextPush:RegisterBroadcastReceiver:lock"

    override fun onReceive(context: Context, intent: Intent?) {
        wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
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
                when (checkToken(context.applicationContext, connectorToken, application)) {
                    TOKEN_REGISTERED_OK -> sendEndpoint(context.applicationContext, connectorToken)
                    TOKEN_NOK -> sendRegistrationFailed(
                        context.applicationContext,
                        application,
                        connectorToken
                    )
                    TOKEN_NEW -> {
                        if (!isConnected(context.applicationContext, showDialog = false)) {
                            sendRegistrationFailed(
                                context.applicationContext,
                                application,
                                connectorToken,
                                message = "NextPush is not connected"
                            )
                            return
                        }
                        if (connectorToken !in createQueue) {
                            createQueue.add(connectorToken)
                            delayRemove(createQueue, connectorToken)
                            createApp(
                                context.applicationContext,
                                application,
                                connectorToken
                            ) {
                                sendEndpoint(context.applicationContext, connectorToken)
                                createQueue.remove(connectorToken)
                            }
                        } else {
                            Log.d(TAG, "Already registering this token")
                        }
                    }
                }
            }
            ACTION_UNREGISTER -> {
                Log.i(TAG, "UNREGISTER")
                val connectorToken = intent.getStringExtra(EXTRA_TOKEN) ?: ""
                getDb(context.applicationContext).getPackageName(connectorToken) ?: return

                if (connectorToken !in delQueue) {
                    delQueue.add(connectorToken)
                    delayRemove(delQueue, connectorToken)
                    try {
                        deleteApp(context.applicationContext, connectorToken) {
                            Log.d(TAG, "Unregistration is finished")
                            delQueue.remove(connectorToken)
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

    private fun delayRemove(list: MutableList<String>, token: String) {
        Timer().schedule(1_000L /* 1sec */) {
            list.remove(token)
        }
    }

    companion object {
        private var wakeLock: PowerManager.WakeLock? = null
    }
}
