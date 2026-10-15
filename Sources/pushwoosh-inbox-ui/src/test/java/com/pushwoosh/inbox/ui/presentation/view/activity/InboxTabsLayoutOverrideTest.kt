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


package com.pushwoosh.inbox.ui.presentation.view.activity

import android.view.View
import com.pushwoosh.inbox.data.InboxTab
import com.pushwoosh.inbox.data.InboxTabs
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * An app that overrides pw_fragment_inbox.xml wrote its copy before the tabs existed, so the panel
 * is simply not in the view tree. Such an Inbox has to go on working as it did before the update:
 * one feed, no panel, no crash.
 *
 * The override is the layout-land fixture in the test resources; the qualifier is how the test asks
 * resource merging for it, and stands in for the app's res/layout winning over the library's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "land")
@LooperMode(LooperMode.Mode.LEGACY)
class InboxTabsLayoutOverrideTest {

    private val tabs = InboxTabs("All", listOf(InboxTab("bills", "Bills"), InboxTab("promos", "Promos")))

    private val messages = listOf(
        fakeInboxMessage(code = "b1", message = "September statement", tabId = "bills"),
        fakeInboxMessage(code = "p1", message = "Autumn sale", tabId = "promos"),
        fakeInboxMessage(code = "n1", message = "Welcome", tabId = null)
    )

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        resetUiRepository()
    }

    @Test
    fun layoutWithoutThePanel_showsTheWholeFeed() {
        withInbox(messages, tabs) { activity, _ ->
            assertNull("the fixture must be the layout in play", activity.findViewById<View>(R.id.inboxTabLayout))
            assertEquals(View.VISIBLE, activity.recycler().visibility)
            assertEquals(listOf("b1", "n1", "p1"), activity.adapterCodes().sorted())
        }
    }
}
