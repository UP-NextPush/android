package org.unifiedpush.distributor.nextpush.api.provider

import android.content.Context
import android.util.Log
import okhttp3.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.api.provider.ApiProvider.Companion.mApiEndpoint
import org.unifiedpush.distributor.nextpush.utils.TAG
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ApiDirectFactory(val context: Context) : ApiProviderFactory {

    private var apiProvider: ApiProvider? = null

    override fun getProviderAndExecute(block: (ApiProvider) -> Unit) {
        val account = getAccount(context) ?: run {
            Log.w(TAG, "No account found")
            return
        }
        val url = account.url ?: run {
            Log.w(TAG, "No url found")
            return
        }
        val client = account.getAccount(context) as OkHttpClient? ?: run {
            Log.w(TAG, "No client found")
            return
        }
        apiProvider?.let(block)
            ?: run {
                Log.d(TAG, "Creating new provider")
                Retrofit.Builder()
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl("$url$mApiEndpoint").build()
                    .create(ApiProvider::class.java).let {
                        apiProvider = it
                        block(it)
                    }
            }
    }

    override fun destroyProvider() {
        // Nothing
    }
}
