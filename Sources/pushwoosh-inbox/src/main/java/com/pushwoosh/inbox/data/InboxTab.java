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

package com.pushwoosh.inbox.data;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * One Inbox tab configured in the Pushwoosh Control Panel.
 *
 * <p>Instances are immutable. The id is opaque and stable: renaming or reordering a tab in the
 * Control Panel does not change it.
 *
 * @see InboxTabs
 * @see com.pushwoosh.inbox.PushwooshInbox#getTabs()
 */
public final class InboxTab implements Serializable {
    private static final long serialVersionUID = 2894501184712093341L;

    private final String id;
    private final String title;

    public InboxTab(@NonNull String id, @NonNull String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * @return opaque tab identifier, matched against {@link InboxMessage#getTabId()}
     */
    @NonNull public String getId() {
        return id;
    }

    /**
     * @return tab name, already translated by the backend into the device language
     */
    @NonNull public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InboxTab that = (InboxTab) o;
        return id.equals(that.id) && title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + title.hashCode();
    }

    @Override
    public String toString() {
        return "InboxTab{id='" + id + "', title='" + title + "'}";
    }
}
