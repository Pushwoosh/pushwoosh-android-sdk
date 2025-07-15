package com.pushwoosh.calls.ui

import android.os.Build
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
                    PushwooshCallUtils.registerPhoneAccount()
                }

                else -> PWLog.warn(TAG, "Unrecognized request code $requestCode")
            }
        }
        finish()
    }
}