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
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxTab
import com.pushwoosh.inbox.data.InboxTabs
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.model.repository.InboxRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * What the Inbox screen shows above the list: the bar exists only when the backend sent tabs,
 * "All" is first and selected, and each tab shows its own messages.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxTabsPanelTest {

    private val tabs = InboxTabs("All", listOf(InboxTab("bills", "Bills"), InboxTab("promos", "Promos")))

    private val messages = listOf(
        fakeInboxMessage(code = "b1", message = "September statement", tabId = "bills"),
        fakeInboxMessage(code = "p1", message = "Autumn sale", tabId = "promos"),
        fakeInboxMessage(code = "n1", message = "Welcome", tabId = null),
        fakeInboxMessage(code = "x1", message = "From a removed tab", tabId = "gone")
    )

    @After
    fun tearDown() {
        resetUiRepository()
    }

    @Test
    fun noTabs_noPanelAndTheFeedIsUnchanged() {
        withInbox(messages, tabs = null) { activity, _ ->
            assertEquals(View.GONE, activity.tabLayout().visibility)
            assertEquals(listOf("b1", "n1", "p1", "x1"), activity.adapterCodes().sorted())
        }
    }

    @Test
    fun withTabs_allIsFirstAndSelected() {
        withInbox(messages, tabs) { activity, _ ->
            assertEquals(View.VISIBLE, activity.tabLayout().visibility)
            assertEquals(listOf("All  4", "Bills  1", "Promos  1"), activity.tabTitles())
            assertEquals(0, activity.tabLayout().selectedTabPosition)
        }
    }

    /** x1 belongs to a tab the Control Panel has since dropped: it must not disappear from the screen. */
    @Test
    fun allTab_showsEveryMessage() {
        withInbox(messages, tabs) { activity, _ ->
            assertEquals(listOf("b1", "n1", "p1", "x1"), activity.adapterCodes().sorted())
        }
    }

    @Test
    fun ownTab_showsOnlyItsMessages() {
        withInbox(messages, tabs) { activity, _ ->
            activity.selectTab(1)
            assertEquals(listOf("b1"), activity.adapterCodes())

            activity.selectTab(2)
            assertEquals(listOf("p1"), activity.adapterCodes())
        }
    }

    @Test
    fun selectedTabSurvivesRecreate() {
        resetUiRepository()
        mockStatic(PushwooshInbox::class.java).use { statics ->
            stubCore(statics, messages, tabs)
            val controller = Robolectric.buildActivity(InboxActivity::class.java).setup()
            controller.get().selectTab(1)

            val recreated = controller.recreate().get()
            recreated.layoutList()

            assertEquals(1, recreated.tabLayout().selectedTabPosition)
            assertEquals(listOf("b1"), recreated.adapterCodes())
        }
    }

    /**
     * Re-entering the Inbox from a bottom navigation is an open, not a configuration change: the
     * contract asks for "All" again, even though the fragment is restored from its own saved state.
     */
    @Test
    fun reopenedInbox_startsOnAllAgain() {
        resetUiRepository()
        mockStatic(PushwooshInbox::class.java).use { statics ->
            stubCore(statics, messages, tabs)
            val activity = Robolectric.buildActivity(InboxActivity::class.java).setup().get()
            activity.selectTab(1)
            assertEquals(listOf("b1"), activity.adapterCodes())

            activity.reopenInbox()

            assertEquals(0, activity.tabLayout().selectedTabPosition)
            assertEquals(listOf("b1", "n1", "p1", "x1"), activity.adapterCodes().sorted())
        }
    }

    @Test
    fun selectedTabGoneFromTheNewList_fallsBackToAll() {
        resetUiRepository()
        mockStatic(PushwooshInbox::class.java).use { statics ->
            stubCore(statics, messages, tabs)
            val activity = Robolectric.buildActivity(InboxActivity::class.java).setup().get()
            activity.selectTab(2)

            val withoutPromos = InboxTabs("All", listOf(InboxTab("bills", "Bills")))
            statics.`when`<InboxTabs?> { PushwooshInbox.getTabs() }.thenReturn(withoutPromos)
            InboxRepository.loadInbox(true, null, -1)
            activity.layoutList()

            assertEquals(listOf("All  4", "Bills  1"), activity.tabTitles())
            assertEquals(0, activity.tabLayout().selectedTabPosition)
            assertEquals(listOf("b1", "n1", "p1", "x1"), activity.adapterCodes().sorted())
        }
    }

    @Test
    fun tabsRemovedWhileATabIsSelected_hidesThePanelAndShowsTheWholeFeed() {
        withInbox(messages, tabs) { activity, statics ->
            activity.selectTab(1)

            statics.`when`<InboxTabs?> { PushwooshInbox.getTabs() }.thenReturn(null)
            InboxRepository.loadInbox(true, null, -1)
            activity.layoutList()

            assertEquals(View.GONE, activity.tabLayout().visibility)
            assertEquals(listOf("b1", "n1", "p1", "x1"), activity.adapterCodes().sorted())
        }
    }

    /** Same tab count, different ids: the bar must be rebuilt, not just relabelled by index. */
    @Test
    fun tabReplacedAtTheSameCount_filtersByTheNewTabId() {
        val withNews = messages + fakeInboxMessage(code = "s1", message = "Release notes", tabId = "news")
        withInbox(withNews, tabs) { activity, statics ->
            val billsAndNews = InboxTabs("All", listOf(InboxTab("bills", "Bills"), InboxTab("news", "News")))
            statics.`when`<InboxTabs?> { PushwooshInbox.getTabs() }.thenReturn(billsAndNews)
            InboxRepository.loadInbox(true, null, -1)
            activity.layoutList()

            activity.selectTab(2)

            assertEquals(listOf("s1"), activity.adapterCodes())
        }
    }
}
