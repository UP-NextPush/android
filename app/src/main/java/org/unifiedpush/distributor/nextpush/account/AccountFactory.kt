package org.unifiedpush.distributor.nextpush.account

import android.app.Activity
import android.content.Context
import android.content.Intent

interface AccountFactory {
    var name: String?
    var url: String?
    fun initAccount(context: Context): Boolean
    fun connect(activity: Activity)
    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?, block: (success: Boolean) -> Unit)
    fun getAccount(context: Context): Any?
    fun logout(context: Context)
}
