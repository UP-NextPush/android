package org.unifiedpush.distributor.nextpush.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.google.android.material.textfield.TextInputEditText
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.account.Account
import org.unifiedpush.distributor.nextpush.account.Account.setTypeDirect
import org.unifiedpush.distributor.nextpush.account.Account.setTypeSSO
import org.unifiedpush.distributor.nextpush.activities.MainActivity.Companion.goToMainActivity
import org.unifiedpush.distributor.nextpush.activities.PermissionsRequest.requestAppPermissions
import org.unifiedpush.distributor.nextpush.utils.TAG

class StartActivity : AppCompatActivity() {
    private var onResult: ((activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, block: (success: Boolean) -> Unit) -> Unit)? = null
    private var passwordIsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        this.requestAppPermissions()
        if (Account.isConnected(this)) {
            goToMainActivity(this)
            finish()
        }
        findViewById<Button>(R.id.btn_sso_login).setOnClickListener {
            setTypeSSO()
            login()
        }
        findViewById<Button>(R.id.btn_manual_login).setOnClickListener {
            val url = findViewById<EditText>(R.id.edt_url).text.toString().let {
                if (it.last() != '/') {
                    "$it/"
                } else {
                    it
                }
            }
            val username = findViewById<EditText>(R.id.edt_username).text.toString()
            val password = findViewById<EditText>(R.id.edt_password).text.toString()
            setTypeDirect(url, username, password)
            login()
        }
        findViewById<TextView>(R.id.manual_login).setOnClickListener {
            findViewById<RelativeLayout>(R.id.manual_login_wrapper).apply {
                isGone = !isGone
            }
        }
        findViewById<ImageView>(R.id.ic_show_password).setOnClickListener {
            passwordIsVisible = !passwordIsVisible
            findViewById<TextInputEditText>(R.id.edt_password).inputType =
                when (passwordIsVisible) {
                    false -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    true -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onResult?.let {
            it(this, requestCode, resultCode, data) { success ->
                Log.d(TAG, "Connection succeed=$success")
                if (success) {
                    goToMainActivity(this)
                    finish()
                }
            }
        }
    }

    private fun login() {
        Account.getAccount(this, uninitialized = true)?.let {
            onResult = { activity: Activity, i: Int, i1: Int, intent: Intent?, block: (success: Boolean) -> Unit ->
                it.onActivityResult(activity, i, i1, intent, block)
            }
            it.connect(this)
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
