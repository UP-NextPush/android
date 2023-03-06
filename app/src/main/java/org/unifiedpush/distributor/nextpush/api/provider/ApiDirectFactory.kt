package org.unifiedpush.distributor.nextpush.api.provider

import android.content.Context
import okhttp3.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.api.provider.ApiProvider.Companion.mApiEndpoint
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class ApiDirectFactory(val context: Context) : ApiProviderFactory {
    override fun getProviderAndExecute(block: (ApiProvider, then: () -> Unit) -> Unit) {
        val account = getAccount(context) ?: run {
            throw NoProviderException("No account found")
        }
        val url = account.url ?: run {
            throw NoProviderException("No url found")
        }
        val client = account.getAccount(context) as OkHttpClient? ?: run {
            throw NoProviderException("No client found")
        }
        Retrofit.Builder()
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .baseUrl("$url$mApiEndpoint").build()
            .create(ApiProvider::class.java).let {
                block(it){}
            }
    }
}
