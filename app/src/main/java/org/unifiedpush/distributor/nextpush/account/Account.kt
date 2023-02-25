package org.unifiedpush.distributor.nextpush.account

import android.content.Context
import android.util.Log
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import org.unifiedpush.distributor.nextpush.utils.TAG

object Account {
    private var account: AccountFactory? = null

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
