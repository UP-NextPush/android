package org.unifiedpush.distributor.nextpush.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

private const val U_TAG = "PackageUtils"

fun Context.getApplicationName(packageId: String): String? {
    try {
        val ai = if (Build.VERSION.SDK_INT >= 33) {
            this.packageManager.getApplicationInfo(
                packageId,
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong()
                )
            )
        } else {
            this.packageManager.getApplicationInfo(packageId, 0)
        }
        return this.packageManager.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(U_TAG, "Could not resolve app name", e)
        return null
    }
}
