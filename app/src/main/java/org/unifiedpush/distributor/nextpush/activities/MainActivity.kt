package org.unifiedpush.distributor.nextpush.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.* // ktlint-disable no-wildcard-imports
import android.widget.AbsListView.MultiChoiceModeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.size
import androidx.core.view.isGone
import androidx.core.view.setPadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.account.Account
import org.unifiedpush.distributor.nextpush.account.Account.getAccount
import org.unifiedpush.distributor.nextpush.account.Account.isConnected
import org.unifiedpush.distributor.nextpush.activities.PermissionsRequest.requestAppPermissions
import org.unifiedpush.distributor.nextpush.activities.StartActivity.Companion.goToStartActivity
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteApp
import org.unifiedpush.distributor.nextpush.distributor.Distributor.deleteDevice
import org.unifiedpush.distributor.nextpush.distributor.Distributor.getDb
import org.unifiedpush.distributor.nextpush.services.FailureHandler
import org.unifiedpush.distributor.nextpush.services.RestartWorker
import org.unifiedpush.distributor.nextpush.services.StartService
import org.unifiedpush.distributor.nextpush.utils.TAG
import org.unifiedpush.distributor.nextpush.utils.getDebugInfo
import java.lang.String.format

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    // if the unregister dialog is shown, we prevent the list to be reset
    private var preventListReset = false
    private var lastClickTime = 0L
    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        this.requestAppPermissions()
        if (!isConnected(this)) {
            goToStartActivity(this)
            finish()
        }

        findViewById<TextView>(R.id.main_account_desc).text =
            format(getString(R.string.main_account_desc), getAccount(this)?.name)
        invalidateOptionsMenu()
        RestartWorker.startPeriodic(this)
        setDebugInformationListener()
    }

    override fun onStart() {
        super.onStart()
        showOptimisationWarning()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (preventListReset) {
                preventListReset = false
            } else {
                setListView()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
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

    private fun showOptimisationWarning() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            findViewById<MaterialCardView>(R.id.card_battery_optimization).isGone = false
            findViewById<Button>(R.id.button_disable_optimisation).setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        } else {
            findViewById<MaterialCardView>(R.id.card_battery_optimization).isGone = true
        }
    }
    private fun restart() {
        Log.d(TAG, "Restarting the Listener")
        FailureHandler.clearFails()
        StartService.stopService {
            RestartWorker.run(this, delay = 0)
        }
    }

    private fun logout() {
        val alert: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(
            this
        )
        alert.setTitle(getString(R.string.logout_alert_title))
        alert.setMessage(R.string.logout_alert_content)
        alert.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            deleteDevice(this) {
                StartService.stopService()
                FailureHandler.clearFails()
            }
            Account.logout(this)
            finish()
            goToStartActivity(this)
        }
        alert.setNegativeButton(getString(R.string.discard)) { dialog, _ -> dialog.dismiss() }
        alert.show()
    }

    private fun setListView() {
        listView = findViewById(R.id.applications_list)

        val appList = emptyList<App>().toMutableList()

        getDb(this).let { db ->
            db.listTokens().forEach {
                appList.add(
                    App(token = it, packageId = db.getPackageName(it) ?: it)
                )
            }
        }

        val editListAdapter = AppListAdapter(
            this,
            R.layout.item_app,
            appList
        )

        listView.adapter = editListAdapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL

        listView.setMultiChoiceModeListener(object : MultiChoiceModeListener {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                editListAdapter.removeSelection()
            }

            override fun onItemCheckedStateChanged(
                mode: ActionMode,
                position: Int,
                id: Long,
                checked: Boolean
            ) {
                val checkedCount = listView.checkedItemCount
                mode.title = "$checkedCount selected"
                editListAdapter.toggleSelection(position)
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
                mode.menuInflater.inflate(R.menu.menu_delete, menu)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                Log.d(TAG, "Action clicked")
                return when (item.itemId) {
                    R.id.action_delete -> {
                        Log.d(TAG, "deleting")
                        val selected = editListAdapter.getSelectedIds()
                        val alert = MaterialAlertDialogBuilder(this@MainActivity)
                        alert.setTitle(getString(R.string.dialog_unregistering_title))
                        alert.setMessage(getString(R.string.dialog_unregistering_content).format(selected.size))
                        alert.setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                            var i = selected.size - 1
                            while (i >= 0) {
                                if (selected.valueAt(i)) {
                                    editListAdapter.getItem(selected.keyAt(i))?.let {
                                        deleteApp(this@MainActivity, it.token) {
                                            Log.d(TAG, "${it.packageId} unregistered")
                                            editListAdapter.remove(it)
                                            this@MainActivity.runOnUiThread {
                                                setListView()
                                            }
                                        }
                                    }
                                    i--
                                }
                            }
                            preventListReset = false
                            dialog.dismiss()
                            mode.finish()
                        }
                        alert.setNegativeButton(getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
                        alert.setOnCancelListener {
                            Log.d(TAG, "Cancelled")
                        }
                        preventListReset = true
                        alert.show()
                        true
                    }
                    else -> false
                }
            }
        })
    }

    private fun setDebugInformationListener() {
        findViewById<TextView>(R.id.main_account_title).setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) {
                clickCount++
                if (clickCount == 5) {
                    val scale = resources.displayMetrics.density
                    val dpAsPixels = (16 * scale + 0.5f)
                    val showText = TextView(this)
                        .apply {
                            setTextIsSelectable(true)
                            text = getDebugInfo()
                            setPadding(dpAsPixels.toInt())
                        }
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Debug information")
                        .setView(showText)
                        .setCancelable(false)
                        .setPositiveButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()

                    clickCount = 0 // Reset count after showing the dialog
                }
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime
        }
    }
    companion object {
        fun goToMainActivity(context: Context) {
            val intent = Intent(
                context,
                MainActivity::class.java
            )
            context.startActivity(intent)
        }
    }
}
