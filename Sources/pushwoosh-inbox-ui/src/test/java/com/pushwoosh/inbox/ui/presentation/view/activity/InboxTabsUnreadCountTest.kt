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

import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxTab
import com.pushwoosh.inbox.data.InboxTabs
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.fakeInboxMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The number next to a tab name is how many unread messages that tab holds, "All" included, and it
 * disappears at zero. Opening a tab reads the rows it shows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxTabsUnreadCountTest {

    private val tabs = InboxTabs("All", listOf(InboxTab("bills", "Bills"), InboxTab("promos", "Promos")))

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.automaticReadOnDisplay = true
        resetUiRepository()
    }

    @Test
    fun unreadCountsAreShownPerTabIncludingAll() {
        val messages = listOf(
            fakeInboxMessage(code = "b1", tabId = "bills", read = false),
            fakeInboxMessage(code = "b2", tabId = "bills", read = false),
            fakeInboxMessage(code = "p1", tabId = "promos", read = true),
            fakeInboxMessage(code = "n1", tabId = null, read = false)
        )

        withInbox(messages, tabs) { activity, _ ->
            assertEquals(listOf("All  3", "Bills  2", "Promos"), activity.tabTitles())
        }
    }

    @Test
    fun everythingRead_hidesEveryCounter() {
        val messages = listOf(
            fakeInboxMessage(code = "b1", tabId = "bills", read = true),
            fakeInboxMessage(code = "p1", tabId = "promos", read = true)
        )

        withInbox(messages, tabs) { activity, _ ->
            assertEquals(listOf("All", "Bills", "Promos"), activity.tabTitles())
        }
    }

    @Test
    fun messageOfARemovedTabStillCountsOnAll() {
        val messages = listOf(fakeInboxMessage(code = "x1", tabId = "gone", read = false))

        withInbox(messages, tabs) { activity, _ ->
            assertEquals(listOf("All  1", "Bills", "Promos"), activity.tabTitles())
        }
    }

    @Test
    fun switchingTab_readsTheRowsItShows() {
        val messages = listOf(
            fakeInboxMessage(code = "b1", tabId = "bills", read = false),
            fakeInboxMessage(code = "p1", tabId = "promos", read = false)
        )

        withInbox(messages, tabs) { activity, statics ->
            activity.selectTab(1)

            statics.verify { PushwooshInbox.readMessages(listOf("b1")) }
        }
    }

    @Test
    fun automaticReadOnDisplayOff_switchingTabReadsNothing() {
        PushwooshInboxStyle.automaticReadOnDisplay = false
        val messages = listOf(fakeInboxMessage(code = "b1", tabId = "bills", read = false))

        withInbox(messages, tabs) { activity, statics ->
            activity.selectTab(1)

            statics.verify({ PushwooshInbox.readMessages(org.mockito.ArgumentMatchers.any()) }, org.mockito.Mockito.never())
        }
    }
}
