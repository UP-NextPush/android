package org.unifiedpush.distributor.nextpush.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import org.unifiedpush.distributor.nextpush.AppCompanion
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.api.Api
import org.unifiedpush.distributor.nextpush.utils.ForegroundNotification
import org.unifiedpush.distributor.nextpush.utils.NOTIFICATION_ID_FOREGROUND
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.util.concurrent.atomic.AtomicReference

class StartService : Service() {

    private val networkCallback = RestartNetworkCallback(this)
    private var api: Api? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        service.set(this)
        Log.i(TAG, "StartService created")
        val notification = ForegroundNotification(this).create()
        startForeground(NOTIFICATION_ID_FOREGROUND, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        networkCallback.register()
        startService()
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying Service")
        api?.apiDestroy()
        wakeLock?.let {
            while (it.isHeld) {
                it.release()
            }
        }
        if (isServiceStarted) {
            Log.d(TAG, "onDestroy: restarting worker")
            RestartWorker.run(this, delay = 0)
        } else {
            networkCallback.unregister()
            RestartWorker.stopPeriodic(this)
        }
    }

    private fun startService() {
        // If the service is running and we don't have any fail
        // In case somehow startService is called when everything is fine
        if (isServiceStarted && !FailureHandler.hasFailed()) return
        getAccount(this) ?: run {
            Log.d(TAG, "No account found")
            return
        }
        isServiceStarted = true

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire(10000L /*10 secs*/)
            }
        }
        api = Api(this).also {
            it.apiSync()
        }
    }

    companion object StartServiceCompanion {
        private const val WAKE_LOCK_TAG = "NextPush:StartService:lock"

        private val service: AtomicReference<StartService?> = AtomicReference(null)
        var isServiceStarted = false
            private set
        var wakeLock: PowerManager.WakeLock? = null
            private set

        fun startListener(context: Context) {
            if (isServiceStarted && !FailureHandler.hasFailed()) return
            Log.d(TAG, "Starting the Listener")
            Log.d(TAG, "Service is started: $isServiceStarted")
            Log.d(TAG, "nFails: ${FailureHandler.nFails()}")
            val serviceIntent = Intent(context, StartService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        fun stopService(block: () -> Unit = {}) {
            Log.d(TAG, "Stopping Service")
            isServiceStarted = false
            AppCompanion.bufferedResponseChecked.set(false)
            AppCompanion.lastEventDate = null
            service.get()?.stopSelf()
            block()
        }
    }
}
