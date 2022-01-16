package org.unifiedpush.distributor.nextpush.distributor

import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.getUrl

/**
 * These functions are used to send messages to other apps
 */

private const val TAG = "DistributorUtils"

private var db : MessagingDatabase? = null

fun getDb(context: Context): MessagingDatabase {
    if (db == null) {
        db = MessagingDatabase(context)
    }
    return db!!
}

fun sendMessage(context: Context, appToken: String, message: String) {
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
    broadcastIntent.putExtra(EXTRA_MESSAGE, message)
    context.sendBroadcast(broadcastIntent)
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
    if (application.isNullOrBlank()) {
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

fun getApp(context: Context, connectorToken: String): String?{
    val db = getDb(context)
    val app = db.getPackageName(connectorToken)
    return if (app.isBlank()) {
        Log.w(TAG, "No app found for $connectorToken")
        null
    } else {
        app
    }
}

fun getEndpoint(context: Context, connectorToken: String): String {
    val db = getDb(context)
    val appToken = db.getAppToken(connectorToken)
    return "${getUrl(context)}/push/$appToken"
}

fun isTokenOk(context: Context, connectorToken: String, app: String): Boolean {
    val db = getDb(context)
    if (connectorToken !in db.listTokens()) {
        return true
    }
    return db.getPackageName(connectorToken) == app
}
