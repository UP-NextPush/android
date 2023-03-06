package org.unifiedpush.distributor.nextpush.api.provider

import io.reactivex.rxjava3.core.Observable
import org.unifiedpush.distributor.nextpush.api.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiProvider {

    @PUT("device/")
    fun createDevice(
        @Body subscribeMap: Map<String, String>
    ): Observable<ApiResponse>?

    @DELETE("device/{deviceId}")
    fun deleteDevice(@Path("deviceId") deviceId: String): Observable<ApiResponse>?

    @PUT("app/")
    fun createApp(
        @Body authorizeMap: Map<String, String>
    ): Observable<ApiResponse>?

    @DELETE("app/{token}")
    fun deleteApp(@Path("token") token: String): Observable<ApiResponse>?

    companion object {
        const val mApiEndpoint = "/index.php/apps/uppush/"
    }
}
