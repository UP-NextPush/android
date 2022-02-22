package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import androidx.work.*
import org.unifiedpush.distributor.nextpush.services.SSEListener.Companion.lastEventDate
import java.util.*
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_TAG = "nextpush::RestartWorker::unique"

class RestartWorker (ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    companion object {
        private const val TAG = "RestartWorker"
        fun start(context: Context, delay: Long? = null) {
            val work = PeriodicWorkRequestBuilder<RestartWorker>(20, TimeUnit.MINUTES)
            if (delay != null) {
                lastEventDate = null
                work.setInitialDelay(delay, TimeUnit.SECONDS)
            }
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                work.build()
            )
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "Working")
        val currentDate = Calendar.getInstance()
        val restartDate = lastEventDate?.add(Calendar.MINUTE, 15)
        if (restartDate == null || currentDate.after(restartDate)) {
            StartService.startListener(applicationContext)
        }
        return Result.success()
    }
}