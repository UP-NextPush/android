package org.unifiedpush.distributor.nextpush.account

import android.content.Context
import android.util.Log
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import org.unifiedpush.distributor.nextpush.utils.TAG

internal const val PREF_NAME = "NextPush"
private const val PREF_DEVICE_ID = "deviceId"
private const val PREF_ACCOUNT_TYPE = "account::type"

enum class AccountType {
    SSO,
    Direct;
    fun toInt(): Int {
        return this.ordinal
    }
}

private fun Int.toAccountType(): AccountType {
    return AccountType.values().getOrNull(this) ?: AccountType.SSO
}

object Account {
    private var account: AccountFactory? = null

    var Context.accountType: AccountType
        get() = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_ACCOUNT_TYPE, 0).toAccountType()
        private set(value) = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(PREF_ACCOUNT_TYPE, value.toInt())
            .apply()

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
                Log.d(TAG, "New account, type=${context.accountType}")
                when (context.accountType) {
                    AccountType.SSO -> {
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
                    AccountType.Direct -> {
                        DirectAccountFactory().apply {
                            initAccount(context)
                            account = this
                        }
                    }
                }
            }
    }

    fun Context.setTypeSSO() {
        account = null
        accountType = AccountType.SSO
        DirectAccountFactory.setCredentials(this, null, null, null)
    }

    fun Context.setTypeDirect(url: String, username: String, password: String) {
        account = null
        accountType = AccountType.Direct
        DirectAccountFactory.setCredentials(this, url, username, password)
    }

    fun isConnected(context: Context): Boolean {
        return getAccount(context)?.initAccount(context) == true
    }
}
