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
import org.json.JSONObject

/**
 * An inline CTA button rendered inside a rich inbox card, mirroring the iOS
 * PushwooshInboxButton contract. Buttons arrive from the server inside
 * `actionParams["buttons"]` (at the root, inside `u` as a dict, or inside `u`
 * as a JSON-encoded string) and are decoded by [decode].
 */
class InboxCardButton(val title: String, val action: Action) {

    sealed class Action {
        class OpenUrl(val url: String) : Action()
        object Dismiss : Action()
        object MarkRead : Action()
        class Custom(val payload: JSONObject) : Action()
    }

    companion object {

        @JvmStatic
        fun decode(message: InboxMessage): List<InboxCardButton> = decode(InboxCardKind.parseActionParams(message))

        internal fun decode(params: JSONObject?): List<InboxCardButton> {
            if (params == null) {
                return emptyList()
            }
            params.optJSONArray("buttons")?.let { return parse(it) }
            return InboxCardKind.readU(params)?.optJSONArray("buttons")?.let { parse(it) } ?: emptyList()
        }

        /**
         * `false` for schemes that must never be handed to the system from
         * untrusted card content: local/script payloads (`file`, `javascript`,
         * `data`) and `intent` (arbitrary component targeting). Deep links and
         * web/mail/tel/sms pass through — same policy as the iOS cell.
         */
        @JvmStatic
        fun isSafeUrl(url: String): Boolean {
            val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
            return scheme !in setOf("file", "javascript", "data", "intent")
        }

        private fun parse(rawButtons: org.json.JSONArray): List<InboxCardButton> {
            val result = mutableListOf<InboxCardButton>()
            for (i in 0 until rawButtons.length()) {
                val item = rawButtons.optJSONObject(i) ?: continue
                val title = item.optString("title")
                if (title.isEmpty()) {
                    continue
                }
                result.add(InboxCardButton(title, resolveAction(item)))
            }
            return result
        }

        private fun resolveAction(item: JSONObject): Action {
            when (item.optString("action").lowercase()) {
                "dismiss" -> return Action.Dismiss
                "markread" -> return Action.MarkRead
            }
            val url = item.optString("url")
            if (url.isNotEmpty() && Uri.parse(url).scheme != null) {
                return Action.OpenUrl(url)
            }
            val payload = JSONObject()
            for (key in item.keys()) {
                if (key != "title" && key != "action") {
                    payload.put(key, item.opt(key))
                }
            }
            return Action.Custom(payload)
        }
    }
}
