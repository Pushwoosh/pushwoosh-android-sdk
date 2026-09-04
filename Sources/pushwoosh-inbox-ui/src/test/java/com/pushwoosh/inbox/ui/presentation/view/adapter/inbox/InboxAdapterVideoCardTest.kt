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
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxVideoActivity
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Video rich card wiring in [InboxAdapter]: a `displayType=video` message with
 * a decodable descriptor renders through [VideoInboxViewHolder] under
 * [InboxAdapter.VIDEO_VIEW_TYPE], and tapping the poster starts
 * [InboxVideoActivity] with the video URL.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxAdapterVideoCardTest {

    private fun ctx(): Context = RuntimeEnvironment.getApplication()

    private fun fakeColorScheme(): ColorSchemeProvider = object : ColorSchemeProvider {
        override val cellBackground: Drawable? = null
        override val titleColor: ColorStateList = ColorStateList.valueOf(0)
        override val descriptionColor: ColorStateList = ColorStateList.valueOf(0)
        override val dateColor: ColorStateList = ColorStateList.valueOf(0)
        override val divider: Drawable? = null
        override val accentColor: Int = 0xFF2196F3.toInt()
        override val imageColor: ColorStateList = ColorStateList.valueOf(0)
        override val defaultIcon: Drawable? = null
        override val backgroundColor: Int = 0
    }

    private fun msg(
        actionParams: String? = null,
        title: String? = null,
        message: String = "body text",
        read: Boolean = false
    ): InboxMessage = fakeInboxMessage(
        title = title,
        message = message,
        actionParams = actionParams,
        read = read
    )

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    private fun holder(adapter: InboxAdapter) =
        adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.VIDEO_VIEW_TYPE)

    private val videoParams =
        """{"displayType":"video","video":{"url":"https://cdn/clip.mp4","poster":"https://cdn/p.jpg"}}"""

    @Test
    fun getItemViewType_videoWithDescriptor_isVideoViewType() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(actionParams = videoParams, title = "t")))
        assertEquals(InboxAdapter.VIDEO_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun createViewHolder_videoViewType_isVideoHolder() {
        assertTrue(holder(newAdapter()) is VideoInboxViewHolder)
    }

    @Test
    fun fillView_bindsTitleBodyAndShowsPlayBadge() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = videoParams, title = "Card title"), 0)

        assertEquals("Card title", holder.itemView.findViewById<TextView>(R.id.inboxVideoTitle).text.toString())
        assertEquals("body text", holder.itemView.findViewById<TextView>(R.id.inboxVideoBody).text.toString())
        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.inboxVideoPlayBadge).visibility)
    }

    @Test
    fun posterTap_startsPlayerWithTheVideoUrl() {
        // Fails if InboxVideoActivity is missing from the module manifest — otherwise the
        // component name below is just this test quoting the class back to itself.
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true)
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = videoParams, title = "t"), 0)

        // Marking the message read afterwards reaches PushwooshInbox, which has no SDK behind
        // it in a unit test and throws. The player intent is already out by then — that is what
        // this test is about.
        runCatching { holder.itemView.findViewById<View>(R.id.inboxVideoPosterHost).performClick() }

        val started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(started)
        assertEquals(InboxVideoActivity::class.java.name, started.component?.className)
        assertEquals("https://cdn/clip.mp4", started.getStringExtra(InboxVideoActivity.VIDEO_URL_EXTRA))
    }

    @Test
    fun posterTap_withoutDescriptor_startsNothing() {
        val holder = holder(newAdapter())
        // A card can only reach this holder with a descriptor, but a recycled view must not
        // fire a stale intent if it is ever bound to a payload-less message.
        holder.fillView(msg(actionParams = """{"displayType":"video"}""", title = "t"), 0)

        holder.itemView.findViewById<View>(R.id.inboxVideoPosterHost).performClick()

        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxVideoPlayBadge).visibility)
    }

    @Test
    fun fillView_noTitleAndNoBody_collapsesTextBlock() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = videoParams, title = null, message = ""), 0)

        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxVideoTextBlock).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxVideoUnreadDot).visibility)
    }

    @Test
    fun fillView_unreadDotGoneWhenRead() {
        val holder = holder(newAdapter())
        val dot = holder.itemView.findViewById<View>(R.id.inboxVideoUnreadDot)

        holder.fillView(msg(actionParams = videoParams, title = "t"), 0)
        assertEquals(View.VISIBLE, dot.visibility)

        holder.fillView(msg(actionParams = videoParams, title = "t", read = true), 0)
        assertEquals(View.GONE, dot.visibility)
    }

    @Test
    fun fillView_togglesPinChipByPinnedFlag() {
        val holder = holder(newAdapter())
        val chip = holder.itemView.findViewById<View>(R.id.inboxVideoPinChip)

        holder.fillView(
            msg(actionParams = """{"displayType":"video","pinned":true,"video":{"url":"https://cdn/clip.mp4"}}""", title = "t"),
            0
        )
        assertEquals(View.VISIBLE, chip.visibility)

        holder.fillView(msg(actionParams = videoParams, title = "t"), 0)
        assertEquals(View.GONE, chip.visibility)
    }

}
