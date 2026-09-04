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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxCardButton
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.utils.parseToString

/**
 * Classic rich card: a compact text-first row inside a card surface — round
 * avatar, unread dot at the leading edge, title with date, two lines of body
 * and optional inline CTA buttons. Mirrors the iOS PushwooshInboxKit classic
 * cell, which is also what every other kind degrades to when its payload is
 * missing (see [InboxCardKind.resolve]).
 */
class ClassicInboxViewHolder(adapter: InboxAdapter,
                             itemView: View,
                             colorSchemeProvider: ColorSchemeProvider) : RichCardViewHolder(adapter, itemView, colorSchemeProvider) {

    companion object {
        private const val READ_AVATAR_COLOR = Color.GRAY
        private const val FALLBACK_INITIAL = "·"

        // The host app's label never changes while the process lives — resolving it per
        // bind would hit PackageManager on every scroll frame.
        private var cachedInitial: String? = null

        private fun appInitial(context: Context): String {
            cachedInitial?.let { return it }
            val label = context.applicationInfo.loadLabel(context.packageManager).toString().trim()
            val initial = label.take(1).uppercase().takeIf { it.isNotEmpty() } ?: FALLBACK_INITIAL
            cachedInitial = initial
            return initial
        }

        /**
         * The launcher icon as a resource id, not a Drawable: one cached Drawable handed to
         * several rows would share bounds and a single callback, and would survive a theme
         * or density change. 0 means the app ships no icon.
         */
        private fun appIconRes(context: Context): Int = context.applicationInfo.icon
    }

    private val avatarView: View = itemView.findViewById(R.id.inboxClassicAvatar)
    private val avatarImageView: ImageView = itemView.findViewById(R.id.inboxClassicAvatarImage)
    private val avatarInitialView: TextView = itemView.findViewById(R.id.inboxClassicAvatarInitial)
    private val titleTextView: TextView = itemView.findViewById(R.id.inboxClassicTitle)
    private val bodyTextView: TextView = itemView.findViewById(R.id.inboxClassicBody)
    private val dateTextView: TextView = itemView.findViewById(R.id.inboxClassicDate)
    private val unreadDotView: View = itemView.findViewById(R.id.inboxClassicUnreadDot)
    private val pinIconView: ImageView = itemView.findViewById(R.id.inboxClassicPinIcon)
    private val buttonsRow: LinearLayout = itemView.findViewById(R.id.inboxClassicButtonsRow)

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }

        val params = InboxCardKind.parseActionParams(model)
        bindAvatar(InboxCardKind.resolveIconUrl(model, params), model)

        // iOS puts the body text in the title slot when the message carries no title;
        // the body is then hidden so the same sentence isn't printed twice.
        val hasTitle = !model.title.isNullOrEmpty()
        val hasBody = !model.message.isNullOrEmpty()
        val unread = isUnread(model)
        titleTextView.text = if (hasTitle) model.title else model.message
        titleTextView.setTextColor(colorSchemeProvider.titleColor)
        titleTextView.isSelected = unread
        bodyTextView.text = model.message
        bodyTextView.setTextColor(colorSchemeProvider.descriptionColor)
        bodyTextView.isSelected = unread
        bodyTextView.visibility = if (hasTitle && hasBody) View.VISIBLE else View.GONE
        dateTextView.text = model.sendDate.parseToString()
        dateTextView.setTextColor(colorSchemeProvider.dateColor)

        bindUnreadDot(unreadDotView, model)
        bindPinIcon(InboxCardKind.isPinned(params))

        renderButtons(buttonsRow, InboxCardButton.decode(params), model)
    }

    /** Message image, then the host app's launcher icon, then its first letter on a tinted circle. */
    private fun bindAvatar(iconUrl: String?, model: InboxMessage) {
        val iconRes = appIconRes(context)

        if (!iconUrl.isNullOrEmpty()) {
            avatarInitialView.visibility = View.GONE
            avatarImageView.visibility = View.VISIBLE
            avatarView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            var request = Glide.with(itemView.context).load(iconUrl)
            if (iconRes != 0) {
                request = request.placeholder(iconRes)
            }
            request.transform(CircleCrop()).into(avatarImageView)
            return
        }

        Glide.with(itemView.context).clear(avatarImageView)
        if (iconRes != 0) {
            avatarInitialView.visibility = View.GONE
            avatarImageView.visibility = View.VISIBLE
            avatarImageView.setImageResource(iconRes)
            avatarView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            return
        }

        avatarImageView.setImageDrawable(null)
        avatarImageView.visibility = View.GONE
        avatarInitialView.visibility = View.VISIBLE
        avatarInitialView.text = appInitial(context)
        val tint = if (model.isRead) READ_AVATAR_COLOR else colorSchemeProvider.accentColor
        avatarView.backgroundTintList = ColorStateList.valueOf(tint)
    }

    private fun bindPinIcon(pinned: Boolean) {
        pinIconView.visibility = if (pinned) View.VISIBLE else View.GONE
        if (pinned) {
            pinIconView.imageTintList = ColorStateList.valueOf(colorSchemeProvider.accentColor)
        }
    }
}
