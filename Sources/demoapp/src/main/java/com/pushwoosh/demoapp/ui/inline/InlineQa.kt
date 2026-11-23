package com.pushwoosh.demoapp.ui.inline

import android.os.Looper
import com.pushwoosh.Pushwoosh
import com.pushwoosh.inapp.InAppManager
import com.pushwoosh.inline.PushwooshInline
import com.pushwoosh.inline.PushwooshInlineBanner
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.inline.PushwooshInlineListener
import com.pushwoosh.internal.utils.PWLog
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

/**
 * Inline slots test hooks shared by the Inline screen buttons and the debug adb receiver.
 * Everything is mirrored to logcat under [TAG] in the formats the e2e scenarios grep for.
 */
object InlineQa {
    const val TAG = "PWQA_INLINE"

    private val requestCounter = AtomicInteger()

    val listener =
        object : PushwooshInlineListener {
            override fun onSlotUpdated(slotId: String, banner: PushwooshInlineBanner?) {
                log("updated slot=$slotId banner=${describe(banner)}")
            }

            override fun onSlotShown(slotId: String, banner: PushwooshInlineBanner) {
                log("shown slot=$slotId banner=${banner.bannerId}")
            }

            override fun onSlotClicked(
                slotId: String,
                banner: PushwooshInlineBanner,
                elementId: String?
            ) {
                log("clicked slot=$slotId banner=${banner.bannerId} element=${elementId ?: "null"}")
            }

            override fun onSlotDismissed(slotId: String, banner: PushwooshInlineBanner) {
                log("dismissed slot=$slotId banner=${banner.bannerId}")
            }
        }

    fun log(message: String) {
        PWLog.info(TAG, message)
    }

    fun request(slotIds: List<String>) {
        val n = requestCounter.incrementAndGet()
        val slots = slotIds.joinToString(",", "[", "]")
        PushwooshInline.requestSlots(slotIds) { error: PushwooshInlineException? ->
            val main = Looper.myLooper() == Looper.getMainLooper()
            val result =
                if (error == null) "ok" else "error code=${error.code} msg=${error.message}"
            log("request#$n slots=$slots -> $result main=$main")
        }
    }

    fun setUserId(id: String) {
        log("setUserId id=$id")
        Pushwoosh.getInstance().setUserId(id)
    }

    fun postEvent(event: String) {
        log("postEvent event=$event")
        InAppManager.getInstance().postEvent(event)
    }

    fun purchase(sku: String, price: String, currency: String) {
        log("purchase sku=$sku price=$price currency=$currency")
        Pushwoosh.getInstance().sendInappPurchase(sku, BigDecimal(price), currency)
    }

    fun banner(slotId: String) {
        log("banner slot=$slotId -> ${describe(PushwooshInline.getBanner(slotId))}")
    }

    private fun describe(banner: PushwooshInlineBanner?) =
        if (banner == null) "null" else "${banner.bannerId}v${banner.version}"
}
