package org.unifiedpush.distributor.nextpush

import android.content.Context
import org.unifiedpush.distributor.nextpush.Database.Companion.getDb
import org.unifiedpush.distributor.nextpush.api.Api
import org.unifiedpush.distributor.nextpush.utils.FromPushNotification
import java.util.UUID

object LocalNotification {
    fun createChannel(context: Context, title: String, block: () -> Unit) {
        Api(context).apiCreateApp(context.getString(R.string.local_notif_title).format(title)) { nextpushToken ->
            nextpushToken?.let {
                getDb(context).registerApp(context.packageName, UUID.randomUUID().toString(), it, title)
            }
            block()
        }
    }

    fun showNotification(context: Context, connectorToken: String, content: String) {
        val title = getDb(context).getNotificationTitle(connectorToken) ?: context.getString(R.string.app_name)
        FromPushNotification(context, title, content).show()
    }
}
