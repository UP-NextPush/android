package org.unifiedpush.distributor.nextpush.services

import android.content.Context
import android.util.Log
import okhttp3.sse.EventSource
import org.unifiedpush.distributor.nextpush.AppCompanion
import org.unifiedpush.distributor.nextpush.utils.DisconnectedNotification
import org.unifiedpush.distributor.nextpush.utils.NoPingNotification
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

object FailureHandler {

    private val ttlFails = AtomicInteger(0)
    private val nFails = AtomicInteger(0)
    private val nFailsBeforePing = AtomicInteger(0)

    // This is the last eventSource opened
    private val eventSource: AtomicReference<EventSource?> = AtomicReference(null)

    private fun isCurrentEventSource(eventSource: EventSource?): Boolean {
        return this.eventSource.get()?.let {
            it == eventSource
        } ?: true
    }
    fun newEventSource(context: Context, eventSource: EventSource) {
        Log.d(TAG, "newEvent/Eventsource: $eventSource")
        this.eventSource.getAndSet(eventSource)?.cancel()
        nFails.set(0)
        DisconnectedNotification(context).delete()
    }

    fun newPing(context: Context) {
        nFailsBeforePing.set(0)
        NoPingNotification(context).delete()
    }

    fun nFails(): Int {
        return nFails.get()
    }

    // Returns true if the fail is from the current eventSource
    fun newFail(context: Context, eventSource: EventSource?): Boolean {
        Log.d(TAG, "newFail/Eventsource: $eventSource")
        // ignore fails from a possible old eventSource
        // if we are already reconnected
        return if (isCurrentEventSource(eventSource)) {
            Log.d(TAG, "EventSource is known or null")
            ttlFails.getAndIncrement()
            if (nFails.incrementAndGet() == 2) {
                DisconnectedNotification(context).show()
            }
            if (AppCompanion.started.get() && !AppCompanion.pinged.get()) {
                Log.d(TAG, "The service has started and it has never received a ping.")
                if (nFailsBeforePing.incrementAndGet() == 5) {
                    NoPingNotification(context).show()
                }
            }
            this.eventSource.getAndSet(null)?.cancel()
            true
        } else {
            Log.d(TAG, "This is an old EventSource.")
            eventSource?.cancel()
            false
        }
    }

    // Returns true if the fail is from the current eventSource
    fun once(eventSource: EventSource?): Boolean {
        Log.d(TAG, "once/Eventsource: $eventSource")
        // ignore fails from a possible old eventSource
        // if we are already reconnected
        return if (isCurrentEventSource(eventSource)) {
            Log.d(TAG, "EventSource is known or null")
            nFails.set(1)
            this.eventSource.getAndSet(null)?.cancel()
            true
        } else {
            Log.d(TAG, "This is an old EventSource.")
            eventSource?.cancel()
            false
        }
    }

    fun setMaxFails(context: Context) {
        // We set nFails to max to not restart the worker
        // and keep it running
        nFails.set(5)
        ttlFails.getAndIncrement()
        this.eventSource.getAndSet(null)?.cancel()
        DisconnectedNotification(context).show()
    }

    fun clearFails() {
        ttlFails.set(0)
        nFails.set(0)
        nFailsBeforePing.set(0)
        this.eventSource.getAndSet(null)?.cancel()
    }

    fun hasFailed(orNeverStart: Boolean = true): Boolean {
        // nFails > 0 to be sure it is not actually restarting
        return if (orNeverStart) { eventSource.get() == null } else { false } || nFails.get() > 0
    }

    fun getDebugInfo(): String {
        return "ttlFails: ${ttlFails.get()}\n" +
            "nFails: $nFails\n" +
            "nFailsBeforePing: $nFailsBeforePing\n" +
            "eventSource null: ${eventSource.get() == null}"
    }
}
