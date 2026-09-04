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
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider

/**
 * Banner rich card: a full-bleed hero image with an unread dot and a pin
 * chip, no text. Mirrors the iOS PushwooshInboxKit banner cell. Selected for
 * messages whose actionParams carry `displayType=banner`, or by the opt-in
 * image/title heuristic when the message has an image and no title — see
 * [InboxCardKind.resolve].
 */
class BannerInboxViewHolder(adapter: InboxAdapter,
                            itemView: View,
                            colorSchemeProvider: ColorSchemeProvider) : RichCardViewHolder(adapter, itemView, colorSchemeProvider) {

    private val heroImageView: ImageView = itemView.findViewById(R.id.inboxBannerHeroImage)
    private val unreadDotView: View = itemView.findViewById(R.id.inboxBannerUnreadDot)
    private val pinChipView: View = itemView.findViewById(R.id.inboxBannerPinChip)

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }
        val params = InboxCardKind.parseActionParams(model)
        loadHero(heroImageView, InboxCardKind.resolveHeroUrl(model, params), R.dimen.pw_banner_corner_radius)
        bindUnreadDot(unreadDotView, model)
        bindPinChip(pinChipView, InboxCardKind.isPinned(params))
    }
}
