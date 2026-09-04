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

import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.InboxCarouselSlide
import com.pushwoosh.inbox.ui.InboxVideoContent
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONException
import org.json.JSONObject

/**
 * Rich-card kind of an inbox message, mirroring the iOS PushwooshInboxKit
 * resolver. Server-driven first: an explicit `displayType` in the message's
 * actionParams selects the card. Without a known displayType the iOS
 * image/title heuristic applies — image + title renders `captioned`, image
 * alone renders `banner`, neither renders `classic` — but only when
 * [PushwooshInboxStyle.richCardsHeuristicEnabled] is on (off by default so
 * existing integrations keep their legacy row rendering, which is what
 * [DEFAULT] stands for).
 *
 * A requested kind whose payload is missing degrades to [CLASSIC], matching
 * iOS: `banner`/`captioned` need a hero image, `carousel` at least one slide,
 * `video` a descriptor. `wallet` always degrades — Apple Wallet is iOS-only.
 */
enum class InboxCardKind {
    BANNER,
    CAPTIONED,
    CLASSIC,
    CAROUSEL,
    VIDEO,
    DEFAULT;

    companion object {
        private const val TAG = "InboxCardKind"
        private const val DEGRADE_LOG_CACHE_LIMIT = 100

        private val KNOWN_KINDS = setOf("banner", "captioned", "classic", "carousel", "video", "wallet")

        // resolve() runs on every getItemViewType() pass — remember what was already
        // logged so a degraded message doesn't spam logcat on each layout/prefetch.
        private val degradeLoggedCodes = object : LinkedHashMap<String, Boolean>() {
            override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>) = size > DEGRADE_LOG_CACHE_LIMIT
        }

        @JvmStatic
        fun resolve(message: InboxMessage): InboxCardKind {
            val params = parseActionParams(message)
            val displayType = readDisplayType(params)
            val hasHero = resolveHeroUrl(message, params) != null

            val requested: String = if (displayType != null && displayType in KNOWN_KINDS) {
                displayType
            } else if (PushwooshInboxStyle.richCardsHeuristicEnabled) {
                when {
                    hasHero && message.title.isNullOrEmpty() -> "banner"
                    hasHero -> "captioned"
                    else -> "classic"
                }
            } else {
                return DEFAULT
            }

            // A kind the server asked for but whose payload is missing degrades to the classic
            // card, never to the legacy row: the message was authored as a card either way.
            val resolved: InboxCardKind = when (requested) {
                "banner" -> if (hasHero) BANNER else CLASSIC
                "captioned" -> if (hasHero) CAPTIONED else CLASSIC
                "carousel" -> if (InboxCarouselSlide.decode(params).isEmpty()) CLASSIC else CAROUSEL
                "video" -> if (InboxVideoContent.decode(params) == null) CLASSIC else VIDEO
                // Apple Wallet has no Android counterpart; the kind stays known so the message
                // renders as a card rather than falling back to the heuristic or a legacy row.
                "wallet" -> CLASSIC
                else -> CLASSIC
            }

            if (resolved == CLASSIC && requested != "classic") {
                logDegrade(message, displayType, requested, hasHero)
            }
            return resolved
        }

        private fun logDegrade(message: InboxMessage, displayType: String?, requested: String, hasHero: Boolean) {
            if (degradeLoggedCodes.put(message.code, true) != null) {
                return
            }
            val reason = when (requested) {
                "carousel" -> "no slides"
                "video" -> "no video descriptor"
                "wallet" -> "wallet cards are iOS-only"
                else -> "no image"
            }
            PWLog.warn(
                TAG,
                "Inbox card degraded — code=${message.code} displayType=$displayType "
                    + "hasImage=$hasHero $requested→classic ($reason)"
            )
        }

        /**
         * The hero image of a banner/captioned card: the push attachment first
         * (`actionParams["attachment"]`, then the `b` key exposed as
         * [InboxMessage.getBannerUrl]), falling back to the message icon
         * ([resolveIconUrl]) so a payload carrying just an icon still renders
         * a card. Attachment values must be http/https.
         */
        @JvmStatic
        fun resolveHeroUrl(message: InboxMessage): String? = resolveHeroUrl(message, parseActionParams(message))

        /**
         * The message icon: [InboxMessage.getImageUrl] first, then `image` at
         * the actionParams root or inside `u`. This is the small round avatar
         * beside a captioned card's text; when it equals [resolveHeroUrl] the
         * payload carried only one picture and cards skip the icon.
         */
        @JvmStatic
        fun resolveIconUrl(message: InboxMessage): String? = resolveIconUrl(message, parseActionParams(message))

        /**
         * `true` if the message carries `pinned == true` in actionParams — at
         * the root, inside `u`, or inside legacy `userdata`.
         */
        @JvmStatic
        fun isPinned(message: InboxMessage): Boolean = isPinned(parseActionParams(message))

        internal fun isPinned(params: JSONObject?): Boolean {
            if (params == null) {
                return false
            }
            readPinnedFlag(params)?.let { return it }
            readU(params)?.let { u -> readPinnedFlag(u)?.let { return it } }
            params.optJSONObject("userdata")?.let { userdata -> readPinnedFlag(userdata)?.let { return it } }
            return false
        }

        internal fun resolveHeroUrl(message: InboxMessage, params: JSONObject?): String? {
            params?.optNonEmptyString("attachment")?.takeIf { isHttpUrl(it) }?.let { return it }
            message.bannerUrl?.takeIf { it.isNotEmpty() && isHttpUrl(it) }?.let { return it }
            return resolveIconUrl(message, params)
        }

        internal fun resolveIconUrl(message: InboxMessage, params: JSONObject?): String? {
            message.imageUrl?.takeIf { it.isNotEmpty() }?.let { return it }
            if (params == null) {
                return null
            }
            params.optNonEmptyString("image")?.let { return it }
            return readU(params)?.optNonEmptyString("image")
        }

        private fun readDisplayType(params: JSONObject?): String? {
            if (params == null) {
                return null
            }
            params.optNonEmptyString("displayType")?.let { return it.lowercase() }
            readU(params)?.optNonEmptyString("displayType")?.let { return it.lowercase() }
            return params.optJSONObject("userdata")?.optNonEmptyString("displayType")?.lowercase()
        }

        internal fun parseActionParams(message: InboxMessage): JSONObject? {
            val raw = message.actionParams
            if (raw.isNullOrEmpty()) {
                return null
            }
            return try {
                JSONObject(raw)
            } catch (e: JSONException) {
                null
            }
        }

        // `u` arrives either as a dict or as a JSON-encoded string — both wire shapes are live.
        internal fun readU(params: JSONObject): JSONObject? {
            params.optJSONObject("u")?.let { return it }
            val uString = params.optNonEmptyString("u") ?: return null
            return try {
                JSONObject(uString)
            } catch (e: JSONException) {
                null
            }
        }

        private fun readPinnedFlag(json: JSONObject): Boolean? = when (val value = json.opt("pinned")) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true)
            else -> null
        }

        internal fun isHttpUrl(value: String): Boolean =
            value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

        internal fun JSONObject.optNonEmptyString(key: String): String? {
            val value = opt(key)
            return if (value is String && value.isNotEmpty()) value else null
        }
    }
}
