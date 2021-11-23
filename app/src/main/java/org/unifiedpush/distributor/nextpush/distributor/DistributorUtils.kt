package org.unifiedpush.distributor.nextpush.distributor

import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.distributor.nextpush.account.getUrl

/**
 * These functions are used to send messages to other apps
 */

private const val TAG = "DistributorUtils"

fun sendMessage(context: Context, appToken: String, message: String) {
    val db = MessagingDatabase(context)
    val connectorToken = db.getConnectorToken(appToken)
    val application = getApp(context, connectorToken, db)
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

fun getApp(context: Context,
           connectorToken: String,
           db: MessagingDatabase = MessagingDatabase(context)
): String?{
    val app = db.getPackageName(connectorToken)
    db.close()
    return if (app.isBlank()) {
        Log.w(TAG, "No app found for $connectorToken")
        null
    } else {
        app
    }
}

fun getEndpoint(context: Context, connectorToken: String): String {
    val db = MessagingDatabase(context)
    val appToken = db.getAppToken(connectorToken)
    db.close()
    return "${getUrl(context)}/push/$appToken"
}