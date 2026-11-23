package com.pushwoosh.inline.internal.view

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.pushwoosh.internal.utils.PWLog

/** Hands a banner link to the system; the banner stays in its slot. Main thread only. */
internal object InlineLinkOpener {
    private const val TAG = "InlineLinkOpener"
    private const val FILE_SCHEME = "file"

    /**
     * Opens [uri] with `ACTION_VIEW` from the Activity behind [context], or as a new task without one;
     * a `file:` link is refused and a link nothing can open is logged.
     */
    fun open(context: Context, uri: String) {
        val parsed = Uri.parse(uri)
        if (parsed.scheme.equals(FILE_SCHEME, ignoreCase = true)) {
            PWLog.warn(TAG, "Inline banner link '$uri' to a local file is ignored")
            return
        }
        val activity = context.findActivity()
        val intent = Intent(Intent.ACTION_VIEW, parsed)
        if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            (activity ?: context).startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            PWLog.warn(TAG, "Nothing on the device opens '$uri'")
        } catch (e: SecurityException) {
            PWLog.warn(TAG, "Opening '$uri' is not permitted: ${e.message}")
        } catch (e: RuntimeException) {
            PWLog.warn(TAG, "Opening '$uri' failed: ${e.message}")
        }
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
