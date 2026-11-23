package com.pushwoosh.pushmart.live

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import com.pushwoosh.pushmart.R

/**
 * The order notification below Android 16, laid out as the iOS Lock Screen Live Activity: the
 * PushMart header, the status and the four phases collapsed; the message, the product photo and the
 * phase names added when expanded. Android 16 promotes the order instead, and a promoted
 * notification takes no custom view.
 */
internal object OrderCard {
    private val segments =
        intArrayOf(
            R.id.order_segment_0, R.id.order_segment_1, R.id.order_segment_2, R.id.order_segment_3)
    private val labels =
        intArrayOf(R.id.order_label_0, R.id.order_label_1, R.id.order_label_2, R.id.order_label_3)

    fun collapsed(context: Context, content: LiveContent, stage: OrderStage): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_order_collapsed).apply {
            bindStatus(content, stage)
            bindSegments(context, stage)
        }

    fun expanded(context: Context, content: LiveContent, stage: OrderStage): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_order_expanded).apply {
            bindStatus(content, stage)
            setTextViewText(R.id.order_text, content.text)
            if (content.largeIcon != null) {
                setImageViewBitmap(R.id.order_photo, content.largeIcon)
            } else {
                setViewVisibility(R.id.order_photo, View.GONE)
            }
            bindSegments(context, stage)
            for (phase in OrderStage.values()) {
                val name = context.getString(phase.label)
                setTextViewText(labels[phase.ordinal], if (phase == stage) bold(name) else name)
                setBoolean(labels[phase.ordinal], "setEnabled", phase <= stage)
            }
        }

    private fun RemoteViews.bindStatus(content: LiveContent, stage: OrderStage) {
        setImageViewResource(R.id.order_icon, stage.icon)
        setTextViewText(R.id.order_header, content.header)
        setTextViewText(R.id.order_title, content.title)
    }

    /** Reached phases in their own colour, the rest stay on the theme's track. */
    private fun RemoteViews.bindSegments(context: Context, stage: OrderStage) {
        for (phase in OrderStage.values()) {
            if (phase > stage) continue
            setImageViewResource(segments[phase.ordinal], R.drawable.bg_live_segment_fill)
            setInt(segments[phase.ordinal], "setColorFilter", phase.color(context))
        }
    }

    private fun bold(text: String): CharSequence =
        SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
}
