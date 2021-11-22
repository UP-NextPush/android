package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.util.Log
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import com.nextcloud.android.sso.ui.UiExceptionManager

private const val TAG = "AccountUtils"

const val PREF_NAME = "NextPush"
const val PREF_DEVICE_ID = "deviceId"

lateinit var ssoAccount: SingleSignOnAccount

fun isConnected(context: Context) : Boolean {
    try {
        ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(context)
    } catch (e: NextcloudFilesAppAccountNotFoundException) {
        UiExceptionManager.showDialogForException(context, e)
    } catch (e: NoCurrentAccountSelectedException) {
        Log.d(TAG,"Device is not connected")
        return false
    }
    return true
}

fun connect(activity: Activity) {
    try {
        AccountImporter.pickNewAccount(activity)
    } catch (e: NextcloudFilesAppNotInstalledException) {
        UiExceptionManager.showDialogForException(activity, e)
    } catch (e: AndroidGetAccountsPermissionNotGranted) {
        UiExceptionManager.showDialogForException(activity, e)
    }
}

fun saveDeviceId(context: Context, deviceId: String) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_DEVICE_ID, deviceId)
        .commit()
}

fun getDeviceId(context: Context) : String? {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(PREF_DEVICE_ID,null)
}

fun removeDeviceId(context: Context) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(PREF_DEVICE_ID)
        .commit()
}
