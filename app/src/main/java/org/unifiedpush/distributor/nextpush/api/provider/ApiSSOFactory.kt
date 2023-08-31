package org.unifiedpush.distributor.nextpush.api.provider

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.nextcloud.android.sso.api.NextcloudAPI
import com.nextcloud.android.sso.model.SingleSignOnAccount
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import retrofit2.NextcloudRetrofitApiBuilder

class ApiSSOFactory(val context: Context) : ApiProviderFactory {

    private val TAG = ApiSSOFactory::class.java.simpleName

    override fun getProviderAndExecute(block: (ApiProvider, then: () -> Unit) -> Unit) {
        var nextcloudAPI: NextcloudAPI? = null
        val account = getAccount(context) ?: run {
            throw NoProviderException("No account found")
        }
        val client = account.getAccount(context) as SingleSignOnAccount? ?: run {
            throw NoProviderException("No client found")
        }
        val ssoApiCallback = object : NextcloudAPI.ApiConnectedListener {
            override fun onConnected() {
                Log.d(TAG, "Api connected.")
                nextcloudAPI?.let { nextcloudAPI ->
                    NextcloudRetrofitApiBuilder(nextcloudAPI, ApiProvider.mApiEndpoint)
                        .create(ApiProvider::class.java).let {
                            block(it) {
                                nextcloudAPI.close()
                            }
                        }
                }
            }

            override fun onError(ex: Exception) {
                Log.d(TAG, "Cannot connect to API: ex = [$ex]")
            }
        }
        nextcloudAPI = NextcloudAPI(
            context,
            client,
            GsonBuilder().create(),
            ssoApiCallback
        )
    }
}
