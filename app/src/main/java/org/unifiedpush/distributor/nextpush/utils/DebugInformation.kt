package org.unifiedpush.distributor.nextpush.utils

import org.unifiedpush.distributor.nextpush.AppCompanion
import org.unifiedpush.distributor.nextpush.services.FailureHandler
import org.unifiedpush.distributor.nextpush.services.StartService
import java.text.SimpleDateFormat

fun getDebugInfo(): String {
    val date = AppCompanion.lastEventDate?.let {
        SimpleDateFormat.getDateTimeInstance().format(it.time)
    } ?: "None"
    return "ServiceStarted: ${StartService.isServiceStarted}\n" +
        "Last Event: $date\n" +
        "Keepalive: ${AppCompanion.keepalive.get()}\n" +
        "SSE started: ${AppCompanion.started}\n" +
        "SSE pinged: ${AppCompanion.pinged}\n" +
        FailureHandler.getDebugInfo()
}
