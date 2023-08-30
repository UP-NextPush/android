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
import java.util.concurrent.atomic.AtomicBoolean

const val NOTIFICATION_ID_FOREGROUND = 51115
const val NOTIFICATION_ID_WARNING = 51215
const val NOTIFICATION_ID_NO_START = 51315
const val NOTIFICATION_ID_LOW_KEEPALIVE = 51315
const val NOTIFICATION_ID_NO_PING = 51515
const val NOTIFICATION_ID_FROM_PUSH = 51615

data class ChannelData(
    val id: String,
    val name: String,
    val importance: Int,
    val description: String
)

data class NotificationData(
    val title: String,
    val text: String,
    val ticker: String,
    val priority: Int,
    val ongoing: Boolean
)

open class AppNotification(
    private val context: Context,
    private val notificationId: Int,
    private val notificationData: NotificationData,
    private val channelData: ChannelData,
) {
    private val shown = AtomicBoolean(false)

    private fun createNotificationChannel() {
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

    private fun createNotification(
        intent: PendingIntent?,
        bigText: Boolean = false
    ): Notification {
        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(
                    context,
                    channelData.id
                )
            } else {
                Notification.Builder(context)
            }

        return builder
            .setContentTitle(notificationData.title)
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

    private fun createIntentToMain(): PendingIntent {
        val notificationIntent = Intent(context, MainActivity::class.java)

        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun deleteNotification(notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    private fun show(notificationId: Int, notification: Notification) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, notification)
            }
        }
    }

    fun create(): Notification {
        createNotificationChannel()

        return createNotification(
            createIntentToMain()
        )
    }
    fun show() {
        if (!shown.getAndSet(true)) {
            show(notificationId, create())
        }
    }

    fun delete() {
        if (shown.getAndSet(false)) {
            deleteNotification(notificationId)
        }
    }
}

private val Context.warningChannelData: ChannelData
    get() = ChannelData(
    "${this.getString(R.string.app_name)}.Warning",
    "Warning",
    NotificationManager.IMPORTANCE_HIGH,
    this.getString(R.string.warning_notif_description)
    )

class DisconnectedNotification(context: Context) : AppNotification(
    context,
    NOTIFICATION_ID_WARNING,
    NotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.warning_notif_content),
        context.getString(R.string.warning_notif_ticker),
        Notification.PRIORITY_HIGH,
        true
    ),
    context.warningChannelData
)

class NoPingNotification(context: Context) : AppNotification(
    context,
    NOTIFICATION_ID_NO_PING,
    NotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.no_ping_notif_content),
        context.getString(R.string.warning_notif_ticker),
        Notification.PRIORITY_HIGH,
        false
    ),
    context.warningChannelData
)

class NoStartNotification(context: Context) : AppNotification(
    context,
    NOTIFICATION_ID_NO_START,
    NotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.start_error_notif_content),
        context.getString(R.string.warning_notif_ticker),
        Notification.PRIORITY_HIGH,
        false
    ),
    context.warningChannelData
)

class LowKeepAliveNotification(context: Context, keepalive: Int) : AppNotification(
    context,
    NOTIFICATION_ID_LOW_KEEPALIVE,
    NotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.low_keepalive_notif_content).format(keepalive),
        context.getString(R.string.warning_notif_ticker),
        Notification.PRIORITY_HIGH,
        false
    ),
    context.warningChannelData
)

class ForegroundNotification(context: Context) : AppNotification(
    context,
    NOTIFICATION_ID_FOREGROUND,
    NotificationData(
        context.getString(R.string.app_name),
        context.getString(R.string.foreground_notif_description),
        context.getString(R.string.foreground_notif_ticker),
        Notification.PRIORITY_LOW,
        true
    ),
    ChannelData(
        "${context.getString(R.string.app_name)}.Listener",
        "Foreground Service",
        NotificationManager.IMPORTANCE_LOW,
        context.getString(R.string.foreground_notif_description)
    )
)

class FromPushNotification(context: Context, title: String, content: String) : AppNotification(
    context,
    NOTIFICATION_ID_FROM_PUSH,
    NotificationData(
        title,
        content,
        title,
        Notification.PRIORITY_HIGH,
        false
    ),
    ChannelData(
        "${context.getString(R.string.app_name)}.PushNotification",
        "Push notifications",
        NotificationManager.IMPORTANCE_HIGH,
        context.getString(R.string.local_notif_description)
    )
)
