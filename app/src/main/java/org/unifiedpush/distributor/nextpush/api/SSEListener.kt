package org.unifiedpush.distributor.nextpush.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.unifiedpush.distributor.nextpush.api.response.SSEResponse
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteAppFromSSE
import org.unifiedpush.distributor.nextpush.distributor.Distributor.sendMessage
import org.unifiedpush.distributor.nextpush.services.FailureHandler
import org.unifiedpush.distributor.nextpush.services.RestartNetworkCallback
import org.unifiedpush.distributor.nextpush.services.RestartWorker
import org.unifiedpush.distributor.nextpush.services.StartService
import org.unifiedpush.distributor.nextpush.utils.NotificationUtils.showLowKeepaliveNotification
import org.unifiedpush.distributor.nextpush.utils.NotificationUtils.showStartErrorNotification
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

class SSEListener(val context: Context) : EventSourceListener() {

    override fun onOpen(eventSource: EventSource, response: Response) {
        FailureHandler.newEventSource(context, eventSource)
        startingTimer = Timer().schedule(30_000L /* 30secs */) {
            StartService.stopService()
            showStartErrorNotification(context)
        }
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
        lastEventDate = Calendar.getInstance()

        when (type) {
            "start" -> {
                started = true
                startingTimer?.cancel()
            }
            "ping" -> {
                pinged = true
                FailureHandler.newPing()
            }
            "keepalive" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                keepalive = message.keepalive
                Log.d(TAG, "New keepalive: $keepalive")
                if (keepalive < 25) {
                    showLowKeepaliveNotification(context, keepalive)
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
        FailureHandler.newFail(context, eventSource)
        clearVars()
        RestartWorker.run(context, delay = 0)
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
        if (!RestartNetworkCallback.hasInternet) {
            Log.d(TAG, "No Internet: do not restart")
            FailureHandler.once(eventSource)
            clearVars()
            return
        }
        FailureHandler.newFail(context, eventSource)
        clearVars()
        val delay = when (FailureHandler.nFails) {
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

    private fun shouldRestart(): Boolean {
        if (!StartService.isServiceStarted) {
            Log.d(TAG, "StartService not started")
            clearVars()
            return false
        }
        return true
    }

    private fun clearVars() {
        startingTimer?.cancel()
        started = false
        pinged = false
    }

    companion object {
        var lastEventDate: Calendar? = null
        var keepalive = 900
            private set
        private var startingTimer: TimerTask? = null
        var pinged = false
            private set
        var started = false
            private set
    }
}
