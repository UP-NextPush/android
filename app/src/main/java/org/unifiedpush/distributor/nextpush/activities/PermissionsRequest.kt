package org.unifiedpush.distributor.nextpush.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.unifiedpush.distributor.nextpush.R
import org.unifiedpush.distributor.nextpush.utils.TAG

object PermissionsRequest {

    fun AppCompatActivity.requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                this.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted: $granted")
                    if (!granted) {
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
}
