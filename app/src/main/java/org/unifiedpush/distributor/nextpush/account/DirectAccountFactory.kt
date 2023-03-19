package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import okhttp3.* // ktlint-disable no-wildcard-imports
import org.unifiedpush.distributor.nextpush.activities.StartActivity
import org.unifiedpush.distributor.nextpush.api.provider.ApiProvider.Companion.mApiEndpoint
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val PREF_CONNECTED = "direct_account::connected"
private const val PREF_URL = "direct_account::url"
private const val PREF_USERNAME = "direct_account::username"
private const val PREF_PASSWORD = "direct_account::password"

class DirectAccountFactory : AccountFactory {
    override var name: String? = null
    override var url: String? = null

    private var Context.connected: Boolean
        get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_CONNECTED, false)
        set(value) = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_CONNECTED, value)
            .apply()

    override fun initAccount(context: Context): Boolean {
        url = context.url
        name = context.username

        return context.connected
    }

    override fun connect(activity: Activity) {
        activity.connected = false
        val client = getAccount(activity) as OkHttpClient? ?: return retActivity(activity)
        val url = activity.url ?: return retActivity(activity)

        val request = try {
            Request.Builder()
                .url("$url/$mApiEndpoint/")
                .build()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(activity, "Expected URL scheme 'http' or 'https'", Toast.LENGTH_SHORT).show()
            return retActivity(activity)
        }

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            private val TAG = "DirectAccountCallback"
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Could not connect", e)
                retActivity(activity)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.e(TAG, "Status: ${response.code}")
                activity.connected = response.code == 200
                response.close()
                retActivity(activity)
            }
        })
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        block: (success: Boolean) -> Unit
    ) {
        block(activity.connected)
    }

    override fun getAccount(context: Context): Any? {
        val username = context.username ?: return null
        val password = context.password ?: return null
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .authenticator(DirectAuth(username, password))
            .followRedirects(false)
            .build()
    }

    override fun logout(context: Context) {
        context.connected = false
        setCredentials(context, null, null, null)
    }

    private fun retActivity(activity: Activity) {
        activity.runOnUiThread {
            (activity as StartActivity).onActivityResult(0, 0, null)
        }
    }

    inner class DirectAuth(private val username: String, private val password: String) : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (responseCount(response) >= 3) {
                return null
            }
            val credential = Credentials.basic(username, password)
            return response.request.newBuilder().header("Authorization", credential).build()
        }

        private fun responseCount(_response: Response): Int {
            var response = _response
            var result = 1
            while (response.priorResponse?.also {
                response = it
            } != null
            ) {
                result++
            }
            return result
        }
    }

    companion object {
        private var Context.url: String?
            get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_URL, null)
            set(value) = value?.let {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(PREF_URL, it)
                    .apply()
            } ?: run {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_URL)
                    .apply()
            }

        private var Context.username: String?
            get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_USERNAME, null)
            set(value) = value?.let {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(PREF_USERNAME, it)
                    .apply()
            } ?: run {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_USERNAME)
                    .apply()
            }

        private var Context.password: String?
            get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_PASSWORD, null)
            set(value) = value?.let {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(PREF_PASSWORD, it)
                    .apply()
            } ?: run {
                this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_PASSWORD)
                    .apply()
            }

        fun setCredentials(context: Context, url: String?, username: String?, password: String?) {
            context.url = url
            context.username = username
            context.password = password
        }
    }
}
