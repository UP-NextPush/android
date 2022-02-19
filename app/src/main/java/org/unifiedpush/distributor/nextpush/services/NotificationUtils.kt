package org.unifiedpush.distributor.nextpush.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import org.unifiedpush.distributor.nextpush.R
import android.app.PendingIntent

import android.content.Intent
import org.unifiedpush.distributor.nextpush.activities.MainActivity

const val NOTIFICATION_ID_FOREGROUND = 51115
const val NOTIFICATION_ID_WARNING = 51215

object NotificationUtils {

    private var warningShown = false

    fun createForegroundNotification(context: Context): Notification {
        val appName = context.getString(R.string.app_name)
        val notificationChannelId = "$appName.Listener"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                appName,
                NotificationManager.IMPORTANCE_LOW
            ).let {
                it.description = context.getString(R.string.foreground_notif_description)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)

        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val intent = PendingIntent.getActivity(
            context, 0,
            notificationIntent, 0
        )

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                context,
                notificationChannelId
            ) else Notification.Builder(context)

        return builder
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.foreground_notif_description))
            .setSmallIcon(R.drawable.ic_launcher_notification)
            .setTicker(context.getString(R.string.foreground_notif_ticker))
            .setPriority(Notification.PRIORITY_LOW) // for under android 26 compatibility
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    fun createWarningNotification(context: Context) {
        if (warningShown)
            return
        val appName = context.getString(R.string.app_name)
        val notificationChannelId = "$appName.Warning"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                appName,
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = context.getString(R.string.warning_notif_description)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)

        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val intent = PendingIntent.getActivity(
            context, 0,
            notificationIntent, 0
        )

        val builder: Notification.Builder = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                    context,
                    notificationChannelId
                )
                else Notification.Builder(context)
                ).setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.warning_notif_description))
            .setSmallIcon(R.drawable.ic_launcher_notification)
            .setTicker(context.getString(R.string.warning_notif_ticker))
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .setContentIntent(intent)
            .setOngoing(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_WARNING, builder.build())
        }
        warningShown = true
    }

    fun deleteWarningNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_WARNING)
        warningShown = false
    }
}