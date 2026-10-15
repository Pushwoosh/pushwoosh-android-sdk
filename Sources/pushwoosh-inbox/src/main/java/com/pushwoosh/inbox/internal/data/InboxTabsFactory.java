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

package com.pushwoosh.inbox.internal.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.inbox.data.InboxTab;
import com.pushwoosh.inbox.data.InboxTabs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds {@link InboxTabs} from the {@code inbox_tabs} payload.
 *
 * <p>The only reader of the tabs text, whether it has just arrived with {@code getInboxMessages} or
 * comes back from prefs after a restart. Anything that is not a usable list — not JSON, a wrong
 * type, a blank title, no surviving entry — is reported as {@code null}, meaning "no tabs, one
 * feed". Nothing here throws: a bad tabs payload must not cost the inbox its messages.
 */
public final class InboxTabsFactory {
    private static final String ALL_TITLE = "all_title";
    private static final String TABS = "tabs";
    private static final String ID = "id";
    private static final String TITLE = "title";

    private InboxTabsFactory() {}

    @Nullable public static InboxTabs fromRawJson(@Nullable String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            return null;
        }
        try {
            return fromJson(new JSONObject(rawJson));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable private static InboxTabs fromJson(@NonNull JSONObject inboxTabsJson) {
        String allTitle = nonEmptyString(inboxTabsJson.opt(ALL_TITLE));
        if (allTitle == null) {
            return null;
        }

        JSONArray tabsJson = inboxTabsJson.optJSONArray(TABS);
        if (tabsJson == null) {
            return null;
        }

        List<InboxTab> tabs = new ArrayList<>(tabsJson.length());
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < tabsJson.length(); i++) {
            JSONObject tabJson = tabsJson.optJSONObject(i);
            if (tabJson == null) {
                continue;
            }
            String id = nonEmptyString(tabJson.opt(ID));
            String title = nonEmptyString(tabJson.opt(TITLE));
            if (id == null || title == null || !seenIds.add(id)) {
                continue;
            }
            tabs.add(new InboxTab(id, title));
        }

        return tabs.isEmpty() ? null : new InboxTabs(allTitle, tabs);
    }

    @Nullable private static String nonEmptyString(@Nullable Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String string = (String) value;
        return string.isEmpty() ? null : string;
    }
}
