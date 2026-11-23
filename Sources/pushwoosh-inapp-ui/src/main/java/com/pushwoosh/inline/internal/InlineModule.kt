package com.pushwoosh.inline.internal

import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.pushwoosh.inapp.inline.InlineContentHandlerProvider
import com.pushwoosh.inline.PushwooshInline
import com.pushwoosh.inline.PushwooshInlineBanner
import com.pushwoosh.inline.PushwooshInlineListener
import com.pushwoosh.inline.internal.net.CoreInlineEventSender
import com.pushwoosh.inline.internal.net.CoreInlineTransport
import com.pushwoosh.inline.internal.net.InlineEndpoint
import com.pushwoosh.inline.internal.net.InlineEventQueue
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.internal.event.AppIdChangedEvent
import com.pushwoosh.internal.event.DeviceDataRemovedEvent
import com.pushwoosh.internal.event.Event
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.event.EventListener
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent
import com.pushwoosh.internal.event.Subscription
import com.pushwoosh.internal.event.UserIdChangeRefusedEvent
import com.pushwoosh.internal.event.UserIdUpdatedEvent
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.ApplicationOpenDetector
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.repository.RepositoryModule
import java.io.File
import java.util.concurrent.Executors

/**
 * Wires inline slots into the SDK. [install] runs during SDK start and only subscribes to core
 * signals; the manager, its banners cache and the event queue are built on first use (the first
 * foreground at the latest), from the files under `noBackupFilesDir/pushwoosh/`. Signals about the
 * user or the app code are dropped while nothing is built: the cache is checked against the current
 * owner when it is loaded. So are server communication starts and device data removals: the queue
 * built later sends its events at once, and drops them when core refuses a removed device.
 */
internal object InlineModule {
    private const val TAG = "InlineModule"
    private const val DIRECTORY = "pushwoosh"
    private const val BANNERS_FILE = "inline-banners.json"
    private const val EVENTS_FILE = "inline-events.json"

    private class Instance(
        val manager: InlineManager,
        val queue: InlineEventQueue?,
        val monitor: InlineNetworkMonitor?,
    )

    @Volatile
    private var instance: Instance? = null

    @Volatile
    private var installed = false

    private val subscriptions = mutableListOf<Subscription<*>>()

    /** Mounted slot views, told when [PushwooshInline.colorScheme] changes. Main thread only. */
    val colorSchemeListeners: MutableSet<() -> Unit> = LinkedHashSet()

    /** Mounted slot views, told when the app comes to the foreground. Main thread only. */
    val foregroundListeners: MutableSet<() -> Unit> = LinkedHashSet()

    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        subscribe(ApplicationOpenDetector.ApplicationMovedToForegroundEvent::class.java) { onForeground() }
        subscribe(ApplicationOpenDetector.ApplicationMovedToBackgroundEvent::class.java) { instance?.manager?.onBackground() }
        subscribe(UserIdUpdatedEvent::class.java) { instance?.manager?.onUserConfirmed() }
        subscribe(UserIdChangeRefusedEvent::class.java) { instance?.manager?.onUserRefused() }
        subscribe(AppIdChangedEvent::class.java) { instance?.manager?.onAppCodeChanged() }
        subscribe(ServerCommunicationStartedEvent::class.java) { instance?.queue?.flush() }
        subscribe(DeviceDataRemovedEvent::class.java) { instance?.queue?.clear() }
        InlineContentHandlerProvider.set { inline, userId, appCode -> managerOrNull()?.onEventAnswer(inline, userId, appCode) }
    }

    /** The manager, built on first call; `null` while the SDK has no application context. Main thread only. */
    @MainThread
    fun managerOrNull(): InlineManager? = obtain(startSession = false)?.manager

    @VisibleForTesting
    @Synchronized
    fun resetForTest(manager: InlineManager?, queue: InlineEventQueue? = null) {
        subscriptions.forEach { it.unsubscribe() }
        subscriptions.clear()
        installed = false
        InlineContentHandlerProvider.set(null)
        instance?.monitor?.stop()
        manager?.listener = ForwardingListener
        instance = manager?.let { Instance(it, queue, null) }
        colorSchemeListeners.clear()
        foregroundListeners.clear()
    }

    private fun <T : Event> subscribe(event: Class<T>, listener: EventListener<T>) {
        subscriptions += EventBus.subscribe(event, listener)
    }

    private fun onForeground() {
        val current = instance
        if (current == null) {
            obtain(startSession = true)
        } else {
            current.manager.onForeground()
            current.queue?.flush()
        }
        foregroundListeners.toList().forEach { it() }
    }

    private fun onNetworkAvailable() {
        val current = instance ?: return
        current.manager.onNetworkAvailable()
        current.queue?.flush(networkBack = true)
    }

    /** A process can be in the foreground before the first build: its session starts here then. */
    private fun obtain(startSession: Boolean): Instance? {
        instance?.let { return it }
        val context = AndroidPlatformModule.getApplicationContext()
        if (context == null) {
            PWLog.warn(TAG, "Inline slots are not available before Pushwoosh is initialized")
            return null
        }
        val created = create(context)
        instance = created
        created.queue?.flush()
        if (startSession || AndroidPlatformModule.isApplicationInForeground()) created.manager.onForeground()
        created.monitor?.start()
        return created
    }

    private fun create(context: Context): Instance {
        val directory = File(context.noBackupFilesDir ?: context.filesDir, DIRECTORY)
        val writer = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "PushwooshInlineIO").apply { isDaemon = true } }
        val metaData: () -> Bundle? = { AndroidPlatformModule.getAppInfoProvider()?.applicationInfo?.metaData }
        val monitor = InlineNetworkMonitor(context) { onNetworkAvailable() }
        val queue = InlineEventQueue(
            file = File(directory, EVENTS_FILE),
            sender = CoreInlineEventSender(metaData),
            clock = SystemInlineClock,
            scheduler = MainInlineScheduler,
            isOnline = monitor::isOnline,
            rootNow = { InlineEndpoint.sendRoot(InlineEndpoint.overrideUrl(metaData())) },
            writer = writer,
            isDeviceRemoved = { RepositoryModule.getRegistrationPreferences()?.removeAllDeviceData()?.get() == true },
        )
        val store = InlineStore(File(directory, BANNERS_FILE), SystemInlineClock, MainInlineScheduler, writer)
        val manager = InlineManager(store, CoreInlineTransport(metaData), queue, CoreInlineIdentity, SystemInlineClock, MainInlineScheduler)
        manager.listener = ForwardingListener
        queue.onQueuedDelivered = manager::onQueuedEventDelivered
        return Instance(manager, queue, monitor)
    }

    /** Reads [PushwooshInline.listener] on every callback, so the app may set it before or after the manager is built. */
    private object ForwardingListener : PushwooshInlineListener {
        override fun onSlotUpdated(slotId: String, banner: PushwooshInlineBanner?) {
            PushwooshInline.listener?.onSlotUpdated(slotId, banner)
        }

        override fun onSlotShown(slotId: String, banner: PushwooshInlineBanner) {
            PushwooshInline.listener?.onSlotShown(slotId, banner)
        }

        override fun onSlotClicked(slotId: String, banner: PushwooshInlineBanner, elementId: String?) {
            PushwooshInline.listener?.onSlotClicked(slotId, banner, elementId)
        }

        override fun onSlotDismissed(slotId: String, banner: PushwooshInlineBanner) {
            PushwooshInline.listener?.onSlotDismissed(slotId, banner)
        }
    }
}
