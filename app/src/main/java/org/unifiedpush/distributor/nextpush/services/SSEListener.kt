package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Base64
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSource
import android.util.Log
import okhttp3.Response
import java.lang.Exception
import com.google.gson.Gson
import org.unifiedpush.distributor.nextpush.api.SSEResponse
import org.unifiedpush.distributor.nextpush.distributor.sendMessage

private const val TAG = "SSEListener"

class SSEListener (val context: Context) : EventSourceListener() {
    private var pingtime = 0.toLong()
    private val networkConnected = false

    override fun onOpen(eventSource: EventSource, response: Response) {
        pingtime = System.currentTimeMillis()
        try {
            Log.d(TAG, "onOpen: " + response.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEvent(eventSource: EventSource, id: String?, eventType: String?, data: String) {
        Log.d(TAG, "New SSE message event=$eventType message=$data")
        pingtime = System.currentTimeMillis()
        if (eventType == "warning") {
            Log.d(TAG, "Warning event received.")
            // Notification warning
        }
        if (eventType == "ping") {
            Log.d(TAG, "SSE ping received.")
        }
        if (eventType != "message") return
        Log.d(TAG, "Notification event received.")
        // handle notification
        val message = Gson().fromJson(data, SSEResponse::class.java)
        if ( message.type == "message" ) {
            sendMessage(context,
                message.token,
                String(Base64.decode(message.message,Base64.DEFAULT)))
        }
    }

    override fun onClosed(eventSource: EventSource) {
        Log.d(TAG, "onClosed: $eventSource")
        if (!networkConnected) return
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        t?.let {
            Log.d(TAG, "An error occurred: $t")
            return
        }
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
            if (!networkConnected) return
            return
        }
    }
}
