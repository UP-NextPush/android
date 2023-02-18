package org.unifiedpush.distributor.nextpush.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.* // ktlint-disable no-wildcard-imports
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.AccountImporter.clearAllAuthTokens
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.ui.UiExceptionManager
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.account.AccountUtils.connect
import org.unifiedpush.distributor.nextpush.account.AccountUtils.isConnected
import org.unifiedpush.distributor.nextpush.account.AccountUtils.nextcloudAppNotInstalledDialog
import org.unifiedpush.distributor.nextpush.account.AccountUtils.ssoAccount
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteDevice
import org.unifiedpush.distributor.nextpush.distributor.Distributor.getDb
import org.unifiedpush.distributor.nextpush.services.RestartWorker
import org.unifiedpush.distributor.nextpush.services.StartService
import org.unifiedpush.distributor.nextpush.utils.TAG
import java.lang.String.format

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private var showLogout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        requestPermissions()
        if (isConnected(this, showDialog = true)) {
            showMain()
        } else {
            findViewById<Button>(R.id.button_connection).setOnClickListener {
                connect(this)
            }
            showStart()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            AccountImporter.onActivityResult(
                requestCode,
                resultCode,
                data,
                this
            ) { account ->
                val context = applicationContext

                SingleAccountHelper.setCurrentAccount(context, account.name)

                // Get the "default" account
                try {
                    ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(context)
                } catch (e: NextcloudFilesAppAccountNotFoundException) {
                    nextcloudAppNotInstalledDialog(context)
                } catch (e: NoCurrentAccountSelectedException) {
                    UiExceptionManager.showDialogForException(context, e)
                }
                showMain()
            }
        } catch (_: AccountImportCancelledException) {}
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showMain() {
        findViewById<ConstraintLayout>(R.id.sub_start).isVisible = false
        findViewById<ConstraintLayout>(R.id.sub_main).isVisible = true
        findViewById<TextView>(R.id.main_account_desc).text =
            format(getString(R.string.main_account_desc), ssoAccount.name)
        showLogout = true
        invalidateOptionsMenu()
        RestartWorker.start(this)
    }

    private fun showStart() {
        findViewById<ConstraintLayout>(R.id.sub_start).isVisible = true
        findViewById<ConstraintLayout>(R.id.sub_main).isVisible = false
        showLogout = false
        invalidateOptionsMenu()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted: $granted")
                    if (granted) {
                        restart()
                    } else {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            Log.d(TAG, "Show POST_NOTIFICATIONS permission rationale")
                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.no_notification_dialog_title))
                                .setMessage(R.string.no_notification_dialog_message)
                                .show()
                        }
                    }
                }.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setListView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_restart).isEnabled = showLogout
        menu.findItem(R.id.action_logout).isEnabled = showLogout
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                RestartWorker.start(this@MainActivity, delay = 0)
            }
        }
        val intentFilter = IntentFilter(StartService.SERVICE_STOPPED_ACTION)
        registerReceiver(receiver, intentFilter)
        StartService.isServiceStarted = false
        val serviceIntent = Intent(this, StartService::class.java)
        this.stopService(serviceIntent)
    }

    private fun logout() {
        val alert: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(
            this
        )
        alert.setTitle(getString(R.string.logout_alert_title))
        alert.setMessage(R.string.logout_alert_content)
        alert.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            clearAllAuthTokens(this)
            AccountImporter.getSharedPreferences(this)
                .edit()
                .remove("PREF_CURRENT_ACCOUNT_STRING")
                .apply()
            deleteDevice(this)
            showStart()
            finish()
            startActivity(intent)
        }
        alert.setNegativeButton(getString(R.string.discard)) { dialog, _ -> dialog.dismiss() }
        alert.show()
    }

    private fun setListView() {
        listView = findViewById(R.id.applications_list)

        val tokenList = emptyList<String>().toMutableList()
        val appList = emptyList<String>().toMutableList()

        getDb(this).let { db ->
            db.listTokens().forEach {
                tokenList.add(it)
                appList.add(db.getPackageName(it) ?: it)
            }
        }

        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            appList
        )

        listView.setOnItemLongClickListener(
            fun(_: AdapterView<*>, _: View, position: Int, _: Long): Boolean {
                val alert: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(
                    this
                )
                alert.setTitle("Unregistering")
                alert.setMessage("Are you sure to unregister ${appList[position]} ?")
                alert.setPositiveButton("YES") { dialog, _ ->
                    val connectorToken = tokenList[position]
                    deleteApp(this, connectorToken) {
                        Log.d(TAG, "Unregistration is finished")
                        this@MainActivity.runOnUiThread {
                            setListView()
                        }
                    }
                    dialog.dismiss()
                }
                alert.setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }
                alert.show()
                return true
            }
        )
    }
}
