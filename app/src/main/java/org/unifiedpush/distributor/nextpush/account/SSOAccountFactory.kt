package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.exceptions.* // ktlint-disable no-wildcard-imports
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.ui.UiExceptionManager
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.utils.TAG

class SSOAccountFactory : AccountFactory {
    override var name: String? = null
    override var url: String? = null

    override fun initAccount(context: Context): Boolean {
        try {
            SingleAccountHelper.getCurrentSingleSignOnAccount(context).let {
                name = it.name
                url = it.url
            }
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            return false
        } catch (e: NoCurrentAccountSelectedException) {
            Log.d(TAG, "Device is not connected")
            return false
        }
        return true
    }

    override fun connect(activity: Activity) {
        Log.d(TAG, "Starting ResultActivity")
        try {
            AccountImporter.pickNewAccount(activity)
        } catch (e: NextcloudFilesAppNotInstalledException) {
            nextcloudAppNotInstalledDialog(activity)
        } catch (e: AndroidGetAccountsPermissionNotGranted) {
            UiExceptionManager.showDialogForException(activity, e)
        }
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        block: (success: Boolean) -> Unit
    ) {
        var success = false
        try {
            AccountImporter.onActivityResult(
                requestCode,
                resultCode,
                data,
                activity
            ) { account ->
                SingleAccountHelper.setCurrentAccount(activity.applicationContext, account.name)
                success = true
            }
        } catch (_: AccountImportCancelledException) {}
        block(success)
    }

    override fun getAccount(context: Context): Any? {
        return try {
            SingleAccountHelper.getCurrentSingleSignOnAccount(context)
        } catch (e: NextcloudFilesAppAccountNotFoundException) {
            Log.w(TAG, "Nextcloud File is not found")
            null
        } catch (e: NoCurrentAccountSelectedException) {
            Log.w(TAG, "No account selected")
            null
        }
    }

    override fun logout(context: Context) {
        AccountImporter.clearAllAuthTokens(context)
        AccountImporter.getSharedPreferences(context)
            .edit()
            .remove("PREF_CURRENT_ACCOUNT_STRING")
            .apply()
    }

    private fun nextcloudAppNotInstalledDialog(context: Context) {
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
        Linkify.addLinks(s, Linkify.WEB_URLS)
        message.text = s
        message.movementMethod = LinkMovementMethod.getInstance()
        message.setPadding(32, 32, 32, 32)
        builder.setTitle(context.getString(R.string.nextcloud_files_not_found_title))
        builder.setView(message)
        builder.show()
    }
}
