package org.unifiedpush.distributor.nextpush.api

data class SSEResponse (
    val type: String = "",
    val token: String = "",
    val message: String = ""
)