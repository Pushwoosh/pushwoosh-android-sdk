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
import com.pushwoosh.inbox.data.InboxMessageType
import java.util.Date

/** Shared test fixture: a stub [InboxMessage] with every field overridable. */
fun fakeInboxMessage(
    code: String = "code",
    title: String? = null,
    imageUrl: String? = null,
    message: String = "message",
    bannerUrl: String? = null,
    actionParams: String? = null,
    read: Boolean = false,
    actionPerformed: Boolean = false
): InboxMessage = object : InboxMessage {
    override fun getCode(): String = code
    override fun getTitle(): String? = title
    override fun getImageUrl(): String? = imageUrl
    override fun getMessage(): String = message
    override fun getSendDate(): Date = Date(0L)
    override fun getISO8601SendDate(): String = "1970-01-01T00:00:00Z"
    override fun getType(): InboxMessageType = InboxMessageType.PLAIN
    override fun getBannerUrl(): String? = bannerUrl
    override fun getActionParams(): String? = actionParams
    override fun isRead(): Boolean = read
    override fun isActionPerformed(): Boolean = actionPerformed
    override fun compareTo(other: InboxMessage): Int = code.compareTo(other.code)
}
