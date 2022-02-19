package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import com.nextcloud.android.sso.ui.UiExceptionManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import org.unifiedpush.distributor.nextpush.R

private const val TAG = "AccountUtils"

private const val PREF_NAME = "NextPush"
private const val PREF_DEVICE_ID = "deviceId"
private const val PREF_URL = "url"

object AccountUtils {

    lateinit var ssoAccount: SingleSignOnAccount

    fun nextcloudAppNotInstalledDialog(context: Context) {
        val message = TextView(context)
        val builder = AlertDialog.Builder(context)
        var messageContent = context.getString(R.string.message_missing_nextcloud_app)
        val installIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(context.getString(R.string.uri_market_nextcloud_app))
            )
        messageContent += if (
            installIntent.resolveActivity(context.applicationContext.packageManager) != null
        ) {
            val callback = {
                context.startActivity(
                    Intent.createChooser(
                        installIntent,
                        context.getString(R.string.market_chooser_title)
                    )
                )
            }
            builder.setPositiveButton(context.getString(R.string.install)) { _: DialogInterface, _: Int ->
                callback()
            }
            builder.setNegativeButton(context.getString(R.string.dismiss)) { _: DialogInterface, _: Int ->
            }
            "."
        } else {
            ": " + context.getString(R.string.uri_fdroid_nextcloud_app)
        }
        val s = SpannableString(messageContent)
        Linkify.addLinks(s, Linkify.ALL)
        message.text = s
        message.movementMethod = LinkMovementMethod.getInstance()
        message.setPadding(32, 32, 32, 32)
        builder.setTitle(context.getString(R.string.nextcloud_files_not_found_title))
        builder.setView(message)
        builder.show()
    }

    fun isConnected(context: Context, showDialog: Boolean = false): Boolean {
        try {
            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(context)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            if (showDialog) {
                nextcloudAppNotInstalledDialog(context)
            }
            return false
        } catch (e: NoCurrentAccountSelectedException) {
            Log.d(TAG, "Device is not connected")
            return false
        }
        return true
    }

    fun connect(activity: Activity) {
        try {
            AccountImporter.pickNewAccount(activity)
        } catch (e: NextcloudFilesAppNotInstalledException) {
            nextcloudAppNotInstalledDialog(activity)
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

    fun getDeviceId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_DEVICE_ID, null)
    }

    fun removeDeviceId(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_DEVICE_ID)
            .commit()
    }

    fun saveUrl(context: Context, url: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_URL, url)
            .commit()
    }

    fun getUrl(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_URL, null)
    }

    fun removeUrl(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_URL)
            .commit()
    }
}