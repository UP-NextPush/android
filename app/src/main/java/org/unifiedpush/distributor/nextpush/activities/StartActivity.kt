package org.unifiedpush.distributor.nextpush.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.account.Account
import org.unifiedpush.distributor.nextpush.activities.MainActivity.Companion.goToMainActivity
import org.unifiedpush.distributor.nextpush.activities.PermissionsRequest.requestAppPermissions

class StartActivity : AppCompatActivity() {
    private var onResult: ((activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, block: (success: Boolean) -> Unit) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        this.requestAppPermissions()
        if (Account.isConnected(this)) {
            goToMainActivity(this)
            finish()
        }
        findViewById<Button>(R.id.sso_connection).setOnClickListener {
            Account.getAccount(this, uninitialized = true)?.let {
                onResult = { activity: Activity, i: Int, i1: Int, intent: Intent?, block: (success: Boolean) -> Unit ->
                    it.onActivityResult(activity, i, i1, intent, block)
                }
                it.connect(this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onResult?.let {
            it(this, requestCode, resultCode, data) { success ->
                if (success) {
                    goToMainActivity(this)
                    finish()
                }
            }
        }
    }

    companion object {
        fun goToStartActivity(context: Context) {
            val intent = Intent(
                context,
                StartActivity::class.java
            )
            context.startActivity(intent)
        }
    }
}
