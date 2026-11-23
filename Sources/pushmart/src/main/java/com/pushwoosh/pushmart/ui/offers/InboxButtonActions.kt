package com.pushwoosh.pushmart.ui.offers

import android.content.Context
import android.content.Intent
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxCardButton
import com.pushwoosh.inbox.ui.OnInboxButtonClickListener
import com.pushwoosh.inbox.ui.OnInboxMessageClickListener
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.LocalReminders
import com.pushwoosh.pushmart.shop.WishlistStore
import com.pushwoosh.pushmart.ui.common.ResultBus
import org.json.JSONObject

/**
 * Inbox card taps, as iOS `inboxKit(_:didTapButton:onMessage:)` and `handleCustomButton`: the
 * custom `save` / `snooze` / `share` tags are handled here, every other button and a row tap keep
 * the SDK default.
 */
class InboxButtonActions(
    context: Context,
    private val wishlist: WishlistStore,
    private val reminders: LocalReminders,
) : OnInboxButtonClickListener, OnInboxMessageClickListener {
    private val context = context.applicationContext

    override fun onInboxMessageClick(message: InboxMessage) {
        PWLog.debug(TAG, "selected ${message.code}")
    }

    override fun onInboxButtonClick(message: InboxMessage, button: InboxCardButton): Boolean {
        val payload = (button.action as? InboxCardButton.Action.Custom)?.payload
        val tag = payload?.optString("tag").orEmpty()
        PWLog.debug(TAG, "button ${button.title} tag=$tag")
        if (payload == null) return true
        return when (tag) {
            "save" -> save(payload)
            "snooze" -> snooze(message, payload)
            "share" -> share(message, payload)
            else -> true
        }
    }

    private fun save(payload: JSONObject): Boolean {
        val product = Catalog.byId(payload.optString("sku")) ?: return true
        if (!wishlist.contains(product.id)) wishlist.toggle(product)
        ResultBus.success(context.getString(R.string.inbox_saved_title), product.name)
        return false
    }

    private fun snooze(message: InboxMessage, payload: JSONObject): Boolean {
        val seconds = payload.optInt("seconds", DEFAULT_SNOOZE_SECONDS)
        val title = message.title ?: context.getString(R.string.inbox_reminder_fallback)
        reminders.schedule(title, message.message, seconds)
        ResultBus.success(context.getString(R.string.inbox_reminder_set))
        return false
    }

    private fun share(message: InboxMessage, payload: JSONObject): Boolean {
        val text = payload.optString("text").ifEmpty { message.title.orEmpty() }
        val send =
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(
            Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return false
    }

    companion object {
        private const val TAG = "PushMartInbox"
        private const val DEFAULT_SNOOZE_SECONDS = 60
    }
}
