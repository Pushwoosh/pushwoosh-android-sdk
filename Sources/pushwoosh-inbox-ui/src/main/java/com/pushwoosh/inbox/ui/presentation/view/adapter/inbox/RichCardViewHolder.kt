/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DimenRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxCardButton
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.utils.parseToString
import com.pushwoosh.internal.utils.PWLog

/**
 * Shared chrome for rich inbox cards: the hero image, the unread dot, the pin
 * chip and the inline CTA buttons behave identically on every card kind.
 */
abstract class RichCardViewHolder(adapter: InboxAdapter,
                                  itemView: View,
                                  protected val colorSchemeProvider: ColorSchemeProvider) : BaseRecyclerAdapter.ViewHolder<InboxMessage>(itemView, adapter) {

    private companion object {
        private const val TAG = "RichCardViewHolder"
        private const val BUTTON_BACKGROUND_ALPHA = 0x1F
        private const val MAX_BUTTONS = 3
    }

    protected fun loadHero(target: ImageView, heroUrl: String?, @DimenRes cornerRadiusRes: Int) {
        val cornerRadius = context.resources.getDimensionPixelSize(cornerRadiusRes)
        Glide.with(itemView.context)
                .load(heroUrl)
                .transform(CenterCrop(), RoundedCorners(cornerRadius))
                .into(target)
    }

    /**
     * The unread dot mirrors the iOS cards: visible until the message is read
     * — by a row tap (performAction -> OPEN) or an inline button
     * (readMessage -> READ); [InboxMessage.isRead] covers both statuses.
     */
    protected fun bindUnreadDot(dot: View, model: InboxMessage) {
        dot.backgroundTintList = ColorStateList.valueOf(colorSchemeProvider.accentColor)
        dot.visibility = if (isUnread(model)) View.VISIBLE else View.GONE
    }

    protected fun bindPinChip(chip: View, pinned: Boolean) {
        chip.visibility = if (pinned) View.VISIBLE else View.GONE
    }

    /**
     * Rounds a container's content, replacing `android:clipToOutline` — that attribute is
     * honoured only from API 31, and this SDK ships from 23.
     */
    protected fun clipToRoundedCorners(view: View, @DimenRes cornerRadiusRes: Int) {
        val radius = context.resources.getDimension(cornerRadiusRes)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(target: View, outline: Outline) {
                outline.setRoundRect(0, 0, target.width, target.height, radius)
            }
        }
        view.clipToOutline = true
    }

    protected fun isUnread(model: InboxMessage): Boolean = !model.isRead

    /**
     * Binds the title / body / date trio shared by every card that shows text, and collapses
     * the whole block when the message carries neither title nor body (a pure media card).
     * Returns `true` when the block stayed visible, so the caller can align its unread dot.
     */
    protected fun bindTextBlock(
        model: InboxMessage,
        titleRow: View,
        titleView: TextView,
        bodyView: TextView,
        dateView: TextView,
        textBlock: View
    ): Boolean {
        val unread = isUnread(model)
        val hasTitle = !model.title.isNullOrEmpty()
        val hasBody = !model.message.isNullOrEmpty()

        titleView.text = model.title
        titleView.setTextColor(colorSchemeProvider.titleColor)
        titleView.isSelected = unread
        bodyView.text = model.message
        bodyView.setTextColor(colorSchemeProvider.descriptionColor)
        bodyView.isSelected = unread
        dateView.text = model.sendDate.parseToString()
        dateView.setTextColor(colorSchemeProvider.dateColor)

        titleRow.visibility = if (hasTitle) View.VISIBLE else View.GONE
        bodyView.visibility = if (hasBody) View.VISIBLE else View.GONE
        val hasText = hasTitle || hasBody
        textBlock.visibility = if (hasText) View.VISIBLE else View.GONE
        return hasText
    }

    /**
     * Opens a destination carried by card content — an inline button or a
     * carousel slide — and marks the message read, mirroring iOS. Unsafe
     * schemes are refused; the message stays unread then.
     */
    protected fun openCardUrl(url: String, model: InboxMessage) {
        if (!InboxCardButton.isSafeUrl(url)) {
            PWLog.warn(TAG, "Refusing to open inbox card URL with unsafe scheme: $url")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            PWLog.warn(TAG, "No activity to handle inbox card URL: $url")
        }
        PushwooshInbox.readMessage(model.code)
    }

    /**
     * Renders up to three inline CTA buttons into [buttonsRow], hiding the row
     * when the message carries none. Mirrors the iOS card button strip.
     */
    protected fun renderButtons(buttonsRow: LinearLayout, buttons: List<InboxCardButton>, model: InboxMessage) {
        buttonsRow.removeAllViews()
        if (buttons.isEmpty()) {
            buttonsRow.visibility = View.GONE
            return
        }
        buttonsRow.visibility = View.VISIBLE
        val res = context.resources
        val height = res.getDimensionPixelSize(R.dimen.pw_rich_card_button_height)
        val gap = res.getDimensionPixelSize(R.dimen.pw_rich_card_button_gap)
        val radius = res.getDimensionPixelSize(R.dimen.pw_rich_card_button_radius).toFloat()
        val accent = colorSchemeProvider.accentColor

        buttons.take(MAX_BUTTONS).forEachIndexed { index, button ->
            val view = TextView(context)
            view.text = button.title
            view.setTextAppearance(R.style.TextAppearance_Inbox_InboxCardButton)
            view.setTextColor(accent)
            view.gravity = Gravity.CENTER
            view.maxLines = 1
            view.ellipsize = TextUtils.TruncateAt.END
            view.background = GradientDrawable().apply {
                cornerRadius = radius
                setColor((accent and 0x00FFFFFF) or (BUTTON_BACKGROUND_ALPHA shl 24))
            }
            view.setOnClickListener { handleButtonTap(button, model) }
            val params = LinearLayout.LayoutParams(0, height, 1f)
            if (index > 0) {
                params.marginStart = gap
            }
            buttonsRow.addView(view, params)
        }
    }

    private fun handleButtonTap(button: InboxCardButton, model: InboxMessage) {
        // Mirrors the iOS delegate contract: the host returning false consumes the tap.
        val shouldPerformDefault = PushwooshInboxUi.onButtonClickListener?.onInboxButtonClick(model, button) ?: true
        if (!shouldPerformDefault) {
            return
        }
        when (val action = button.action) {
            is InboxCardButton.Action.OpenUrl -> openCardUrl(action.url, model)
            is InboxCardButton.Action.Dismiss -> PushwooshInbox.deleteMessage(model.code)
            is InboxCardButton.Action.MarkRead -> PushwooshInbox.readMessage(model.code)
            is InboxCardButton.Action.Custom -> {
                // The payload reaches the host via onButtonClickListener; the interaction
                // still counts as engagement, so flip the message to read like iOS does.
                PWLog.noise(TAG, "Custom inbox button tapped: ${button.title}")
                PushwooshInbox.readMessage(model.code)
            }
        }
    }
}
