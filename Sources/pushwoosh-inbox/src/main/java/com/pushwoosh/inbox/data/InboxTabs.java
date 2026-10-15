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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Inbox tab bar configured in the Pushwoosh Control Panel.
 *
 * <p>Instances are immutable. {@link #getTabs()} is never empty and keeps the order the backend
 * sent; the "All" tab is not part of it — it is always first in the UI and is named by
 * {@link #getAllTitle()}.
 *
 * @see com.pushwoosh.inbox.PushwooshInbox#getTabs()
 */
public final class InboxTabs implements Serializable {
    private static final long serialVersionUID = -6611943286205012207L;

    private final String allTitle;
    private final List<InboxTab> tabs;

    public InboxTabs(@NonNull String allTitle, @NonNull List<InboxTab> tabs) {
        this.allTitle = allTitle;
        this.tabs = Collections.unmodifiableList(new ArrayList<>(tabs));
    }

    /**
     * @return name of the "All" tab, which shows every message regardless of its tab
     */
    @NonNull public String getAllTitle() {
        return allTitle;
    }

    /**
     * @return the application's own tabs, in backend order, never empty
     */
    @NonNull public List<InboxTab> getTabs() {
        return tabs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InboxTabs that = (InboxTabs) o;
        return allTitle.equals(that.allTitle) && tabs.equals(that.tabs);
    }

    @Override
    public int hashCode() {
        return 31 * allTitle.hashCode() + tabs.hashCode();
    }

    @Override
    public String toString() {
        return "InboxTabs{allTitle='" + allTitle + "', tabs=" + tabs + "}";
    }
}
