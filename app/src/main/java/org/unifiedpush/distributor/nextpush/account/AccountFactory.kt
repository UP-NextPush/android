package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.content.Intent

interface AccountFactory {
    val apiFactory: Class<*>
    val name: String
    val url: String
    fun initAccount(context: Context)
    fun isConnected(context: Context): Boolean
    fun connect(activity: Activity)
    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, block: (success: Boolean) -> Unit)
    fun getAccount(context: Context): Any?
}
