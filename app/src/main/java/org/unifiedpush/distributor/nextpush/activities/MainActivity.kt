package org.unifiedpush.distributor.nextpush.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.* // ktlint-disable no-wildcard-imports
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.setPadding
import com.google.android.material.card.MaterialCardView
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
            setListView()
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
                    AlertDialog.Builder(this)
                        .setTitle("Debug information")
                        .setView(showText)
                        .setCancelable(false)
                        .setPositiveButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()

                    clickCount = 0 // Réinitialisez le compteur après l'affichage de la popup
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
