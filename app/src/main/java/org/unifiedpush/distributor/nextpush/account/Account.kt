package org.unifiedpush.distributor.nextpush.account

import android.content.Context
import android.util.Log
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import org.unifiedpush.distributor.nextpush.utils.TAG

private const val PREF_NAME = "NextPush"
private const val PREF_DEVICE_ID = "deviceId"
object Account {
    private var account: AccountFactory? = null

    var Context.deviceId: String?
        get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_DEVICE_ID, null)
        set(value) {
            this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .apply {
                    value?.let {
                        putString(PREF_DEVICE_ID, it)
                    } ?: run {
                        remove(PREF_DEVICE_ID)
                    }
                }.apply()
        }

    fun getAccount(context: Context, uninitialized: Boolean = false): AccountFactory? {
        return account
            ?: run {
                try {
                    SSOAccountFactory().apply {
                        initAccount(context)
                        account = this
                    }
                } catch (e: NextcloudFilesAppAccountNotFoundException) {
                    Log.w(TAG, "Nextcloud application is not found")
                    null
                } catch (e: NoCurrentAccountSelectedException) {
                    if (uninitialized) {
                        SSOAccountFactory()
                    } else {
                        null
                    }
                }
            }
    }

    fun isConnected(context: Context): Boolean {
        return getAccount(context)?.isConnected(context) == true
    }
}
