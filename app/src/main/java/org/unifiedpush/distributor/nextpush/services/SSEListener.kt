package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.os.CountDownTimer
import android.os.Looper
import android.util.Base64
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSource
import android.util.Log
import okhttp3.Response
import java.lang.Exception
import com.google.gson.Gson
import org.unifiedpush.distributor.nextpush.api.SSEResponse
import org.unifiedpush.distributor.nextpush.distributor.getDb
import org.unifiedpush.distributor.nextpush.distributor.sendMessage
import org.unifiedpush.distributor.nextpush.distributor.sendUnregistered

private const val TAG = "SSEListener"

class SSEListener (val context: Context) : EventSourceListener() {

    override fun onOpen(eventSource: EventSource, response: Response) {
        deleteWarningNotification(context)
        failed = false
        try {
            Log.d(TAG, "onOpen: " + response.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEvent(eventSource: EventSource, id: String?, eventType: String?, data: String) {
        Log.d(TAG, "New SSE message event=$eventType message=$data")
        when (eventType) {
            "warning" -> Log.d(TAG, "Warning event received.")
            "ping" -> Log.d(TAG, "SSE ping received.")
            "message" -> {
                Log.d(TAG, "Notification event received.")
                val message = Gson().fromJson(data, SSEResponse::class.java)
                sendMessage(
                    context,
                    message.token,
                    String(Base64.decode(message.message, Base64.DEFAULT))
                )
            }
            "deleteApp" -> {
                val message = Gson().fromJson(data, SSEResponse::class.java)
                val db = getDb(context.applicationContext)
                val connectorToken = db.getConnectorToken(message.token)
                if (connectorToken.isEmpty())
                    return
                sendUnregistered(context.applicationContext, connectorToken)
                db.unregisterApp(connectorToken)
            }
        }
    }

    override fun onClosed(eventSource: EventSource) {
        Log.d(TAG, "onClosed: $eventSource")
        isServiceStarted = false
        createWarningNotification(context)
        startListener(context)
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        Log.d(TAG, "onFailure")
        isServiceStarted = false
        createWarningNotification(context)
        val timeStop = if (!failed) {
            2000
        } else {
            60000
        }.toLong()
        Log.d(TAG, "Retrying in $timeStop ms")
        failed = true
        Looper.prepare()
        object : CountDownTimer(timeStop, timeStop) {

            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                Log.d(TAG, "Trying to restart")
                startListener(context)
            }

        }.start()
        Looper.loop()
        t?.let {
            Log.d(TAG, "An error occurred: $t")
            return
        }
        response?.let {
            Log.d(TAG, "onFailure: ${it.code}")
        }
    }
}
