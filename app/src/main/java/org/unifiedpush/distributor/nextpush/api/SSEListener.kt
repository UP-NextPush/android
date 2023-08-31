package org.unifiedpush.distributor.nextpush.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.unifiedpush.distributor.nextpush.AppCompanion
import org.unifiedpush.distributor.nextpush.api.response.SSEResponse
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteAppFromSSE
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendMessage
import org.unifiedpush.distributor.nextpush.services.FailureHandler
import org.unifiedpush.distributor.nextpush.services.RestartWorker
import org.unifiedpush.distributor.nextpush.services.StartService
import org.unifiedpush.distributor.nextpush.utils.LowKeepAliveNotification
import org.unifiedpush.distributor.nextpush.utils.NoStartNotification
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.schedule

class SSEListener(val context: Context) : EventSourceListener() {

    override fun onOpen(eventSource: EventSource, response: Response) {
        FailureHandler.newEventSource(context, eventSource)
        val timer: TimerTask? = if (
            !AppCompanion.bufferedResponseChecked.get() &&
            !AppCompanion.booting.getAndSet(false)
        ) {
            Timer().schedule(45_000L /* 45secs */) {
                if (FailureHandler.newFail(context, eventSource)) {
                    StartService.stopService()
                    NoStartNotification(context).show()
                }
            }
        } else {
            null
        }
        startingTimer.getAndSet(timer)?.cancel()
        StartService.wakeLock?.let {
            while (it.isHeld) {
                it.release()
            }
        }
        try {
            Log.d(TAG, "onOpen: " + response.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        Log.d(TAG, "New SSE message event: $type")
        StartService.wakeLock?.acquire(30000L /*30 secs*/)
        AppCompanion.lastEventDate = Calendar.getInstance()

        when (type) {
            "start" -> {
                AppCompanion.started.set(true)
                startingTimer.getAndSet(null)?.cancel()
                AppCompanion.bufferedResponseChecked.set(true)
                NoStartNotification(context).delete()
            }
            "ping" -> {
                AppCompanion.pinged.set(true)
                FailureHandler.newPing(context)
            }
            "keepalive" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                message.keepalive.let {
                    AppCompanion.keepalive.set(it)
                    Log.d(TAG, "New keepalive: $it")
                    if (it < 25) {
                        LowKeepAliveNotification(context, it).show()
                    } else {
                        LowKeepAliveNotification(context, it).delete()
                    }
                }
            }
            "message" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                sendMessage(
                    context,
                    message.token,
                    Base64.decode(message.message, Base64.DEFAULT)
                )
            }
            "deleteApp" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                deleteAppFromSSE(context, message.token)
            }
        }
        StartService.wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onClosed(eventSource: EventSource) {
        Log.d(TAG, "onClosed: $eventSource")
        eventSource.cancel()
        if (!shouldRestart()) return
        if (FailureHandler.newFail(context, eventSource)) {
            clearVars()
            RestartWorker.run(context, delay = 0)
        }
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        Log.d(TAG, "onFailure")
        eventSource.cancel()
        if (!shouldRestart()) return
        t?.let {
            Log.d(TAG, "An error occurred: $t")
        }
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
        }
        if (!AppCompanion.hasInternet.get()) {
            Log.d(TAG, "No Internet: do not restart")
            if (FailureHandler.once(eventSource)) {
                clearVars()
            }
            return
        }
        if (FailureHandler.newFail(context, eventSource)) {
            clearVars()
            val delay = when (FailureHandler.nFails()) {
                1 -> 2 // 2sec
                2 -> 5 // 5sec
                3 -> 20 // 20sec
                4 -> 60 // 1min
                5 -> 300 // 5min
                6 -> 600 // 10min
                else -> return // else keep the worker with its 16min
            }.toLong()
            Log.d(TAG, "Retrying in $delay s")
            RestartWorker.run(context, delay = delay)
        }
    }

    private fun shouldRestart(): Boolean {
        if (!StartService.isServiceStarted) {
            Log.d(TAG, "StartService not started")
            clearVars()
            return false
        }
        return true
    }

    private fun clearVars() {
        startingTimer.getAndSet(null)?.cancel()
        AppCompanion.started.set(false)
        AppCompanion.pinged.set(false)
    }

    companion object {
        private var startingTimer: AtomicReference<TimerTask?> = AtomicReference(null)
    }
}
