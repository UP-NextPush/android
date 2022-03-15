package org.unifiedpush.distributor.nextpush.distributor

import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.AccountUtils.getUrl

/**
 * These functions are used to send messages to other apps
 */

private const val TAG = "DistributorUtils"

object DistributorUtils {

    const val TOKEN_NEW = "token_new"
    const val TOKEN_REGISTERED_OK = "token_registered_ok"
    const val TOKEN_NOK = "token_nok"

    private var db: MessagingDatabase? = null

    fun getDb(context: Context): MessagingDatabase {
        if (db == null) {
            db = MessagingDatabase(context)
        }
        return db!!
    }

    fun sendMessage(context: Context, appToken: String, message: ByteArray) {
        val db = getDb(context)
        val connectorToken = db.getConnectorToken(appToken)
        val application = getApp(context, connectorToken)
        if (application.isNullOrBlank()) {
            return
        }
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

    fun sendUnregistered(context: Context, connectorToken: String) {
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
        return if (app.isBlank()) {
            Log.w(TAG, "No app found for $connectorToken")
            null
        } else {
            app
        }
    }

    private fun getEndpoint(context: Context, connectorToken: String): String {
        val db = getDb(context)
        val appToken = db.getAppToken(connectorToken)
        return "${getUrl(context)}/push/$appToken"
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
}