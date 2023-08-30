package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import okhttp3.sse.EventSource
import org.unifiedpush.distributor.nextpush.api.SSEListener
import org.unifiedpush.distributor.nextpush.utils.DisconnectedNotification
import org.unifiedpush.distributor.nextpush.utils.NoPingNotification
import org.unifiedpush.distributor.nextpush.utils.TAG

object FailureHandler {

    private var ttlFails = 0

    var nFails = 0
        private set(value) {
            if (value > 0) {
                ttlFails += 1
            }
            field = value
        }

    private var nFailsBeforePing = 0

    // This is the last eventSource opened
    private var eventSource: EventSource? = null
        set(value) {
            field?.cancel()
            field = value
        }

    fun newEventSource(context: Context, eventSource: EventSource) {
        Log.d(TAG, "newEvent/Eventsource: $eventSource")
        this.eventSource = eventSource
        nFails = 0
        DisconnectedNotification(context).delete()
    }

    fun newPing() {
        nFailsBeforePing = 0
    }

    fun newFail(context: Context, eventSource: EventSource?) {
        Log.d(TAG, "newFail/Eventsource: $eventSource")
        // ignore fails from a possible old eventSource
        // if we are already reconnected
        if (this.eventSource == null || this.eventSource == eventSource) {
            Log.d(TAG, "EventSource is known or null")
            nFails++
            if (nFails == 2) {
                DisconnectedNotification(context).show()
            }
            if (SSEListener.started && !SSEListener.pinged) {
                Log.d(TAG, "The service has started and it has never received a ping.")
                nFailsBeforePing++
                if (nFailsBeforePing == 5) {
                    NoPingNotification(context).show()
                }
            }
            this.eventSource = null
        } else {
            eventSource?.cancel()
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
        } else {
            eventSource?.cancel()
        }
    }

    fun setMaxFails(context: Context) {
        // We set nFails to max to not restart the worker
        // and keep it running
        nFails = 5
        eventSource = null
        DisconnectedNotification(context).show()
    }

    fun clearFails() {
        ttlFails = 0
        nFails = 0
        nFailsBeforePing = 0
        eventSource = null
    }

    fun hasFailed(orNeverStart: Boolean = true): Boolean {
        // nFails > 0 to be sure it is not actually restarting
        return if (orNeverStart) { eventSource == null } else { false } || nFails > 0
    }

    fun getDebugInfo(): String {
        return "ttlFails: $ttlFails\n" +
            "nFails: $nFails\n" +
            "nFailsBeforePing: $nFailsBeforePing\n" +
            "eventSource null: ${eventSource == null}"
    }
}
