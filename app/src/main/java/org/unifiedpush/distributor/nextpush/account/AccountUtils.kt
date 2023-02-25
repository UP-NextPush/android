package org.unifiedpush.distributor.nextpush.account

import android.content.Context

private const val PREF_NAME = "NextPush"
private const val PREF_DEVICE_ID = "deviceId"
private const val PREF_URL = "url"

object AccountUtils {

    fun saveDeviceId(context: Context, deviceId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_DEVICE_ID, deviceId)
            .apply()
    }

    fun getDeviceId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_DEVICE_ID, null)
    }

    fun removeDeviceId(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_DEVICE_ID)
            .apply()
    }

    fun saveUrl(context: Context, url: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_URL, url)
            .apply()
    }

    fun getUrl(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_URL, null)
    }

    fun removeUrl(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_URL)
            .apply()
    }
}
