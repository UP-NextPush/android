package org.unifiedpush.distributor.nextpush.api

import android.content.Context
import android.os.Build
import android.util.Log
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import org.unifiedpush.distributor.nextpush.account.Account.accountType
import org.unifiedpush.distributor.nextpush.account.Account.deviceId
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.account.AccountType
import org.unifiedpush.distributor.nextpush.api.provider.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.api.provider.ApiProvider.Companion.mApiEndpoint
import org.unifiedpush.distributor.nextpush.api.response.ApiResponse
import java.util.concurrent.TimeUnit

class Api(val context: Context) {

    private val TAG = Api::class.java.simpleName

    private val baseUrl: String
        get() = getAccount(context)?.url ?: "http://0.0.0.0/"

    private fun withApiProvider(block: (ApiProvider) -> Unit) {
        when (context.accountType) {
            AccountType.SSO -> ApiSSOFactory(context)
            AccountType.Direct -> ApiDirectFactory(context)
        }.getProviderAndExecute(block)
    }

    fun apiDestroy() {
        Log.d(TAG, "Destroying API")
        syncSource?.cancel()
        syncSource = null
    }

    fun apiSync() {
        context.deviceId?.let {
            syncDevice(it)
        }
            ?: run {
                Log.d(TAG, "No deviceId found.")

                val parameters = mapOf("deviceName" to Build.MODEL)
                try {
                    withApiProvider { apiProvider ->
                        apiProvider.createDevice(parameters)
                            ?.subscribeOn(Schedulers.newThread())
                            ?.observeOn(Schedulers.newThread())
                            ?.subscribe(object : Observer<ApiResponse?> {
                                override fun onSubscribe(d: Disposable) {
                                    Log.d(TAG, "onSubscribe")
                                }

                                override fun onNext(response: ApiResponse) {
                                    response.deviceId.let {
                                        context.deviceId = it
                                    }
                                }

                                override fun onError(e: Throwable) {
                                    e.printStackTrace()
                                }

                                override fun onComplete() {
                                    // Sync once it is registered
                                    context.deviceId?.let {
                                        syncDevice(it)
                                    }
                                    Log.d(TAG, "mApi register: onComplete")
                                }
                            })
                    }
                } catch (e: NoProviderException) {
                    e.printStackTrace()
                }
            }
    }

    private fun syncDevice(deviceId: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val url = "$baseUrl${mApiEndpoint}device/$deviceId"

        val request = Request.Builder().url(url)
            .get()
            .build()

        syncSource = EventSources.createFactory(client).newEventSource(request, SSEListener(context))
        Log.d(TAG, "cSync done.")
    }

    fun apiDeleteDevice(block: () -> Unit = {}) {
        val deviceId = context.deviceId ?: return
        try {
            withApiProvider { apiProvider ->
                apiProvider.deleteDevice(deviceId)
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
                                Log.d(TAG, "An error occurred while deleting the device.")
                            }
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                        }

                        override fun onComplete() {
                            block()
                        }
                    })
                context.deviceId = null
            }
        } catch (e: NoProviderException) {
            e.printStackTrace()
        }
    }

    fun apiCreateApp(
        appName: String,
        block: (String?) -> Unit
    ) {
        // The unity of connector token must already be checked here
        val parameters = context.deviceId?.let {
            mutableMapOf(
                "deviceId" to it,
                "appName" to appName
            )
        } ?: return
        try {
            withApiProvider { apiProvider ->
                apiProvider.createApp(parameters)
                    ?.subscribeOn(Schedulers.newThread())
                    ?.observeOn(Schedulers.newThread())
                    ?.subscribe(object : Observer<ApiResponse?> {
                        override fun onSubscribe(d: Disposable) {
                            Log.d(TAG, "Subscribed to createApp.")
                        }

                        override fun onNext(response: ApiResponse) {
                            val nextpushToken = if (response.success) {
                                Log.d(TAG, "App successfully created.")
                                response.token
                            } else {
                                Log.d(TAG, "An error occurred while creating the application.")
                                null
                            }
                            block(nextpushToken)
                        }

                        override fun onError(e: Throwable) {
                            block(null)
                            e.printStackTrace()
                        }

                        override fun onComplete() {}
                    })
            }
        } catch (e: NoProviderException) {
            e.printStackTrace()
        }
    }

    fun apiDeleteApp(nextpushToken: String, block: () -> Unit) {
        try {
            withApiProvider { apiProvider ->
                apiProvider.deleteApp(nextpushToken)
                    ?.subscribeOn(Schedulers.newThread())
                    ?.observeOn(Schedulers.newThread())
                    ?.subscribe(object : Observer<ApiResponse?> {
                        override fun onSubscribe(d: Disposable) {
                            Log.d(TAG, "Subscribed to deleteApp.")
                        }

                        override fun onNext(response: ApiResponse) {
                            if (response.success) {
                                Log.d(TAG, "App successfully deleted.")
                            } else {
                                Log.d(TAG, "An error occurred while deleting the application.")
                            }
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                        }

                        override fun onComplete() {
                            block()
                        }
                    })
            }
        } catch (e: NoProviderException) {
            e.printStackTrace()
        }
    }

    companion object {
        private var syncSource: EventSource? = null
    }
}
