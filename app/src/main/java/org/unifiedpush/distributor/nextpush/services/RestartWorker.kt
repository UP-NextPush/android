package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import androidx.work.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.api.SSEListener.Companion.keepalive
import org.unifiedpush.distributor.nextpush.api.SSEListener.Companion.lastEventDate
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val UNIQUE_PERIODIC_WORK_TAG = "nextpush::RestartWorker::unique_periodic"
private const val UNIQUE_ONETIME_WORK_TAG = "nextpush::RestartWorker::unique_onetime"

class RestartWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        Log.d(TAG, "Working")
        val currentDate = Calendar.getInstance()
        val restartDate = Calendar.getInstance()
        lastEventDate?.let {
            restartDate.time = it.time
            restartDate.add(Calendar.SECOND, keepalive)
            Log.d(TAG, "restartDate: ${restartDate.time}")
            if (currentDate.after(restartDate)) {
                Log.d(TAG, "Restarting")
                FailureHandler.setMaxFails(applicationContext) // Max, will keep using the current worker
                StartService.startListener(applicationContext)
            }
        } ?: run {
            Log.d(TAG, "Restarting")
            StartService.startListener(applicationContext)
        }
        return Result.success()
    }

    companion object {

        fun startPeriodic(context: Context) {
            getAccount(context) ?: return
            val work = PeriodicWorkRequestBuilder<RestartWorker>(16, TimeUnit.MINUTES)
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK_TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    work.build()
                )
        }
        fun run(context: Context, delay: Long) {
            val work = OneTimeWorkRequestBuilder<RestartWorker>().apply {
                setInitialDelay(delay, TimeUnit.SECONDS)
            }
            lastEventDate = null
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONETIME_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                work.build()
            )
        }

        fun stopPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUE_PERIODIC_WORK_TAG)
        }
    }
}
