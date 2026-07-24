package com.pushwoosh.inapp.ui.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.internal.utils.PWLog

/** Runs an in-app URL action by handing the URL to the system (browser / deep link). */
internal object InAppActionDispatcher {

    private const val TAG = "InAppActionDispatcher"

    fun open(context: Context, action: InAppAction.Url) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            PWLog.error(TAG, "Failed to open in-app action url: ${action.url}", e)
        }
    }
}
