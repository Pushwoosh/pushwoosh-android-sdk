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

package com.pushwoosh.inbox.ui

import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxCardKind
import org.json.JSONObject

/**
 * Video payload of a video inbox card, mirroring the iOS
 * PushwooshInboxVideoContent contract.
 *
 * Like the carousel slides, the descriptor cannot live in the standard message
 * fields, so it arrives in `actionParams["video"]` (at the root, inside `u` as
 * a dict, or inside `u` as a JSON-encoded string):
 *
 * ```
 * { "video": { "url": "https://…/clip.mp4", "poster": "https://…/p.jpg" } }
 * ```
 *
 * The card shows the poster with a play badge; tapping it opens a full-screen
 * player.
 *
 * @property videoUrl remote video played full-screen on tap
 * @property posterUrl optional preview image shown in the card
 */
class InboxVideoContent(val videoUrl: String, val posterUrl: String?) {

    companion object {

        @JvmStatic
        fun decode(message: InboxMessage): InboxVideoContent? =
            decode(InboxCardKind.parseActionParams(message))

        internal fun decode(params: JSONObject?): InboxVideoContent? {
            if (params == null) {
                return null
            }
            params.optJSONObject("video")?.let { return content(it) }
            return InboxCardKind.readU(params)?.optJSONObject("video")?.let { content(it) }
        }

        private fun content(video: JSONObject): InboxVideoContent? {
            // Network schemes only: the url is handed to a media player, which — unlike an
            // ACTION_VIEW intent — reads file:// and content:// straight off the device.
            val url = video.optString("url")
            if (url.isEmpty() || !InboxCardKind.isHttpUrl(url)) {
                return null
            }
            val poster = video.optString("poster").takeIf { it.isNotEmpty() }
            return InboxVideoContent(url, poster)
        }
    }
}
