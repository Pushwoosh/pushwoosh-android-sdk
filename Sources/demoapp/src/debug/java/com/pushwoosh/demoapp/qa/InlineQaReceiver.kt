package com.pushwoosh.demoapp.qa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.pushwoosh.demoapp.ui.inline.InlineQa

/** Debug-only adb driver: `am broadcast -a com.pushwoosh.demoapp.QA_INLINE --es cmd <cmd> ...`. */
class InlineQaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val cmd = intent.getStringExtra("cmd")) {
            "request" ->
                InlineQa.request(
                    intent.getStringExtra("slots").orEmpty().split(",").map { it.trim() })
            "setUserId" -> InlineQa.setUserId(intent.getStringExtra("id").orEmpty())
            "postEvent" -> InlineQa.postEvent(intent.getStringExtra("event").orEmpty())
            "banner" -> InlineQa.banner(intent.getStringExtra("slot").orEmpty())
            "purchase" ->
                InlineQa.purchase(
                    intent.getStringExtra("sku") ?: "qa_sku",
                    intent.getStringExtra("price") ?: "0.99",
                    intent.getStringExtra("currency") ?: "USD")
            else -> InlineQa.log("receiver unknown cmd=$cmd")
        }
    }

    companion object {
        const val ACTION = "com.pushwoosh.demoapp.QA_INLINE"

        fun register(context: Context) {
            ContextCompat.registerReceiver(
                context, InlineQaReceiver(), IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
        }
    }
}
