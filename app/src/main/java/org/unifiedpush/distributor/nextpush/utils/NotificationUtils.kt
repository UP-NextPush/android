package org.unifiedpush.distributor.nextpush.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.activities.MainActivity

const val NOTIFICATION_ID_FOREGROUND = 51115
const val NOTIFICATION_ID_WARNING = 51215
const val NOTIFICATION_ID_NO_START = 51315
const val NOTIFICATION_ID_LOW_KEEPALIVE = 51315
const val NOTIFICATION_ID_NO_PING = 51515

private data class ChannelData(
    val id: String,
    val name: String,
    val importance: Int,
    val description: String
)

private data class NotificationData(
    val text: String,
    val ticker: String,
    val priority: Int,
    val ongoing: Boolean,
    val channelId: String
)
object NotificationUtils {

    private var warningShown = false

    private fun createNotificationChannel(context: Context, channelData: ChannelData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelData.id,
                channelData.name,
                channelData.importance
            ).let {
                it.description = channelData.description
                it
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(context: Context, notificationData: NotificationData, intent: PendingIntent?, bigText: Boolean = false): Notification {
        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(
                    context,
                    notificationData.channelId
                )
            } else {
                Notification.Builder(context)
            }

        return builder
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(notificationData.text)
            .setSmallIcon(R.drawable.ic_logo)
            .setTicker(notificationData.ticker)
            .setOngoing(notificationData.ongoing)
            .setPriority(notificationData.priority) // for under android 26 compatibility
            .apply {
                intent?.let {
                    setContentIntent(intent)
                }
                if (bigText) {
                    style = Notification.BigTextStyle()
                        .bigText(notificationData.text)
                }
            }
            .build()
    }

    private fun createIntentToMain(context: Context): PendingIntent {
        val notificationIntent = Intent(context, MainActivity::class.java)

        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun show(context: Context, id: Int, notification: Notification) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(id, notification)
            }
        }
    }

    fun createForegroundNotification(context: Context): Notification {
        val notificationChannelId = "${context.getString(R.string.app_name)}.Listener"

        createNotificationChannel(
            context,
            ChannelData(
                notificationChannelId,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW,
                context.getString(R.string.foreground_notif_description)
            )
        )

        val intent = createIntentToMain(context)

        return createNotification(
            context,
            NotificationData(
                context.getString(R.string.foreground_notif_description),
                context.getString(R.string.foreground_notif_ticker),
                Notification.PRIORITY_LOW,
                true,
                notificationChannelId
            ),
            intent
        )
    }

    private fun createWarningNotificationChannel(context: Context): String {
        val notificationChannelId = "${context.getString(R.string.app_name)}.Warning"

        createNotificationChannel(
            context,
            ChannelData(
                notificationChannelId,
                "Warning",
                NotificationManager.IMPORTANCE_HIGH,
                context.getString(R.string.warning_notif_description)
            )
        )
        return notificationChannelId
    }

    fun showWarningNotification(context: Context) {
        if (warningShown) {
            return
        }
        val notificationChannelId = createWarningNotificationChannel(context)

        val intent = createIntentToMain(context)

        val notification = createNotification(
            context,
            NotificationData(
                context.getString(R.string.warning_notif_content),
                context.getString(R.string.warning_notif_ticker),
                Notification.PRIORITY_HIGH,
                true,
                notificationChannelId
            ),
            intent
        )

        show(context, NOTIFICATION_ID_WARNING, notification)
        warningShown = true
    }

    fun deleteWarningNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_WARNING)
        warningShown = false
    }

    fun showStartErrorNotification(context: Context) {
        val notificationChannelId = createWarningNotificationChannel(context)

        val notification = createNotification(
            context,
            NotificationData(
                context.getString(R.string.start_error_notif_content),
                context.getString(R.string.warning_notif_ticker),
                Notification.PRIORITY_HIGH,
                false,
                notificationChannelId
            ),
            null,
            true
        )

        show(context, NOTIFICATION_ID_NO_START, notification)
    }

    fun showLowKeepaliveNotification(context: Context, keepalive: Int) {
        val notificationChannelId = createWarningNotificationChannel(context)

        val notification = createNotification(
            context,
            NotificationData(
                context.getString(R.string.low_keepalive_notif_content).format(keepalive),
                context.getString(R.string.warning_notif_ticker),
                Notification.PRIORITY_HIGH,
                false,
                notificationChannelId
            ),
            null,
            true
        )

        show(context, NOTIFICATION_ID_LOW_KEEPALIVE, notification)
    }

    fun showNoPingNotification(context: Context) {
        val notificationChannelId = createWarningNotificationChannel(context)

        val notification = createNotification(
            context,
            NotificationData(
                context.getString(R.string.no_ping_notif_content),
                context.getString(R.string.warning_notif_ticker),
                Notification.PRIORITY_HIGH,
                false,
                notificationChannelId
            ),
            null,
            true
        )

        show(context, NOTIFICATION_ID_NO_PING, notification)
    }
}
