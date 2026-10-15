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
import android.widget.TextView
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxTab
import com.pushwoosh.inbox.data.InboxTabs
import com.pushwoosh.inbox.event.InboxMessagesUpdatedEventBuilder
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.model.repository.InboxRepository
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxAdapter
import com.pushwoosh.internal.event.EventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The empty state, the error state and a swipe all read the whole inbox, not the rows the current
 * tab happens to show: an empty tab is not an empty inbox, and a deleted message must not come
 * back when the user switches tabs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxTabsStatesTest {

    private val tabs = InboxTabs("All", listOf(InboxTab("bills", "Bills"), InboxTab("promos", "Promos")))

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        resetUiRepository()
    }

    @Test
    fun emptyTab_showsEmptyViewAndKeepsThePanel() {
        val messages = listOf(fakeInboxMessage(code = "b1", tabId = "bills", read = true))

        withInbox(messages, tabs) { activity, _ ->
            activity.selectTab(2)

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.inboxEmpty).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.inboxRecyclerView).visibility)
            assertEquals("the tab bar stays on an empty tab", View.VISIBLE, activity.tabLayout().visibility)
            assertEquals(listOf("All", "Bills", "Promos"), activity.tabTitles())
        }
    }

    @Test
    fun networkErrorOnAnEmptyTabOfANonEmptyInbox_isNotTheFullScreenError() {
        val messages = listOf(fakeInboxMessage(code = "b1", tabId = "bills"))

        withInbox(messages, tabs) { activity, _ ->
            activity.selectTab(2)

            InboxRepository.loadInbox(true, null, -1)
            fragmentOf(activity).failedLoadingInboxList(
                com.pushwoosh.inbox.ui.presentation.data.UserError(message = "offline")
            )

            assertEquals(
                "a non-empty inbox never gets the full-screen error",
                View.GONE,
                activity.findViewById<View>(R.id.inboxError).visibility
            )

            val snackbarText = activity.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            assertNotNull("the error must still reach the user as a snackbar", snackbarText)
            assertEquals("offline", snackbarText!!.text.toString())
        }
    }

    @Test
    fun networkErrorOnAnEmptyInbox_isTheFullScreenError() {
        withInbox(emptyList(), tabs) { activity, _ ->
            fragmentOf(activity).failedLoadingInboxList(
                com.pushwoosh.inbox.ui.presentation.data.UserError(message = "offline")
            )

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.inboxError).visibility)
        }
    }

    @Test
    fun inboxBecameEmpty_dropsCountersAndNextErrorIsFullScreen() {
        val messages = listOf(fakeInboxMessage(code = "b1", tabId = "bills"))

        withInbox(messages, tabs) { activity, _ ->
            val fragment = fragmentOf(activity)
            fragment.showEmptyView()

            assertEquals(listOf("All", "Bills", "Promos"), activity.tabTitles())

            fragment.failedLoadingInboxList(com.pushwoosh.inbox.ui.presentation.data.UserError(message = "offline"))

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.inboxError).visibility)
        }
    }

    /** The first sync of an empty inbox brings tabs but no messages: the empty screen must still show them. */
    @Test
    fun emptyInbox_showsTabsThatArriveWithAnEmptySync() {
        withInbox(emptyList(), tabs = null) { activity, statics ->
            assertEquals(View.GONE, activity.tabLayout().visibility)

            statics.`when`<InboxTabs?> { PushwooshInbox.getTabs() }.thenReturn(tabs)
            EventBus.sendEvent(InboxMessagesUpdatedEventBuilder().build())

            assertEquals(View.VISIBLE, activity.tabLayout().visibility)
            assertEquals(listOf("All", "Bills", "Promos"), activity.tabTitles())
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.inboxEmpty).visibility)
        }
    }

    /** Review Focus 5: a swiped message must not reappear when the user goes back to All. */
    @Test
    fun swipeDeleteInATab_removesTheMessageFromTheWholeList() {
        val messages = listOf(
            fakeInboxMessage(code = "b1", tabId = "bills"),
            fakeInboxMessage(code = "p1", tabId = "promos")
        )

        withInbox(messages, tabs) { activity, _ ->
            activity.selectTab(1)
            val adapter = activity.recycler().adapter as InboxAdapter
            adapter.onItemSwiped(0)
            activity.layoutList()

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.inboxEmpty).visibility)
            assertEquals("the tab bar stays", View.VISIBLE, activity.tabLayout().visibility)
            assertEquals(listOf("All  1", "Bills", "Promos  1"), activity.tabTitles())

            activity.selectTab(0)
            assertFalse("the swiped message must not come back", activity.adapterCodes().contains("b1"))
            assertEquals(listOf("p1"), activity.adapterCodes())
        }
    }

    private fun fragmentOf(activity: InboxActivity) =
        activity.supportFragmentManager.findFragmentByTag(InboxActivity.FRAGMENT_TAG)
                as com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment
}
