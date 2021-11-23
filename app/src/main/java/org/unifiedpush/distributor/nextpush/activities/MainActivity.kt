package org.unifiedpush.distributor.nextpush.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.ui.UiExceptionManager
import org.unifiedpush.distributor.nextpush.services.StartService
import com.nextcloud.android.sso.AccountImporter.IAccountAccessGranted

import com.nextcloud.android.sso.api.NextcloudAPI.ApiConnectedListener

import com.nextcloud.android.sso.helper.SingleAccountHelper

import com.nextcloud.android.sso.model.SingleSignOnAccount
import java.lang.Exception
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.nextcloud.android.sso.AccountImporter.clearAllAuthTokens
import com.nextcloud.android.sso.exceptions.*
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.account.isConnected
import org.unifiedpush.distributor.nextpush.account.connect
import org.unifiedpush.distributor.nextpush.account.ssoAccount
import org.unifiedpush.distributor.nextpush.api.ApiUtils
import org.unifiedpush.distributor.nextpush.distributor.sendUnregistered
import org.unifiedpush.distributor.nextpush.distributor.MessagingDatabase
import java.lang.String.format

private const val TAG = "NextPush-MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var listView : ListView
    private var showLogout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (isConnected(this)) {
            showMain()
        } else {
            findViewById<Button>(R.id.button_connection).setOnClickListener {
                connect(this)
            }
            showStart()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            AccountImporter.onActivityResult(
                requestCode,
                resultCode,
                data,
                this,
                object : IAccountAccessGranted {
                    var callback: ApiConnectedListener = object : ApiConnectedListener {
                        override fun onConnected() {}

                        override fun onError(ex: Exception) {
                            Log.e(TAG, "Cannot get account access", ex)
                        }
                    }

                    override fun accountAccessGranted(account: SingleSignOnAccount) {
                        val context = applicationContext

                        // As this library supports multiple accounts we created some helper methods if you only want to use one.
                        // The following line stores the selected account as the "default" account which can be queried by using
                        // the SingleAccountHelper.getCurrentSingleSignOnAccount(context) method
                        SingleAccountHelper.setCurrentAccount(context, account.name)

                        // Get the "default" account
                        try {
                            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(context)
                        } catch (e: NextcloudFilesAppAccountNotFoundException) {
                            UiExceptionManager.showDialogForException(context, e)
                        } catch (e: NoCurrentAccountSelectedException) {
                            UiExceptionManager.showDialogForException(context, e)
                        }
                        showMain()
                    }
                })
        } catch (e: AccountImportCancelledException) {}
    }

    private fun showMain() {
        findViewById<ConstraintLayout>(R.id.sub_start).isVisible = false
        findViewById<ConstraintLayout>(R.id.sub_main).isVisible = true
        findViewById<TextView>(R.id.main_account_desc).text =
            format(getString(R.string.main_account_desc), ssoAccount.name)
        showLogout = true
        invalidateOptionsMenu()
        startListener()
    }

    private fun showStart() {
        findViewById<ConstraintLayout>(R.id.sub_start).isVisible = true
        findViewById<ConstraintLayout>(R.id.sub_main).isVisible = false
        showLogout = false
        invalidateOptionsMenu()
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) {
            setListView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_logout).isVisible = showLogout
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_restart -> {
                restart()
                return true
            }
            R.id.action_logout -> {
                logout()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun restart() {
        Log.d(TAG, "Restarting the Listener")
        val serviceIntent = Intent(this, StartService::class.java)
        this.stopService(serviceIntent)
        startListener()
    }

    private fun logout() {
        val alert: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(
            this)
        alert.setTitle(getString(R.string.logout_alert_title))
        alert.setMessage(R.string.logout_alert_content)
        alert.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            clearAllAuthTokens(this)
            AccountImporter.getSharedPreferences(this)
                .edit()
                .remove("PREF_CURRENT_ACCOUNT_STRING")
                .apply()
            ApiUtils().deleteDevice(this)
            showStart()
            finish();
            startActivity(intent);
        }
        alert.setNegativeButton(getString(R.string.discard)) { dialog, _ -> dialog.dismiss() }
        alert.show()
    }

    private fun startListener(){
        Log.d(TAG, "Starting the Listener")
        val serviceIntent = Intent(this, StartService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent)
        }else{
            this.startService(serviceIntent)
        }
    }

    private fun setListView(){
        listView = findViewById<ListView>(R.id.applications_list)
        val db = MessagingDatabase(this)
        val tokenList = db.listTokens().toMutableList()
        val appList = emptyArray<String>().toMutableList()
        tokenList.forEach {
            appList.add(db.getPackageName(it))
        }
        db.close()
        listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                appList
        )
        listView.setOnItemLongClickListener(
                fun(parent: AdapterView<*>, v: View, position: Int, id: Long): Boolean {
                    val alert: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(
                            this)
                    alert.setTitle("Unregistering")
                    alert.setMessage("Are you sure to unregister ${appList[position]} ?")
                    alert.setPositiveButton("YES") { dialog, _ ->
                        val connectorToken = tokenList[position]
                        sendUnregistered(this, connectorToken)
                        val db = MessagingDatabase(this)
                        val appToken = db.getAppToken(connectorToken)
                        db.unregisterApp(connectorToken)
                        db.close()
                        ApiUtils().deleteApp(this, appToken) {
                            Log.d(TAG,"Unregistration is finished")
                        }
                        tokenList.removeAt(position)
                        appList.removeAt(position)
                        dialog.dismiss()
                    }
                    alert.setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }
                    alert.show()
                    return true
                }
        )
    }
}
