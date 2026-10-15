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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.data.InboxTabs
import com.pushwoosh.inbox.exception.InboxMessagesException
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.model.repository.InboxRepository
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxAdapter
import com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.isNull
import org.mockito.MockedStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric

/**
 * The core with the cache and the server told apart: the open path reads [cached], the sync answers
 * with [syncAnswer], and holding either answer freezes the screen where a test can read it.
 */
class InboxCore internal constructor(
    val statics: MockedStatic<PushwooshInbox>,
    private val cached: List<InboxMessage>,
    private val cacheAnswer: Result<Collection<InboxMessage>, InboxMessagesException>,
    private val syncAnswer: Result<Collection<InboxMessage>, InboxMessagesException>,
    private val holdCacheRead: Boolean,
    private val holdSync: Boolean
) {
    /** How many times the screen asked the core to sync with the server. */
    var syncRequests = 0
        private set

    private val heldSyncs = mutableListOf<Callback<Collection<InboxMessage>, InboxMessagesException>>()

    internal fun stub(tabs: InboxTabs?) {
        statics.`when`<InboxTabs?> { PushwooshInbox.getTabs() }.thenReturn(tabs)
        statics.`when`<Collection<InboxMessage>> { PushwooshInbox.loadCachedMessages(isNull(), anyInt()) }
            .thenReturn(cached)
        statics.`when`<Unit> { PushwooshInbox.loadCachedMessages(any(), isNull(), anyInt()) }
            .thenAnswer { invocation ->
                val callback = callbackOf(invocation.arguments[0])
                if (!holdCacheRead) callback.process(cacheAnswer)
                null
            }
        statics.`when`<Unit> { PushwooshInbox.loadMessages(any(), isNull(), anyInt()) }
            .thenAnswer { invocation ->
                syncRequests++
                val callback = callbackOf(invocation.arguments[0])
                if (holdSync) heldSyncs.add(callback) else callback.process(syncAnswer)
                null
            }
    }

    /** Lets every held sync answer, in the order the screen asked for them. */
    fun releaseSyncs() {
        check(heldSyncs.isNotEmpty()) { "no sync is waiting" }
        val waiting = heldSyncs.toList()
        heldSyncs.clear()
        waiting.forEach { it.process(syncAnswer) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun callbackOf(raw: Any?): Callback<Collection<InboxMessage>, InboxMessagesException> =
        raw as Callback<Collection<InboxMessage>, InboxMessagesException>
}

/** The read answers with these messages. */
fun answerWith(vararg messages: InboxMessage): Result<Collection<InboxMessage>, InboxMessagesException> =
    Result.fromData(messages.toList())

/** The read answers with nothing at all. */
fun answerNull(): Result<Collection<InboxMessage>, InboxMessagesException> =
    Result.fromData(null)

/**
 * The read fails. The core reports a failed sync as the storage it holds plus the exception
 * (GetInboxMessagesTask: Result.from(inboxMessages, exception)), not as a bare exception, so the
 * fake answers the same shape.
 */
fun answerFailure(message: String = "offline"): Result<Collection<InboxMessage>, InboxMessagesException> =
    Result.from(emptyList(), InboxMessagesException(message))

/**
 * Opens the stock Inbox screen over a core whose cache and server answer separately: everything
 * below the static facade (presenter, fragment, adapter) is the real thing, and the assertions read
 * what was drawn.
 */
fun withInboxCore(
    cached: List<InboxMessage> = emptyList(),
    cacheAnswer: Result<Collection<InboxMessage>, InboxMessagesException> = Result.fromData(cached),
    syncAnswer: Result<Collection<InboxMessage>, InboxMessagesException> = Result.fromData(emptyList()),
    tabs: InboxTabs? = null,
    holdCacheRead: Boolean = false,
    holdSync: Boolean = false,
    body: (InboxActivity, InboxCore) -> Unit
) {
    resetUiRepository()
    mockStatic(PushwooshInbox::class.java).use { statics ->
        val core = InboxCore(statics, cached, cacheAnswer, syncAnswer, holdCacheRead, holdSync)
        core.stub(tabs)
        val activity = Robolectric.buildActivity(InboxActivity::class.java).setup().get()
        activity.layoutList()
        body(activity, core)
    }
}

/**
 * Builds the stock Inbox screen on top of a stubbed core: the tabs and the message list are given,
 * everything below (presenter, fragment, adapter) is the real thing, and the assertions read what
 * was drawn.
 */
fun withInbox(
    messages: List<InboxMessage>,
    tabs: InboxTabs?,
    body: (InboxActivity, MockedStatic<PushwooshInbox>) -> Unit
) {
    withInboxCore(
        cached = messages,
        syncAnswer = Result.fromData(messages),
        tabs = tabs
    ) { activity, core -> body(activity, core.statics) }
}

/** Stubs an already-mocked facade the way [withInbox] does, for tests that build the activity themselves. */
fun stubCore(statics: MockedStatic<PushwooshInbox>, messages: List<InboxMessage>, tabs: InboxTabs?) {
    InboxCore(
        statics = statics,
        cached = messages,
        cacheAnswer = Result.fromData(messages),
        syncAnswer = Result.fromData(messages),
        holdCacheRead = false,
        holdSync = false
    ).stub(tabs)
}

/** The ui repository is an object that replays its last event to every new subscriber. */
@Suppress("UNCHECKED_CAST")
fun resetUiRepository() {
    val type = InboxRepository::class.java
    type.getDeclaredField("currentInboxEvent").apply { isAccessible = true }.set(InboxRepository, null)
    (type.getDeclaredField("currentInboxMessages").apply { isAccessible = true }.get(InboxRepository)
            as MutableList<Any>).clear()
    (type.getDeclaredField("callbacks").apply { isAccessible = true }.get(InboxRepository)
            as MutableList<Any>).clear()
}

fun InboxActivity.tabLayout(): TabLayout = findViewById(R.id.inboxTabLayout)

fun InboxActivity.tabTitles(): List<String> {
    val tabs = tabLayout()
    return (0 until tabs.tabCount).map { tabs.getTabAt(it)!!.text.toString() }
}

fun InboxActivity.selectTab(index: Int) {
    val tabs = tabLayout()
    tabs.selectTab(tabs.getTabAt(index))
    layoutList()
}

/**
 * Leaves the Inbox and comes back the way a bottom-navigation host does: the activity stays alive
 * while the fragment is destroyed and a fresh one is built from the state the old one saved.
 */
fun InboxActivity.reopenInbox() {
    val manager = supportFragmentManager
    val leaving = manager.findFragmentByTag(InboxActivity.FRAGMENT_TAG)!!
    val savedState = manager.saveFragmentInstanceState(leaving)
    manager.beginTransaction().remove(leaving).commitNow()

    val returning = InboxFragment()
    returning.setInitialSavedState(savedState)
    manager.beginTransaction()
        .add(R.id.inboxContentContainer, returning, InboxActivity.FRAGMENT_TAG)
        .commitNow()
    layoutList()
}

fun InboxActivity.recycler(): RecyclerView = findViewById(R.id.inboxRecyclerView)

fun InboxActivity.progressBar(): View = findViewById(R.id.inboxTotalProgressBar)

fun InboxActivity.emptyPanel(): View = findViewById(R.id.inboxEmpty)

fun InboxActivity.errorPanel(): View = findViewById(R.id.inboxError)

fun InboxActivity.adapterCodes(): List<String> {
    val adapter = recycler().adapter as InboxAdapter
    return (0 until adapter.itemCount).map { adapter.getItem(it)!!.code }
}

/** Robolectric does not lay views out; the list must be measured for visible-position queries. */
fun InboxActivity.layoutList() {
    val recycler = recycler()
    recycler.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
    )
    recycler.layout(0, 0, 1080, 1920)
}
