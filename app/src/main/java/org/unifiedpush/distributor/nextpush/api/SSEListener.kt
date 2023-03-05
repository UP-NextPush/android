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
import org.unifiedpush.distributor.nextpush.services.RestartWorker
import org.unifiedpush.distributor.nextpush.services.StartService
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.Exception
import java.util.Calendar

class SSEListener(val context: Context) : EventSourceListener() {

    override fun onOpen(eventSource: EventSource, response: Response) {
        FailureHandler.newEventSource(context, eventSource)
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
            "keepalive" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                keepalive = message.keepalive
                Log.d(TAG, "New keepalive: $keepalive")
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
        eventSource.cancel()
        if (!StartService.isServiceStarted) {
            return
        }
        Log.d(TAG, "onClosed: $eventSource")
        FailureHandler.newFail(context, eventSource)
        RestartWorker.run(context, delay = 0)
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        eventSource.cancel()
        if (!StartService.isServiceStarted) {
            return
        }
        Log.d(TAG, "onFailure")
        t?.let {
            Log.d(TAG, "An error occurred: $t")
        }
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
        }
        FailureHandler.newFail(context, eventSource)
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

    companion object {
        var lastEventDate: Calendar? = null
        var keepalive = 900
            private set
    }
}
