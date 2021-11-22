package org.unifiedpush.distributor.nextpush.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.ui.UiExceptionManager

import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.api.ApiUtils
import org.unifiedpush.distributor.nextpush.account.ssoAccount

private const val TAG = "StartService"

class StartService: Service(){

    private var isServiceStarted = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var api = ApiUtils()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate(){
        super.onCreate()
        Log.i(TAG,"Starting")
        val notification = createNotification()
        startForeground(51115, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startService()
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onDestroy() {
        api.destroy()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val appName = getString(R.string.app_name)
        val notificationChannelId = "$appName.Listener"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                    notificationChannelId,
                    appName,
                    NotificationManager.IMPORTANCE_LOW
            ).let {
                it.description = getString(R.string.listening_notif_description)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
        ) else Notification.Builder(this)

        return builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.listening_notif_description))
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setTicker("Listening")
                .setPriority(Notification.PRIORITY_LOW) // for under android 26 compatibility
                .build()
    }

    private fun startService() {
        if (isServiceStarted) return
        isServiceStarted = true

        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(this)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            UiExceptionManager.showDialogForException(this, e)
        } catch (e: NoCurrentAccountSelectedException) {
            return
        }

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                acquire()
            }
        }

        api.sync(this)
    }
}

