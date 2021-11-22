package org.unifiedpush.distributor.nextpush.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.GsonBuilder
import com.nextcloud.android.sso.api.NextcloudAPI
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import org.unifiedpush.distributor.nextpush.account.getDeviceId
import org.unifiedpush.distributor.nextpush.account.removeDeviceId
import org.unifiedpush.distributor.nextpush.account.saveDeviceId
import org.unifiedpush.distributor.nextpush.account.ssoAccount
import org.unifiedpush.distributor.nextpush.api.ProviderApi.Companion.mApiEndpoint
import org.unifiedpush.distributor.nextpush.services.SSEListener
import retrofit2.NextcloudRetrofitApiBuilder
import java.util.concurrent.TimeUnit

private const val TAG = "ApiUtils"

class ApiUtils {
    private lateinit var mApi: ProviderApi
    private lateinit var nextcloudAPI: NextcloudAPI
    private lateinit var factory: EventSource.Factory

    fun destroy() {
        if (this::nextcloudAPI.isInitialized)
            nextcloudAPI.stop()
    }

    private fun cApi(context: Context, callback: ()->Unit) {
        if (this::mApi.isInitialized and this::nextcloudAPI.isInitialized) {
            callback()
        } else {
            val callback = object : NextcloudAPI.ApiConnectedListener {
                override fun onConnected() {
                    Log.d(TAG, "Api connected.")
                    callback()
                }
                override fun onError(ex: Exception) {
                    Log.d(TAG, "Cannot connect to API: ex = [$ex]")
                }
            }
            nextcloudAPI = NextcloudAPI(context, ssoAccount, GsonBuilder().create(), callback)
            mApi = NextcloudRetrofitApiBuilder(nextcloudAPI, mApiEndpoint)
                .create(ProviderApi::class.java)
        }
    }

    fun sync(context: Context) {
        cApi(context) { cSync(context) }
    }

    private fun cSync(context: Context) {
        var deviceId = getDeviceId(context)
        // Register the device if it is not yet registered
        if (deviceId.isNullOrEmpty()) {
            Log.d(TAG, "No deviceId found.")
            val parameters: MutableMap<String, String> = HashMap()
            parameters["deviceName"] = Build.MODEL
            mApi.createDevice(parameters)
                ?.subscribeOn(Schedulers.newThread())
                ?.observeOn(Schedulers.newThread())
                ?.subscribe(object : Observer<ApiResponse?> {
                    override fun onSubscribe(d: Disposable) {
                        Log.d(TAG, "onSubscribe")
                    }

                    override fun onNext(response: ApiResponse) {
                        val deviceIdentifier: String = response.deviceId
                        Log.d(TAG, "Device Identifier: $deviceIdentifier")
                        saveDeviceId(context,deviceIdentifier)
                        deviceId = deviceIdentifier
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        // Sync once it is registered
                        cSync(deviceId!!)
                        Log.d(TAG, "mApi register: onComplete")
                    }
                })
        } else {
            // Sync directly
            Log.d(TAG, "Found deviceId: $deviceId")
            cSync(deviceId!!)
        }
    }


    private fun cSync(deviceId: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val url = "${ssoAccount.url}$mApiEndpoint/device/$deviceId"

        val request = Request.Builder().url(url)
            .get()
            .build()

        factory = EventSources.createFactory(client)
        factory.newEventSource(request, SSEListener())
        Log.d(TAG, "doConnect done.")
    }

    fun deleteDevice(context: Context) {
        cApi(context) { cDeleteDevice(context) }
    }

    private fun cDeleteDevice(context: Context) {
        var deviceId = getDeviceId(context)

        mApi.deleteDevice(deviceId)
            ?.subscribeOn(Schedulers.newThread())
            ?.observeOn(Schedulers.newThread())
            ?.subscribe(object : Observer<ApiResponse?> {
                override fun onSubscribe(d: Disposable) {
                    Log.d(TAG, "Subscribed to deleteDevice.")
                }

                override fun onNext(response: ApiResponse) {
                    if (response.success) {
                        Log.d(TAG, "Device successfully deleted.")
                    } else {
                        Log.d(TAG, "An error occurred while deleting the device registration.")
                    }
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                }
            })
        removeDeviceId(context)
    }

    fun createApp(context: Context) {
        cApi(context) { cCreateApp(context) }
    }

    private fun cCreateApp(context: Context) {

    }

    fun deleteApp(context: Context) {
        cApi(context) { cDeleteApp(context) }
    }

    private fun cDeleteApp(context: Context) {

    }
}