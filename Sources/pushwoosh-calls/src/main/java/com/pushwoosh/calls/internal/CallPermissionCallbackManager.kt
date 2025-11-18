package com.pushwoosh.calls.internal

import com.pushwoosh.calls.CallPermissionsCallback
import com.pushwoosh.internal.utils.PWLog

/**
 * Internal manager for handling call permission callbacks.
 * Stores callback temporarily while permission dialog is shown.
 * Callback is automatically cleared after being invoked.
 */
internal object CallPermissionCallbackManager {
    private const val TAG = "CallPermissionCallbackManager"

    @Volatile
    private var callback: CallPermissionsCallback? = null

    /**
     * Sets the callback for the next permission request.
     */
    fun setCallback(callback: CallPermissionsCallback?) {
        this.callback = callback
        PWLog.debug(TAG, "Callback set: ${callback != null}")
    }

    /**
     * Invokes the callback with permission result and clears it.
     */
    fun invokeCallback(
        granted: Boolean,
        grantedPermissions: List<String>,
        deniedPermissions: List<String>
    ) {
        if (callback != null) {
            PWLog.debug(TAG, "Invoking callback: granted=$granted")
            try {
                callback!!.onPermissionResult(granted, grantedPermissions, deniedPermissions)
            } catch (e: Exception) {
                PWLog.error(TAG, "Error invoking callback", e)
            }
        } else {
            PWLog.debug(TAG, "No callback to invoke")
        }
        // Clear the callback after invoking it
        callback = null
    }

    /**
     * Clears the stored callback without invoking it.
     */
    fun clearCallback() {
        callback = null
        PWLog.debug(TAG, "Callback cleared")
    }
}
