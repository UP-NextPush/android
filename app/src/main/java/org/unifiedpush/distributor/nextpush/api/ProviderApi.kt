package org.unifiedpush.distributor.nextpush.api

import io.reactivex.Observable
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Path

interface ProviderApi {

    @PUT("/device/")
    fun createDevice(
        @Body subscribeMap: MutableMap<String, String>
    ): Observable<ApiResponse>?

    @DELETE("/device/{deviceId}")
    fun deleteDevice(@Path("deviceId") deviceId: String): Observable<ApiResponse>?

    @PUT("/app/")
    fun createApp(
        @Body authorizeMap: MutableMap<String, String>
    ): Observable<ApiResponse>?

    @DELETE("/app/{token}")
    fun deleteApp(@Path("token") token: String): Observable<ApiResponse>?

    companion object {
        const val mApiEndpoint = "/index.php/apps/uppush"
    }
}
