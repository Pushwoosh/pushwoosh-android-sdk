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
 * Captioned rich card: a hero image on top, title + date and body below,
 * with the message's round icon beside the caption and up to [MAX_BUTTONS]
 * inline CTA buttons. Mirrors the iOS PushwooshInboxKit captioned cell.
 * Selected for `displayType=captioned` or by the opt-in image/title
 * heuristic — see [InboxCardKind.resolve].
 */
class CaptionedInboxViewHolder(adapter: InboxAdapter,
                               itemView: View,
                               colorSchemeProvider: ColorSchemeProvider) : RichCardViewHolder(adapter, itemView, colorSchemeProvider) {

    private val heroImageView: ImageView = itemView.findViewById(R.id.inboxCaptionedHeroImage)
    private val iconView: ImageView = itemView.findViewById(R.id.inboxCaptionedIcon)
    private val titleTextView: TextView = itemView.findViewById(R.id.inboxCaptionedTitle)
    private val bodyTextView: TextView = itemView.findViewById(R.id.inboxCaptionedBody)
    private val dateTextView: TextView = itemView.findViewById(R.id.inboxCaptionedDate)
    private val unreadDotView: View = itemView.findViewById(R.id.inboxCaptionedUnreadDot)
    private val pinChipView: View = itemView.findViewById(R.id.inboxCaptionedPinChip)
    private val buttonsRow: LinearLayout = itemView.findViewById(R.id.inboxCaptionedButtonsRow)

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }

        val params = InboxCardKind.parseActionParams(model)
        val heroUrl = InboxCardKind.resolveHeroUrl(model, params)
        loadHero(heroImageView, heroUrl, R.dimen.pw_captioned_hero_corner_radius)

        // The round icon duplicates the hero when the payload carried only one
        // picture — skip it then, same as the iOS captioned cell.
        val iconUrl = InboxCardKind.resolveIconUrl(model, params)
        if (iconUrl != null && iconUrl != heroUrl) {
            iconView.visibility = View.VISIBLE
            Glide.with(itemView.context)
                    .load(iconUrl)
                    .transform(CircleCrop())
                    .into(iconView)
        } else {
            iconView.visibility = View.GONE
        }

        val unread = isUnread(model)
        titleTextView.text = model.title
        titleTextView.setTextColor(colorSchemeProvider.titleColor)
        titleTextView.isSelected = unread
        bodyTextView.text = model.message
        bodyTextView.setTextColor(colorSchemeProvider.descriptionColor)
        bodyTextView.isSelected = unread
        dateTextView.text = model.sendDate.parseToString()
        dateTextView.setTextColor(colorSchemeProvider.dateColor)

        bindUnreadDot(unreadDotView, model)
        bindPinChip(pinChipView, InboxCardKind.isPinned(params))

        renderButtons(buttonsRow, InboxCardButton.decode(params), model)
    }
}
