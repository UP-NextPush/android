package org.unifiedpush.distributor.nextpush

import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object AppCompanion {
    val booting = AtomicBoolean(false)
    val hasInternet = AtomicBoolean(true)
    val started = AtomicBoolean(false)
    val pinged = AtomicBoolean(false)
    val bufferedResponseChecked = AtomicBoolean(false)
    val keepalive = AtomicInteger(900)
    var lastEventDate: Calendar? = null
}
