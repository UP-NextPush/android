package org.unifiedpush.distributor.nextpush.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.distributor.MessagingDatabase
import org.unifiedpush.distributor.nextpush.distributor.getEndpoint
import org.unifiedpush.distributor.nextpush.distributor.sendEndpoint

class SettingsActivity : AppCompatActivity() {
    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("Config", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_settings)
        val address = prefs?.getString("address", "")
        val proxy = prefs?.getString("proxy", "")
        findViewById<EditText>(R.id.settings_address_value).setText(address)
        findViewById<EditText>(R.id.settings_proxy_value).setText(proxy)
        val btn = findViewById<View>(R.id.settings_save_button)
        btn.setOnClickListener { v -> save(v) }
    }

    fun save(view: View){
        val address = findViewById<EditText>(R.id.settings_address_value).text.toString()
        val proxy = findViewById<EditText>(R.id.settings_proxy_value).text.toString()
        Log.i("save",address)
        val editor = prefs!!.edit()
        editor.putString("address", address)
        editor.putString("proxy", proxy)
        editor.commit()
        val db = MessagingDatabase(this)
        val tokenList = db.listTokens()
        db.close()
        tokenList.forEach {
            sendEndpoint(this, it, getEndpoint(this, it))
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}