package com.pushwoosh.pushmart.live

import android.content.Context
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.liveupdates.PushwooshLiveUpdates
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.shop.OrderStore
import com.pushwoosh.pushmart.ui.common.ResultBus
import java.util.concurrent.CopyOnWriteArraySet

/**
 * The tracked order on the Lock Screen, as iOS `ManualLiveActivityController`: one ongoing
 * `PUSHWOOSH_ORDER` notification, replaced by a server Live Update with the same id. The tracked
 * order is read live from the notifications on screen, so nothing needs re-attaching after a
 * relaunch.
 */
class LiveOrders(
    context: Context,
    private val prefs: KeyValueStore,
    private val notifier: LiveNotifier,
    private val orders: OrderStore,
) {
    private val context = context.applicationContext
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    val isAvailable: Boolean
        get() = notifier.isAvailable

    val trackedOrderId: String?
        get() = prefs.getString(KEY).takeIf { it.isNotEmpty() && notifier.isActive(ACTIVITY_ID) }

    /** Starts tracking only when notifications are already allowed: no Push Primer. */
    fun startIfAllowed(orderId: String): Boolean {
        if (!notifier.isAllowed) return false
        val posted = post(orderId)
        notifyListeners()
        return posted
    }

    /** Starts tracking, through the Push Primer when needed. Listeners fire either way. */
    fun start(orderId: String) {
        notifier.whenAllowed { granted ->
            if (granted) post(orderId)
            notifyListeners()
        }
    }

    fun stop() {
        PushwooshLiveUpdates.endLiveUpdate(ACTIVITY_ID)
        notifier.cancel(ACTIVITY_ID)
        prefs.remove(KEY)
        notifyListeners()
    }

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    private fun post(orderId: String): Boolean {
        val shown =
            notifier.show(
                ACTIVITY_ID,
                LiveContent(
                    context.getString(R.string.live_order_title),
                    context.getString(R.string.live_order_text),
                    notifier.openApp(),
                    promoted = true,
                    stage = OrderStage.CREATED,
                    header = context.getString(R.string.live_order_header),
                    largeIcon =
                        orders.byId(orderId)?.products?.firstOrNull()?.let {
                            notifier.photo(it.imageName)
                        },
                ),
            )
        if (shown) {
            prefs.putString(KEY, orderId)
            PWLog.debug(TAG, "tracking $orderId")
        } else {
            ResultBus.fail(context.getString(R.string.live_order_pin_failed))
        }
        return shown
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    companion object {
        const val ACTIVITY_ID = "PUSHWOOSH_ORDER"
        const val KEY = "pushmart.trackedOrder"
        private const val TAG = "LiveOrders"
    }
}
