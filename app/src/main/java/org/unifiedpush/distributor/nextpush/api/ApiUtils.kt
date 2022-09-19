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
import org.unifiedpush.distributor.nextpush.account.AccountUtils.getDeviceId
import org.unifiedpush.distributor.nextpush.account.AccountUtils.removeDeviceId
import org.unifiedpush.distributor.nextpush.account.AccountUtils.removeUrl
import org.unifiedpush.distributor.nextpush.account.AccountUtils.saveDeviceId
import org.unifiedpush.distributor.nextpush.account.AccountUtils.saveUrl
import org.unifiedpush.distributor.nextpush.account.AccountUtils.ssoAccount
import org.unifiedpush.distributor.nextpush.api.ProviderApi.Companion.mApiEndpoint
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.getDb
import org.unifiedpush.distributor.nextpush.distributor.DistributorUtils.sendUnregistered
import org.unifiedpush.distributor.nextpush.services.SSEListener
import retrofit2.NextcloudRetrofitApiBuilder
import java.util.concurrent.TimeUnit

private const val TAG = "ApiUtils"

object ApiUtils {

    val createQueue = emptyList<String>().toMutableList()
    val delQueue = emptyList<String>().toMutableList()

    private lateinit var mApi: ProviderApi
    private lateinit var nextcloudAPI: NextcloudAPI
    private lateinit var factory: EventSource.Factory
    private lateinit var source: EventSource

    fun apiDestroy() {
        if (::nextcloudAPI.isInitialized)
            nextcloudAPI.stop()
        if (::source.isInitialized)
            source.cancel()
    }

    private fun cApi(context: Context, callback: () -> Unit) {
        if (::mApi.isInitialized and ::nextcloudAPI.isInitialized) {
            callback()
        } else {
            val nCallback = object : NextcloudAPI.ApiConnectedListener {
                override fun onConnected() {
                    Log.d(TAG, "Api connected.")
                    callback()
                }

                override fun onError(ex: Exception) {
                    Log.d(TAG, "Cannot connect to API: ex = [$ex]")
                }
            }
            nextcloudAPI = NextcloudAPI(context, ssoAccount, GsonBuilder().create(), nCallback)
            mApi = NextcloudRetrofitApiBuilder(nextcloudAPI, mApiEndpoint)
                .create(ProviderApi::class.java)
        }
    }

    fun apiSync(context: Context) {
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
                        saveDeviceId(context, deviceIdentifier)
                        deviceId = deviceIdentifier
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        saveUrl(context, "${ssoAccount.url}${mApiEndpoint}")
                        // Sync once it is registered
                        deviceId?.let {
                            cSync(context, it)
                        }
                        Log.d(TAG, "mApi register: onComplete")
                    }
                })
        } else {
            // Sync directly
            Log.d(TAG, "Found deviceId: $deviceId")
            deviceId?.let {
                cSync(context, it)
            }
        }
    }


    private fun cSync(context: Context, deviceId: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val url = "${ssoAccount.url}$mApiEndpoint/device/$deviceId"

        val request = Request.Builder().url(url)
            .get()
            .build()

        factory = EventSources.createFactory(client)
        source = factory.newEventSource(request, SSEListener(context))
        Log.d(TAG, "cSync done.")
    }

    fun apiDeleteDevice(context: Context) {
        val db = getDb(context)
        db.listTokens().forEach {
            sendUnregistered(context, it)
            db.unregisterApp(it)
        }
        cApi(context) { cDeleteDevice(context) }
    }

    private fun cDeleteDevice(context: Context) {
        val deviceId = getDeviceId(context)

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
                        Log.d(TAG, "An error occurred while deleting the device.")
                    }
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                    removeUrl(context)
                }
            })
        removeDeviceId(context)
    }

    fun apiCreateApp(
        context: Context,
        appName: String,
        connectorToken: String,
        callback: () -> Unit
    ) {
        cApi(context) {
            cCreateApp(context, appName, connectorToken) {
                callback()
            }
        }
    }

    private fun cCreateApp(
        context: Context,
        appName: String,
        connectorToken: String,
        callback: () -> Unit
    ) {
        // The unity of connector token must already be checked here
        val parameters = getDeviceId(context)?.let {
            mutableMapOf(
                "deviceId" to it,
                "appName" to appName
            )
        } ?: return

        mApi.createApp(parameters)
            ?.subscribeOn(Schedulers.newThread())
            ?.observeOn(Schedulers.newThread())
            ?.subscribe(object : Observer<ApiResponse?> {
                override fun onSubscribe(d: Disposable) {
                    Log.d(TAG, "Subscribed to createApp.")
                }

                override fun onNext(response: ApiResponse) {
                    if (response.success) {
                        Log.d(TAG, "App successfully created.")
                        /**
                         * Ignore printed error for SQLiteContstraintException.
                         * It is printed and not thrown by SQLiteDatabase.java
                         * So we can't catch it
                         */
                        val db = getDb(context)
                        db.registerApp(appName, connectorToken, response.token)
                    } else {
                        Log.d(TAG, "An error occurred while creating the application.")
                    }
                }

                override fun onError(e: Throwable) {
                    createQueue.remove(connectorToken)
                    e.printStackTrace()
                }

                override fun onComplete() {
                    createQueue.remove(connectorToken)
                    callback()
                }
            })
    }

    fun apiDeleteApp(context: Context, connectorToken: String, callback: () -> Unit) {
        cApi(context) {
            cDeleteApp(context, connectorToken) {
                callback()
            }
        }
    }

    private fun cDeleteApp(context: Context, connectorToken: String, callback: () -> Unit) {
        val appToken = getDb(context).getAppToken(connectorToken)
        mApi.deleteApp(appToken)
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
                    delQueue.remove(connectorToken)
                    e.printStackTrace()
                }

                override fun onComplete() {
                    delQueue.remove(connectorToken)
                    callback()
                }
            })
    }
}