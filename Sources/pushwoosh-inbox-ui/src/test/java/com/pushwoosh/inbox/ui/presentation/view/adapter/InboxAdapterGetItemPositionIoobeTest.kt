/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inbox.ui.presentation.view.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.data.InboxMessageType
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression guard for candidate #28 crash-inboxadapter-getitem-position-ioobe.
 *
 * Was: [BaseRecyclerAdapter.getItem] did `return mDataList[position]` with no bounds
 * check, so any out-of-range index (-1 from a cleared ViewHolder, or a stale captured
 * click index >= size) threw [IndexOutOfBoundsException] through two unwrapped
 * RecyclerView/ItemTouchHelper callbacks ([InboxAdapter.onItemSwiped] / the click
 * closure built in [InboxAdapter.onBindViewHolder]).
 *
 * Fix: `getItem` now returns `mDataList.getOrNull(position)` (it is already `Model?`),
 * and [InboxAdapter.onItemSwiped] ignores an out-of-range position before touching the
 * backing list. These tests assert the graceful behaviour the fix introduced — no throw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxAdapterGetItemPositionIoobeTest {

    private fun ctx(): Context = RuntimeEnvironment.getApplication()

    /** A no-op ColorSchemeProvider so we can build a real InboxAdapter / callback without a theme. */
    private fun fakeColorScheme(): ColorSchemeProvider = object : ColorSchemeProvider {
        override val cellBackground: Drawable? = null
        override val titleColor: ColorStateList = ColorStateList.valueOf(0)
        override val descriptionColor: ColorStateList = ColorStateList.valueOf(0)
        override val dateColor: ColorStateList = ColorStateList.valueOf(0)
        override val divider: Drawable? = null
        override val accentColor: Int = 0
        override val imageColor: ColorStateList = ColorStateList.valueOf(0)
        override val defaultIcon: Drawable? = null
        override val backgroundColor: Int = 0
    }

    private fun msg(code: String): InboxMessage = object : InboxMessage {
        override fun getCode(): String = code
        override fun getTitle(): String? = null
        override fun getImageUrl(): String? = null
        override fun getMessage(): String = "m-$code"
        override fun getSendDate(): Date = Date(0L)
        override fun getISO8601SendDate(): String = "1970-01-01T00:00:00Z"
        override fun getType(): InboxMessageType = InboxMessageType.PLAIN
        override fun getBannerUrl(): String? = null
        override fun getActionParams(): String? = null
        override fun isRead(): Boolean = false
        override fun isActionPerformed(): Boolean = false
        override fun compareTo(other: InboxMessage): Int = code.compareTo(other.code)
    }

    private fun newAdapter(): InboxAdapter =
        InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    /** An unowned RecyclerView.ViewHolder — mBindingAdapter is null, so adapterPosition == -1. */
    private fun unownedHolder(): RecyclerView.ViewHolder {
        val v = View(ctx())
        return object : RecyclerView.ViewHolder(v) {}
    }

    // ---- Path 1 (swipe NO_POSITION): real onSwiped(unownedHolder) -> onItemSwiped(-1) is a no-op ----

    @Test
    fun path1_swipeNoPosition_isIgnoredGracefully() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b")))
        // Production wires this in InboxFragment.onViewCreated:161; without it the `:65`
        // safe-call short-circuits and getItem is never evaluated.
        var removed = false
        adapter.onItemRemoved = { removed = true }
        val callback = SimpleItemTouchHelperCallback(adapter, ctx(), fakeColorScheme())
        val holder = unownedHolder()
        // sanity: the holder genuinely yields NO_POSITION (the production condition)
        assertEquals(RecyclerView.NO_POSITION, holder.adapterPosition)

        // Was IndexOutOfBoundsException; now a clean no-op.
        callback.onSwiped(holder, androidx.recyclerview.widget.ItemTouchHelper.START)

        // The out-of-range swipe removed nothing and did not notify a removal.
        assertEquals(2, adapter.itemCount)
        assertEquals(false, removed)
    }

    // ---- Crash-point: InboxAdapter.onItemSwiped(-1) directly -> graceful no-op ----

    @Test
    fun crashPoint_onItemSwipedMinusOne_isIgnoredGracefully() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b")))
        var removed = false
        adapter.onItemRemoved = { removed = true }

        adapter.onItemSwiped(RecyclerView.NO_POSITION) // -1, was IOOBE

        assertEquals(2, adapter.itemCount)
        assertEquals(false, removed)
    }

    // ---- Crash-point (upper bound): onItemSwiped(>= size) -> graceful no-op ----
    // The guard has two arms (position < 0 || position >= size); this exercises the
    // `>= size` arm — the swipe analogue of a stale index — distinct from the -1 arm above.

    @Test
    fun crashPoint_onItemSwipedAtOrAboveSize_isIgnoredGracefully() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b")))
        var removed = false
        adapter.onItemRemoved = { removed = true }

        adapter.onItemSwiped(2) // == size, was IOOBE on removeAt
        adapter.onItemSwiped(99) // > size

        assertEquals(2, adapter.itemCount)
        assertEquals(false, removed)
    }

    // ---- Path 2 (stale click >= size): the captured closure now reads null, no throw ----
    // Mirrors InboxAdapter.onBindViewHolder:74 `{ onItemClick?.invoke(getItem(position)) }`:
    // the closure captures bind-time `position` and later reads getItem(position) on the mutated list.

    @Test
    fun path2_staleClickPosition_yieldsNullGracefully() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b"), msg("c"))) // size 3
        var clicked: InboxMessage? = msg("sentinel")
        adapter.onItemClick = { clicked = it }
        val capturedPosition = 2 // bind-time position of the last item

        // The exact closure onBindViewHolder builds (it calls the adapter's inherited getItem).
        val staleClickListener: () -> Unit = { adapter.onItemClick?.invoke(adapter.getItem(capturedPosition)) }

        // Refresh shrinks the list (showList -> setCollection); the click listener is now stale.
        adapter.setCollection(listOf(msg("a"))) // size 1; index 2 now out of range

        // Was IndexOutOfBoundsException; now the stale index degrades to null.
        staleClickListener.invoke()

        assertNull("stale out-of-range index must read as null, not throw", clicked)
    }

    // ---- Direct unit anchor: getItem(outOfRange) returns null instead of throwing ----

    @Test
    fun getItem_outOfRange_returnsNull() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b")))
        assertNull(adapter.getItem(-1))
        assertNull(adapter.getItem(2))
        assertNull(adapter.getItem(99))
    }

    // ---- Negative control 1: in-range swipe still removes (guard didn't degenerate to no-op) ----

    @Test
    fun negativeControl_validSwipePosition_stillRemoves() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b")))
        var removed: InboxMessage? = null
        adapter.onItemRemoved = { removed = it }
        adapter.onItemSwiped(0) // valid index
        assertEquals("a", removed?.code)
        assertEquals(1, adapter.itemCount)
    }

    // ---- Negative control 2: in-range click still resolves the item (necessary condition: stale index) ----

    @Test
    fun negativeControl_validClickPosition_stillResolvesItem() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg("a"), msg("b"), msg("c")))
        var clicked: InboxMessage? = null
        adapter.onItemClick = { clicked = it }
        // After a refresh that KEEPS the index in range, the same captured position is valid.
        adapter.setCollection(listOf(msg("x"), msg("y"), msg("z")))
        val item = adapter.getItem(2)
        adapter.onItemClick?.invoke(item)
        assertNotNull(item)
        assertEquals("z", clicked?.code)
    }
}
