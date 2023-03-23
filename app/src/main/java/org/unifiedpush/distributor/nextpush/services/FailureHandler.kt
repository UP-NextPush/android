package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import okhttp3.sse.EventSource
import org.unifiedpush.distributor.nextpush.utils.NotificationUtils.createWarningNotification
import org.unifiedpush.distributor.nextpush.utils.NotificationUtils.deleteWarningNotification
import org.unifiedpush.distributor.nextpush.utils.TAG

object FailureHandler {

    var nFails = 0
        private set

    // This is the last eventSource opened
    private var eventSource: EventSource? = null

    fun newEventSource(context: Context, eventSource: EventSource) {
        Log.d(TAG, "newEvent/Eventsource: $eventSource")
        this.eventSource = eventSource
        nFails = 0
        deleteWarningNotification(context)
    }

    fun newFail(context: Context, eventSource: EventSource?) {
        Log.d(TAG, "newFail/Eventsource: $eventSource")
        // ignore fails from a possible old eventSource
        // if we are already reconnected
        if (this.eventSource == null || this.eventSource == eventSource) {
            Log.d(TAG, "EventSource is known or null")
            nFails++
            if (nFails == 2) {
                createWarningNotification(context)
            }
            this.eventSource = null
        }
    }

    fun once(eventSource: EventSource?) {
        Log.d(TAG, "once/Eventsource: $eventSource")
        // ignore fails from a possible old eventSource
        // if we are already reconnected
        if (this.eventSource == null || this.eventSource == eventSource) {
            Log.d(TAG, "EventSource is known or null")
            nFails = 1
            this.eventSource = null
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

    fun hasFailed(orNeverStart: Boolean = true): Boolean {
        // nFails > 0 to be sure it is not actually restarting
        return if (orNeverStart) { eventSource == null } else { false } || nFails > 0
    }
}
