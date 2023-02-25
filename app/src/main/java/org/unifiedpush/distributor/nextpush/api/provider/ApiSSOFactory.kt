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
    private var apiProvider: ApiProvider? = null
    private lateinit var nextcloudAPI: NextcloudAPI

    override fun getProviderAndExecute(block: (ApiProvider) -> Unit) {
        val account = getAccount(context) ?: run {
            Log.w(TAG, "No account found")
            return
        }
        apiProvider?.let(block)
            ?: run {
                Log.d(TAG, "Creating new provider")
                val ssoApiCallback = object : NextcloudAPI.ApiConnectedListener {
                    override fun onConnected() {
                        Log.d(TAG, "Api connected.")
                        NextcloudRetrofitApiBuilder(nextcloudAPI, ApiProvider.mApiEndpoint)
                            .create(ApiProvider::class.java).let {
                                apiProvider = it
                                block(it)
                            }
                    }

                    override fun onError(ex: Exception) {
                        Log.d(TAG, "Cannot connect to API: ex = [$ex]")
                    }
                }
                nextcloudAPI = NextcloudAPI(
                    context,
                    account.getAccount(context) as SingleSignOnAccount,
                    GsonBuilder().create(),
                    ssoApiCallback
                )
            }
    }

    override fun destroyProvider() {
        nextcloudAPI.stop()
    }
}
