package org.unifiedpush.distributor.nextpush.api.response

data class SSEResponse(
    val type: String = "",
    val token: String = "",
    val message: String = "",
    val keepalive: Int = 900
)
