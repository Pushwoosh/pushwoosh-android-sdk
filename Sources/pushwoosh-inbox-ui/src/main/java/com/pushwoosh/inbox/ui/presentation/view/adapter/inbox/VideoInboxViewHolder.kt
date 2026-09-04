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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxVideoContent
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxVideoActivity
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.internal.utils.PWLog

/**
 * Video rich card: a poster preview with a play badge over the message title /
 * body. Mirrors the iOS PushwooshInboxKit video cell — there is no in-card
 * playback, tapping the poster opens [InboxVideoActivity] full-screen. The
 * text block collapses when the message carries neither title nor body.
 */
class VideoInboxViewHolder(adapter: InboxAdapter,
                           itemView: View,
                           colorSchemeProvider: ColorSchemeProvider) : RichCardViewHolder(adapter, itemView, colorSchemeProvider) {

    private companion object {
        private const val TAG = "VideoInboxViewHolder"
    }

    private val posterHostView: View = itemView.findViewById(R.id.inboxVideoPosterHost)
    private val posterView: ImageView = itemView.findViewById(R.id.inboxVideoPoster)
    private val playBadgeView: View = itemView.findViewById(R.id.inboxVideoPlayBadge)
    private val textBlockView: View = itemView.findViewById(R.id.inboxVideoTextBlock)
    private val titleRowView: View = itemView.findViewById(R.id.inboxVideoTitleRow)
    private val titleTextView: TextView = itemView.findViewById(R.id.inboxVideoTitle)
    private val bodyTextView: TextView = itemView.findViewById(R.id.inboxVideoBody)
    private val dateTextView: TextView = itemView.findViewById(R.id.inboxVideoDate)
    private val unreadDotView: View = itemView.findViewById(R.id.inboxVideoUnreadDot)
    private val pinChipView: View = itemView.findViewById(R.id.inboxVideoPinChip)

    init {
        clipToRoundedCorners(posterHostView, R.dimen.pw_video_poster_corner_radius)
    }

    override fun fillView(model: InboxMessage?, position: Int) {
        if (model == null) {
            return
        }

        val params = InboxCardKind.parseActionParams(model)
        val content = InboxVideoContent.decode(params)

        // A video card carries its picture in the descriptor's poster and nowhere else: iOS
        // feeds message.imageUrl only to its glass backdrop, never to the poster itself.
        Glide.with(itemView.context)
                .load(content?.posterUrl)
                .placeholder(colorSchemeProvider.defaultIcon)
                .into(posterView)

        playBadgeView.visibility = if (content == null) View.GONE else View.VISIBLE
        posterHostView.setOnClickListener {
            val videoUrl = content?.videoUrl ?: return@setOnClickListener
            openPlayer(videoUrl, model)
        }

        val hasText = bindTextBlock(model, titleRowView, titleTextView, bodyTextView, dateTextView, textBlockView)

        bindUnreadDot(unreadDotView, model)
        if (!hasText) {
            unreadDotView.visibility = View.GONE
        }
        bindPinChip(pinChipView, InboxCardKind.isPinned(params))
    }

    // Marks read even when the player fails to open, matching openCardUrl: the tap itself is
    // the engagement signal, and iOS reports it the same way.
    private fun openPlayer(videoUrl: String, model: InboxMessage) {
        try {
            val intent = Intent(context, InboxVideoActivity::class.java)
            intent.putExtra(InboxVideoActivity.VIDEO_URL_EXTRA, videoUrl)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            PWLog.warn(TAG, "Failed to open the inbox video player: $videoUrl")
        }
        PushwooshInbox.readMessage(model.code)
    }
}
