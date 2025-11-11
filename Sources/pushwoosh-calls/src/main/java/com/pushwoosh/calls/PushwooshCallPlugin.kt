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
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider
import java.util.Collections

/**
 * Core plugin for managing Pushwoosh VoIP call functionality in the Android SDK.
 *
 * This singleton plugin handles VoIP push notifications, manages call lifecycle events,
 * and provides access to call-related preferences and custom event listeners.
 *
 * **Custom Listener Setup:**
 * To use a custom [CallEventListener], add this to AndroidManifest.xml:
 * ```xml
 * <meta-data
 *     android:name="com.pushwoosh.CALL_EVENT_LISTENER"
 *     android:value="com.example.MyCallEventListener" />
 * ```
 * If not specified, [PushwooshCallEventListener] is used as default.
 *
 * **Usage:**
 * ```kotlin
 * val plugin = PushwooshCallPlugin.instance
 * val listener = plugin.callEventListener
 * val prefs = plugin.callPrefs
 * ```
 */
class PushwooshCallPlugin : Plugin {

    companion object {
        /**
         * Singleton instance of the plugin.
         *
         * Thread-safe lazy initialization ensures single instance across the SDK.
         */
        @JvmStatic
        val instance: PushwooshCallPlugin by lazy { PushwooshCallPlugin() }
    }

    /**
     * Listener for VoIP call lifecycle events (answer, reject, disconnect, etc.).
     *
     * Resolved at initialization from AndroidManifest.xml or defaults to [PushwooshCallEventListener].
     * Implements callbacks for call actions and Android system events (ConnectionService, InCallService).
     */
    val callEventListener: CallEventListener

    /**
     * Preferences manager for call-related settings.
     *
     * Provides access to phone account names, notification channel names,
     * custom ringtones, and VoIP permission status.
     */
    val callPrefs: CallPrefs

    init {
        callEventListener = resolveCallEventListener()
        callPrefs = CallPrefs()
    }

    /**
     * Initializes the plugin by registering the call notification handler.
     *
     * Adds [CallNotificationHandler] to the SDK's message processing chain
     * to intercept and handle VoIP push notifications.
     */
    override fun init() {
        PWLog.noise("PushwooshCallPlugin", "init pushwoosh-call-plugin")
        MessageSystemHandleChainProvider.getMessageSystemChain()
            .addItem(CallNotificationHandler())
    }

    override fun getPrefsMigrationSchemes(prefsProvider: PrefsProvider?):
            MutableCollection<out MigrationScheme> {
        return Collections.emptyList()
    }

    /**
     * Resolves the custom call event listener from AndroidManifest.xml metadata.
     *
     * Looks for `com.pushwoosh.CALL_EVENT_LISTENER` meta-data key and instantiates
     * the specified class. Falls back to [PushwooshCallEventListener] if not found,
     * invalid, or if any error occurs during instantiation.
     *
     * @return Custom [CallEventListener] implementation or default [PushwooshCallEventListener]
     */
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
