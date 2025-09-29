package com.pushwoosh.calls.ui

import android.content.pm.PackageManager
import android.os.Build
import com.pushwoosh.calls.PushwooshCallSettings
import com.pushwoosh.calls.util.CallPrefs
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.internal.utils.PermissionActivity

class PhoneNumbersPermissionActivity : PermissionActivity() {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        PWLog.debug(TAG, "onRequestPermissionsResult")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (requestCode) {
                REQUEST_CODE -> {
                    handlePermissionsResult(permissions, grantResults)
                    
                    val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    val permissionStatus = if (allPermissionsGranted) {
                        CallPrefs.PERMISSION_STATUS_GRANTED
                    } else {
                        CallPrefs.PERMISSION_STATUS_DENIED
                    }
                    
                    PushwooshCallUtils.updateCallPermissionStatusAndRegisterAccount(permissionStatus)
                }

                else -> PWLog.warn(TAG, "Unrecognized request code $requestCode")
            }
        }
        finish()
    }
}