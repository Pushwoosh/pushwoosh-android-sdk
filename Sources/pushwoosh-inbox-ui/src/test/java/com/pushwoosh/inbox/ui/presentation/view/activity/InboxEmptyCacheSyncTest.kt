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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Opening the Inbox always means "show the cache, then sync": an empty cache is not an empty Inbox,
 * and only the result of a sync may tell the user there is nothing there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxEmptyCacheSyncTest {

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        resetUiRepository()
        resetSnackbarManagerSingleton()
    }

    /**
     * [com.google.android.material.snackbar.SnackbarManager] is a process-wide singleton whose
     * Handler is bound to whichever Looper happened to be the main Looper the first time any
     * Snackbar was shown in this test JVM. Robolectric gives every test its own main Looper, so a
     * singleton created by a test would otherwise go on holding that snackbar as "current" for
     * every later test in the module, silently queuing their snackbars instead of showing them.
     * Run unconditionally from [tearDown], so a failed assertion in a test does not skip it.
     */
    private fun resetSnackbarManagerSingleton() {
        val managerClass = Class.forName("com.google.android.material.snackbar.SnackbarManager")
        val instanceField = managerClass.getDeclaredField("snackbarManager")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun emptyCache_whileTheCacheReadIsInFlight_showsProgressNotTheEmptyState() {
        withInboxCore(cached = emptyList(), holdCacheRead = true) { activity, _ ->
            assertEquals(
                "an unread cache is not an empty Inbox",
                View.GONE,
                activity.emptyPanel().visibility
            )
            assertEquals(View.VISIBLE, activity.progressBar().visibility)
        }
    }

    @Test
    fun cachedMessages_areOnScreenBeforeTheCacheReadAnswers() {
        withInboxCore(
            cached = listOf(fakeInboxMessage(code = "c1")),
            holdCacheRead = true
        ) { activity, _ ->
            assertEquals(listOf("c1"), activity.adapterCodes())
            assertEquals(View.VISIBLE, activity.recycler().visibility)
            assertEquals(View.GONE, activity.progressBar().visibility)
        }
    }

    /** Spec scenario 1: messages wait on the server, nothing is cached. */
    @Test
    fun emptyCacheAndMessagesOnTheServer_asksForASyncAndShowsThemWhenItLands() {
        withInboxCore(
            cached = emptyList(),
            syncAnswer = answerWith(fakeInboxMessage(code = "s1")),
            holdSync = true
        ) { activity, core ->
            assertEquals("an empty cache must still reach the server", 1, core.syncRequests)
            assertEquals(View.VISIBLE, activity.progressBar().visibility)
            assertEquals(
                "the screen must not call the Inbox empty before the sync answers",
                View.GONE,
                activity.emptyPanel().visibility
            )

            core.releaseSyncs()
            activity.layoutList()

            assertEquals(listOf("s1"), activity.adapterCodes())
            assertEquals(View.VISIBLE, activity.recycler().visibility)
            assertEquals(View.GONE, activity.emptyPanel().visibility)
        }
    }

    /** Spec scenario 2: nothing cached, nothing on the server. */
    @Test
    fun emptyCacheAndEmptyServer_showsTheEmptyStateAfterExactlyOneSync() {
        withInboxCore(cached = emptyList(), syncAnswer = answerWith()) { activity, core ->
            assertEquals("the empty verdict must not loop back into another sync", 1, core.syncRequests)
            assertEquals(View.VISIBLE, activity.emptyPanel().visibility)
            assertEquals(View.GONE, activity.progressBar().visibility)
        }
    }

    /** Spec scenario 3: nothing cached and no network. */
    @Test
    fun emptyCacheAndAFailedSync_showsTheFullScreenError() {
        withInboxCore(cached = emptyList(), syncAnswer = answerFailure()) { activity, core ->
            assertEquals(1, core.syncRequests)
            assertEquals(View.VISIBLE, activity.errorPanel().visibility)
            assertEquals(
                "a failed first sync is an error, not an empty Inbox",
                View.GONE,
                activity.emptyPanel().visibility
            )
        }
    }

    /** Spec scenario 4: the path that already worked must keep working. */
    @Test
    fun cachedMessages_areShownFirstAndTheSyncReplacesThem() {
        withInboxCore(
            cached = listOf(fakeInboxMessage(code = "c1")),
            syncAnswer = answerWith(fakeInboxMessage(code = "s1")),
            holdSync = true
        ) { activity, core ->
            assertEquals(listOf("c1"), activity.adapterCodes())
            assertEquals("a non-empty cache already asked for a sync", 1, core.syncRequests)

            core.releaseSyncs()
            activity.layoutList()

            assertEquals(listOf("s1"), activity.adapterCodes())
        }
    }

    /** Review Focus 1: a cache read that answers nothing at all is still a cache read. */
    @Test
    fun aCacheReadThatAnswersNothing_stillReachesTheServer() {
        withInboxCore(
            cached = emptyList(),
            cacheAnswer = answerNull(),
            syncAnswer = answerWith(fakeInboxMessage(code = "s1"))
        ) { activity, core ->
            assertEquals(1, core.syncRequests)
            assertEquals(listOf("s1"), activity.adapterCodes())
        }
    }

    /** Review Focus 2: a broken local storage is an error, and it is not a reason to call the server. */
    @Test
    fun aFailedCacheRead_showsTheErrorAndNeverAsksForASync() {
        withInboxCore(
            cached = emptyList(),
            cacheAnswer = answerFailure("storage is gone")
        ) { activity, core ->
            assertEquals("a broken cache must not start a sync", 0, core.syncRequests)
            assertEquals(View.VISIBLE, activity.errorPanel().visibility)
        }
    }

    /** Review Focus 3: the repository replays its last event, and the replay must not win over an open. */
    @Test
    fun reopeningAnEmptyInbox_asksTheCoreAgain() {
        withInboxCore(cached = emptyList(), syncAnswer = answerWith()) { activity, core ->
            assertEquals(View.VISIBLE, activity.emptyPanel().visibility)

            activity.reopenInbox()

            assertEquals("every open asks the core; the throttle is the core's call", 2, core.syncRequests)
        }
    }

    /** Review Focus 4: the core reports a failed sync as its storage plus the exception. */
    @Test
    fun aFailedSyncOverAFullInbox_keepsTheListAndReportsInASnackbar() {
        withInboxCore(
            cached = listOf(fakeInboxMessage(code = "c1")),
            syncAnswer = answerFailure(),
            holdSync = true
        ) { activity, core ->
            // Held so the failure resolves after the cached row is on screen, not during activity setup.
            core.releaseSyncs()
            activity.layoutList()

            assertEquals("the cached list survives a failed sync", listOf("c1"), activity.adapterCodes())
            assertEquals(
                "a non-empty inbox never gets the full-screen error",
                View.GONE,
                activity.errorPanel().visibility
            )

            val snackbarText = activity.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            assertNotNull("the error must still reach the user as a snackbar", snackbarText)
        }
    }

    /** Review Focus 5: leaving and coming back while the first sync is still in flight. */
    @Test
    fun reopeningWhileTheFirstSyncIsInFlight_endsOnTheServerList() {
        withInboxCore(
            cached = emptyList(),
            syncAnswer = answerWith(fakeInboxMessage(code = "s1")),
            holdSync = true
        ) { activity, core ->
            activity.reopenInbox()
            assertEquals(2, core.syncRequests)

            core.releaseSyncs()
            activity.layoutList()

            assertEquals(listOf("s1"), activity.adapterCodes())
            assertEquals(View.GONE, activity.emptyPanel().visibility)
        }
    }

    /**
     * Fix wave: a presenter recreated after a pull-to-refresh must open like any other open, not
     * replay the swipe-refresh progress style and leave the stale empty panel on screen.
     */
    @Test
    fun recreatingAfterAPullToRefreshOnAnEmptyInbox_doesNotKeepTheEmptyPanelDuringTheNewSync() {
        withInboxCore(cached = emptyList(), syncAnswer = answerWith(), holdSync = true) { activity, core ->
            assertEquals(1, core.syncRequests)
            core.releaseSyncs()
            activity.layoutList()
            assertEquals(View.VISIBLE, activity.emptyPanel().visibility)

            // Pull to refresh over the empty Inbox: the server still has nothing, swipeToRefresh latches true.
            activity.pullToRefresh()
            assertEquals(2, core.syncRequests)
            core.releaseSyncs()
            activity.layoutList()
            assertEquals(View.VISIBLE, activity.emptyPanel().visibility)

            activity.reopenInbox()
            assertEquals("the restored open must still reach the server", 3, core.syncRequests)
            assertEquals(
                "a restored open is not a pull-to-refresh: progress must replace the stale empty panel",
                View.GONE,
                activity.emptyPanel().visibility
            )

            core.releaseSyncs()
            activity.layoutList()
            assertEquals(
                "the server confirmed empty again: the empty panel is correct once the sync lands",
                View.VISIBLE,
                activity.emptyPanel().visibility
            )
        }
    }

    /**
     * Team review: a pull-to-refresh over a non-empty cached list must survive a rotation without
     * the restored open forcing the full-screen progress bar over the list the user was reading.
     */
    @Test
    fun recreatingAfterAPullToRefreshOnANonEmptyInbox_keepsTheListVisibleDuringTheRestoredSync() {
        withInboxCore(
            cached = listOf(fakeInboxMessage(code = "c1")),
            syncAnswer = answerWith(fakeInboxMessage(code = "c1")),
            holdSync = true
        ) { activity, core ->
            assertEquals(1, core.syncRequests)
            core.releaseSyncs()
            activity.layoutList()
            assertEquals(listOf("c1"), activity.adapterCodes())

            // Pull to refresh over the non-empty Inbox and hold the sync so it is still in flight at rotation.
            activity.pullToRefresh()
            assertEquals(2, core.syncRequests)

            activity.reopenInbox()
            assertEquals("the restored open must still reach the server", 3, core.syncRequests)

            assertEquals(
                "a restored open mid pull-to-refresh must keep the cached list, not hide it behind the full-screen progress bar",
                View.VISIBLE,
                activity.recycler().visibility
            )
            assertEquals(View.GONE, activity.progressBar().visibility)
            assertEquals(listOf("c1"), activity.adapterCodes())

            core.releaseSyncs()
            activity.layoutList()
        }
    }

    /**
     * Robolectric does not simulate the drag distance SwipeRefreshLayout needs to fire its listener
     * on its own, so the listener is invoked directly, the way the real gesture would.
     */
    private fun InboxActivity.pullToRefresh() {
        val layout = findViewById<SwipeRefreshLayout>(R.id.inboxSwipeRefreshLayout)
        val listenerField = SwipeRefreshLayout::class.java.getDeclaredField("mListener")
        listenerField.isAccessible = true
        (listenerField.get(layout) as SwipeRefreshLayout.OnRefreshListener).onRefresh()
    }
}
