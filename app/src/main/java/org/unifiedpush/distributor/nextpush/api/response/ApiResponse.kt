package org.unifiedpush.distributor.nextpush.api.response

data class ApiResponse(
    val success: Boolean = false,
    val deviceId: String = "",
    val token: String = ""
)
