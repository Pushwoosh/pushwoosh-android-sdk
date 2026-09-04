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

package com.pushwoosh.inbox.ui;

import androidx.annotation.NonNull;

import com.pushwoosh.inbox.data.InboxMessage;

/**
 * Host-side hook for taps on inline CTA buttons rendered inside rich inbox
 * cards. Mirrors the iOS {@code PushwooshInboxKitDelegate.didTapButton}
 * contract: the listener runs before the SDK's default handling and receives
 * the tapped button (including a {@code Custom} button's payload) on every
 * invocation, whatever it returns.
 */
public interface OnInboxButtonClickListener {
    /**
     * Called when the user taps an inline CTA button on a rich inbox card.
     *
     * @return {@code true} to let the SDK perform the button's default action
     * — open the URL, mark read, dismiss, and for a {@code Custom} button
     * mark the message read; {@code false} to suppress all of that.
     */
    boolean onInboxButtonClick(@NonNull InboxMessage message, @NonNull InboxCardButton button);
}
