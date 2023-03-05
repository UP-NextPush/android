package org.unifiedpush.distributor.nextpush.distributor

import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.api.Api
import org.unifiedpush.distributor.nextpush.api.provider.ApiProvider.Companion.mApiEndpoint
import org.unifiedpush.distributor.nextpush.utils.TAG

/**
 * These functions are used to send messages to other apps
 */

object Distributor {

    const val TOKEN_NEW = "token_new"
    const val TOKEN_REGISTERED_OK = "token_registered_ok"
    const val TOKEN_NOK = "token_nok"

    private lateinit var db: ConnectionsDatabase

    fun getDb(context: Context): ConnectionsDatabase {
        if (!this::db.isInitialized) {
            db = ConnectionsDatabase(context)
        }
        return db
    }

    fun sendMessage(context: Context, appToken: String, message: ByteArray) {
        val db = getDb(context)
        val connectorToken = db.getConnectorToken(appToken) ?: return
        val application = getApp(context, connectorToken)

        val broadcastIntent = Intent()
        broadcastIntent.`package` = application
        broadcastIntent.action = ACTION_MESSAGE
        broadcastIntent.putExtra(EXTRA_TOKEN, connectorToken)
        broadcastIntent.putExtra(EXTRA_MESSAGE, String(message))
        broadcastIntent.putExtra(EXTRA_BYTES_MESSAGE, message)
        context.sendBroadcast(broadcastIntent)
        Log.d(TAG, "Message forwarded")
    }

    fun sendEndpoint(context: Context, connectorToken: String) {
        val application = getApp(context, connectorToken)
        if (application.isNullOrBlank()) {
            return
        }
        val broadcastIntent = Intent()
        broadcastIntent.`package` = application
        broadcastIntent.action = ACTION_NEW_ENDPOINT
        broadcastIntent.putExtra(EXTRA_TOKEN, connectorToken)
        broadcastIntent.putExtra(EXTRA_ENDPOINT, getEndpoint(context, connectorToken))
        context.sendBroadcast(broadcastIntent)
    }

    fun sendRegistrationFailed(
        context: Context,
        application: String,
        connectorToken: String,
        message: String = ""
    ) {
        application.ifBlank {
            return
        }
        val broadcastIntent = Intent()
        broadcastIntent.`package` = application
        broadcastIntent.action = ACTION_REGISTRATION_FAILED
        broadcastIntent.putExtra(EXTRA_TOKEN, connectorToken)
        broadcastIntent.putExtra(EXTRA_MESSAGE, message)
        context.sendBroadcast(broadcastIntent)
    }

    private fun sendUnregistered(context: Context, connectorToken: String) {
        val application = getApp(context, connectorToken)
        if (application.isNullOrBlank()) {
            return
        }
        val broadcastIntent = Intent()
        broadcastIntent.`package` = application
        broadcastIntent.action = ACTION_UNREGISTERED
        broadcastIntent.putExtra(EXTRA_TOKEN, connectorToken)
        context.sendBroadcast(broadcastIntent)
    }

    private fun getApp(context: Context, connectorToken: String): String? {
        val db = getDb(context)
        val app = db.getPackageName(connectorToken)
        return app
    }

    private fun getEndpoint(context: Context, connectorToken: String): String {
        val db = getDb(context)
        val appToken = db.getAppToken(connectorToken)
        return "${getAccount(context)?.url}$mApiEndpoint/push/$appToken"
    }

    fun checkToken(context: Context, connectorToken: String, app: String): String {
        val db = getDb(context)
        if (connectorToken !in db.listTokens()) {
            return TOKEN_NEW
        }
        if (db.isRegistered(app, connectorToken)) {
            return TOKEN_REGISTERED_OK
        }
        return TOKEN_NOK
    }

    fun deleteDevice(context: Context, block: () -> Unit = {}) {
        val db = getDb(context)
        db.listTokens().forEach {
            sendUnregistered(context, it)
            db.unregisterApp(it)
        }
        Api(context).apiDeleteDevice(block)
    }

    fun createApp(context: Context, appName: String, connectorToken: String, block: () -> Unit) {
        Api(context).apiCreateApp(appName) { nextpushToken ->
            nextpushToken?.let {
                getDb(context).registerApp(appName, connectorToken, it)
            }
            block()
        }
    }

    fun deleteApp(context: Context, connectorToken: String, block: () -> Unit) {
        sendUnregistered(context, connectorToken)
        val db = getDb(context)
        db.getAppToken(
            connectorToken
        )?.let { nextpushToken ->
            Api(context).apiDeleteApp(nextpushToken) {
                db.unregisterApp(connectorToken)
                block()
            }
        }
    }

    fun deleteAppFromSSE(context: Context, appToken: String) {
        val db = getDb(context)
        db.getConnectorToken(appToken)?.let { connectorToken ->
            sendUnregistered(context, connectorToken)
            db.unregisterApp(connectorToken)
        }
    }
}
