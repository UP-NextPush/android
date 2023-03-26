package org.unifiedpush.distributor.nextpush.utils

import org.unifiedpush.distributor.nextpush.api.SSEListener
import org.unifiedpush.distributor.nextpush.services.FailureHandler
import org.unifiedpush.distributor.nextpush.services.StartService
import java.text.SimpleDateFormat

fun getDebugInfo(): String {
    val date = SSEListener.lastEventDate?.let {
        SimpleDateFormat.getDateTimeInstance().format(it.time)
    } ?: "None"
    return "ServiceStarted: ${StartService.isServiceStarted}\n" +
        "Last Event: $date\n" +
        "Keepalive: ${SSEListener.keepalive}\n" +
        "SSE started: ${SSEListener.started}\n" +
        "SSE pinged: ${SSEListener.pinged}\n" +
        FailureHandler.getDebugInfo()
}
