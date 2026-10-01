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

package com.pushwoosh.inbox.ui.presentation.view.adapter.inbox

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The legacy row ([InboxViewHolder], [InboxAdapter.TEXT_VIEW_TYPE]) paints its unread
 * look from [InboxMessage.isRead], the same predicate the rich cards use: a message
 * read through `PushwooshInbox.readMessages` (READ, not OPEN) looks read, and a tapped
 * one (OPEN) looks the same.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxViewHolderReadStateTest {

    private companion object {
        const val UNREAD_TINT = 0xFF2196F3.toInt()
        const val READ_TINT = 0xFF9E9E9E.toInt()
    }

    private fun ctx(): Context = RuntimeEnvironment.getApplication()

    /** Two-colour icon tint so the selected (unread) and default (read) colours differ. */
    private fun fakeColorScheme(): ColorSchemeProvider = object : ColorSchemeProvider {
        override val cellBackground: Drawable? = null
        override val titleColor: ColorStateList = ColorStateList.valueOf(0)
        override val descriptionColor: ColorStateList = ColorStateList.valueOf(0)
        override val dateColor: ColorStateList = ColorStateList.valueOf(0)
        override val divider: Drawable? = null
        override val accentColor: Int = UNREAD_TINT
        override val imageColor: ColorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(UNREAD_TINT, READ_TINT)
        )
        override val defaultIcon: Drawable? = null
        override val backgroundColor: Int = 0
    }

    private fun msg(read: Boolean, actionPerformed: Boolean): InboxMessage = fakeInboxMessage(
        title = "title",
        message = "body",
        read = read,
        actionPerformed = actionPerformed
    )

    private fun legacyRow(): BaseRecyclerAdapter.ViewHolder<InboxMessage> {
        val adapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.TEXT_VIEW_TYPE)
        assertTrue(holder is InboxViewHolder)
        return holder
    }

    private fun label(holder: BaseRecyclerAdapter.ViewHolder<InboxMessage>): TextView =
        holder.itemView.findViewById(R.id.inboxLabelTextView)

    private fun description(holder: BaseRecyclerAdapter.ViewHolder<InboxMessage>): TextView =
        holder.itemView.findViewById(R.id.inboxDescriptionTextView)

    private fun statusIcon(holder: BaseRecyclerAdapter.ViewHolder<InboxMessage>): ImageView =
        holder.itemView.findViewById(R.id.inboxStatusImageView)

    private fun tintOf(icon: ImageView): Int =
        Shadows.shadowOf(icon.colorFilter as PorterDuffColorFilter).color

    @Test
    fun getItemViewType_plainMessage_isTextViewType() {
        val adapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }
        adapter.setCollection(listOf(msg(read = false, actionPerformed = false)))
        assertEquals(InboxAdapter.TEXT_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun fillView_unread_selectsTitle() {
        val holder = legacyRow()
        holder.fillView(msg(read = false, actionPerformed = false), 0)
        assertTrue(label(holder).isSelected)
    }

    @Test
    fun fillView_readNotOpened_deselectsAllThreeViews() {
        // The state after PushwooshInbox.readMessages(codes): status READ, no OPEN.
        val holder = legacyRow()
        holder.fillView(msg(read = true, actionPerformed = false), 0)
        assertFalse(label(holder).isSelected)
        assertFalse(description(holder).isSelected)
        assertFalse(statusIcon(holder).isSelected)
    }

    @Test
    fun fillView_readAndOpened_deselectsTitle() {
        // A row tap sets OPEN, which reports both isRead and isActionPerformed.
        val holder = legacyRow()
        holder.fillView(msg(read = true, actionPerformed = true), 0)
        assertFalse(label(holder).isSelected)
        assertFalse(description(holder).isSelected)
        assertFalse(statusIcon(holder).isSelected)
    }

    @Test
    fun fillView_statusIconTintFollowsReadState() {
        val holder = legacyRow()

        holder.fillView(msg(read = false, actionPerformed = false), 0)
        assertEquals(UNREAD_TINT, tintOf(statusIcon(holder)))

        holder.fillView(msg(read = true, actionPerformed = false), 0)
        assertEquals(READ_TINT, tintOf(statusIcon(holder)))

        holder.fillView(msg(read = true, actionPerformed = true), 0)
        assertEquals(READ_TINT, tintOf(statusIcon(holder)))
    }

    @Test
    fun fillView_rebindFromUnreadToRead_clearsSelection() {
        // RecyclerView recycles holders; a stale selected state must not survive a rebind.
        val holder = legacyRow()
        holder.fillView(msg(read = false, actionPerformed = false), 0)
        assertTrue(label(holder).isSelected)

        holder.fillView(msg(read = true, actionPerformed = false), 0)
        assertFalse(label(holder).isSelected)
        assertFalse(description(holder).isSelected)
        assertFalse(statusIcon(holder).isSelected)
    }

    @Test
    fun fillView_unreadButActionPerformed_staysUnread() {
        // Not a state the SDK produces, but a custom InboxMessage may report it; isRead rules.
        val holder = legacyRow()
        holder.fillView(msg(read = false, actionPerformed = true), 0)
        assertTrue(label(holder).isSelected)
        assertEquals(UNREAD_TINT, tintOf(statusIcon(holder)))
    }
}
