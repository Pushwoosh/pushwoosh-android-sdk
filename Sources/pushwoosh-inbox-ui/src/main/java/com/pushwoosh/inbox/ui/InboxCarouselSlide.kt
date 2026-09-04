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

import android.net.Uri
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxCardKind
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single slide inside a carousel inbox card, mirroring the iOS
 * PushwooshInboxCarouselSlide contract.
 *
 * Unlike the other card kinds — which render the message's single image — a
 * carousel shows several images from one message. Those slides cannot live in
 * the standard message fields, so they arrive in `actionParams["carousel"]`
 * (at the root, inside `u` as a dict, or inside `u` as a JSON-encoded string)
 * and are decoded by [decode]:
 *
 * ```
 * { "carousel": [
 *     { "image": "https://…/1.jpg", "title": "New in", "url": "myapp://p/1" },
 *     { "image": "https://…/2.jpg", "title": "On sale", "url": "myapp://p/2" }
 * ] }
 * ```
 *
 * @property imageUrl remote image of the slide; a slide without one is dropped
 * @property title optional caption drawn over the bottom of the slide
 * @property url optional destination opened on tap; `null` falls through to the
 *               message's default row action
 */
class InboxCarouselSlide(val imageUrl: String, val title: String?, val url: String?) {

    companion object {

        /**
         * Slides past this many are dropped: every slide costs a view, a page dot
         * and an image request, so a mis-authored campaign must not freeze the list.
         */
        const val MAX_SLIDES = 10

        @JvmStatic
        fun decode(message: InboxMessage): List<InboxCarouselSlide> =
            decode(InboxCardKind.parseActionParams(message))

        internal fun decode(params: JSONObject?): List<InboxCarouselSlide> {
            if (params == null) {
                return emptyList()
            }
            params.optJSONArray("carousel")?.let { return parse(it) }
            return InboxCardKind.readU(params)?.optJSONArray("carousel")?.let { parse(it) } ?: emptyList()
        }

        private fun parse(rawSlides: JSONArray): List<InboxCarouselSlide> {
            val result = mutableListOf<InboxCarouselSlide>()
            for (i in 0 until rawSlides.length()) {
                if (result.size == MAX_SLIDES) {
                    break
                }
                val item = rawSlides.optJSONObject(i) ?: continue
                // Network schemes only: the image goes to Glide, which loads file:// and
                // content:// straight off the device from a payload-controlled string.
                val image = item.optString("image")
                if (image.isEmpty() || !InboxCardKind.isHttpUrl(image)) {
                    continue
                }
                val title = item.optString("title").takeIf { it.isNotEmpty() }
                val url = item.optString("url")
                    .takeIf { it.isNotEmpty() && Uri.parse(it).scheme != null }
                result.add(InboxCarouselSlide(image, title, url))
            }
            return result
        }
    }
}
