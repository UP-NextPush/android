package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import okhttp3.sse.EventSource
import org.unifiedpush.distributor.nextpush.services.NotificationUtils.createWarningNotification
import org.unifiedpush.distributor.nextpush.services.NotificationUtils.deleteWarningNotification

private const val TAG = "FailureHandler"

interface FailureHandler {

    var nFails: Int
    // This is the last eventSource opened
    var eventSource: EventSource?

    fun newEvent(context: Context, eventSource: EventSource) {
        Log.d(TAG,"newEvent/Eventsource: $eventSource")
        this.eventSource = eventSource
        nFails = 0
        deleteWarningNotification(context)
    }

    fun newFail(context: Context, eventSource: EventSource?) {
        Log.d(TAG,"newFail/Eventsource: $eventSource")
        // no eventSource or the last opened
        if (this.eventSource == null || this.eventSource == eventSource) {
            Log.d(TAG, "EventSource is known or null")
            nFails++
            if (hasFailed(twice = true))
                createWarningNotification(context)
        }
    }

    fun setMaxFails(context: Context) {
        // We set nFails to max to not restart the worker
        // and keep it running
        nFails = 5
        eventSource = null
        createWarningNotification(context)
    }

    fun clearFails() {
        nFails = 0
        eventSource = null
    }
    
    fun hasFailed(twice: Boolean = false, orNeverStart: Boolean = true): Boolean {
        // nFails > 0 to be sure it is not actually restarting
        return if (orNeverStart) { eventSource == null } else { false } ||
                nFails > if (twice) { 1 } else { 0 }
    }
}