package org.unifiedpush.distributor.nextpush.api

data class ApiResponse(
    val success: Boolean = false,
    val deviceId: String = "",
    val token: String = "",
)
