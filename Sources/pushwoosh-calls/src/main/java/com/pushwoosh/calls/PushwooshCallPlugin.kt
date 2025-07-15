package com.pushwoosh.calls

import android.content.Context
import android.content.pm.PackageManager
import com.pushwoosh.calls.listener.CallEventListener
import com.pushwoosh.calls.listener.PushwooshCallEventListener
import com.pushwoosh.calls.util.CallPrefs
import com.pushwoosh.internal.Plugin
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.prefs.PrefsProvider
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider
import java.util.Collections

class PushwooshCallPlugin : Plugin {

    companion object {
        @JvmStatic
        val instance: PushwooshCallPlugin by lazy { PushwooshCallPlugin() }
    }

    val callEventListener: CallEventListener
    val callPrefs: CallPrefs

    init {
        callEventListener = resolveCallEventListener()
        callPrefs = CallPrefs()
    }

    override fun init() {
        MessageSystemHandleChainProvider.getMessageSystemChain()
            .addItem(CallNotificationHandler())
    }

    override fun getPrefsMigrationSchemes(prefsProvider: PrefsProvider?):
            MutableCollection<out MigrationScheme> {
        return Collections.emptyList()
    }

    private fun resolveCallEventListener(): CallEventListener {
        return try {
            val context: Context? = AndroidPlatformModule.getApplicationContext()
            if (context != null) {
                val meta = context.packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                    .metaData

                val className = meta?.getString("com.pushwoosh.CALL_EVENT_LISTENER")
                val clazz = className?.let { Class.forName(it) }
                val listener = clazz?.newInstance() as? CallEventListener
                listener ?: PushwooshCallEventListener()
            } else {
                PushwooshCallEventListener()
            }
        } catch (e: Exception) {
            PushwooshCallEventListener()
        }
    }
}
