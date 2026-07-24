package com.pushwoosh.inapp.ui

import android.content.Context
import com.pushwoosh.inapp.ui.presentation.InAppActivityChannel
import com.pushwoosh.inapp.ui.presentation.InAppFrequencyStore
import com.pushwoosh.inapp.ui.presentation.InAppOverlayChannel
import com.pushwoosh.inapp.ui.presentation.InAppQueueManager
import com.pushwoosh.inapp.ui.presentation.InAppRoutingChannel
import com.pushwoosh.internal.platform.AndroidPlatformModule
import java.lang.ref.WeakReference

/**
 * Lazily-built singletons for the module (IoC holder, mirrors the SDK's `XxxModule`
 * pattern). Holds the shared queue manager, frequency store and the host-set delegate.
 */
internal object InAppModule {

    @Volatile
    private var queueManagerInstance: InAppQueueManager? = null

    @Volatile
    private var frequencyStoreInstance: InAppFrequencyStore? = null

    // Held weakly (iOS parity): the host must keep its own strong reference, or an anonymous
    // delegate is collected and callbacks silently stop.
    @Volatile
    private var delegateRef: WeakReference<InAppMessageDelegate>? = null

    var delegate: InAppMessageDelegate?
        get() = delegateRef?.get()
        set(value) {
            delegateRef = value?.let { WeakReference(it) }
        }

    fun frequencyStore(context: Context): InAppFrequencyStore =
        frequencyStoreInstance ?: synchronized(this) {
            frequencyStoreInstance ?: InAppFrequencyStore(context.applicationContext)
                .also { frequencyStoreInstance = it }
        }

    fun queueManager(context: Context): InAppQueueManager =
        queueManagerInstance ?: synchronized(this) {
            queueManagerInstance
                ?: InAppQueueManager(
                        frequencyStore(context),
                        { AndroidPlatformModule.isApplicationInForeground() }) { delegate }
                    .apply {
                        val appContext = context.applicationContext
                        channel = InAppRoutingChannel(
                            InAppActivityChannel(appContext),
                            InAppOverlayChannel(this)
                        )
                    }
                    .also { queueManagerInstance = it }
        }
}
