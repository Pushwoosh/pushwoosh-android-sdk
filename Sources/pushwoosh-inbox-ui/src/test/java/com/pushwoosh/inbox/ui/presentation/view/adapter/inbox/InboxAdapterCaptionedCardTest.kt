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
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.OnInboxButtonClickListener
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Captioned rich card wiring in [InboxAdapter]: `displayType=captioned` (or
 * the image+title heuristic) renders through [CaptionedInboxViewHolder] under
 * [InboxAdapter.CAPTIONED_VIEW_TYPE]; the icon beside the caption shows only
 * when it differs from the hero image.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxAdapterCaptionedCardTest {

    @After
    fun tearDown() {
        PushwooshInboxStyle.richCardsHeuristicEnabled = false
        PushwooshInboxUi.onButtonClickListener = null
    }

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
        bannerUrl: String? = null,
        imageUrl: String? = null,
        title: String? = null,
        actionPerformed: Boolean = false,
        read: Boolean = false
    ): InboxMessage = fakeInboxMessage(
        title = title,
        imageUrl = imageUrl,
        message = "body text",
        bannerUrl = bannerUrl,
        actionParams = actionParams,
        read = read,
        actionPerformed = actionPerformed
    )

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    @Test
    fun getItemViewType_displayTypeCaptioned_isCaptionedViewType() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(
            msg(actionParams = """{"displayType":"captioned"}""", bannerUrl = "https://img.example/hero.png")
        ))
        assertEquals(InboxAdapter.CAPTIONED_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun getItemViewType_heuristicImageWithTitle_isCaptionedViewType() {
        PushwooshInboxStyle.richCardsHeuristicEnabled = true
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(imageUrl = "https://img.example/icon.png", title = "title")))
        assertEquals(InboxAdapter.CAPTIONED_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun getItemViewType_heuristicOff_imageWithTitleStaysLegacyRow() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(imageUrl = "https://img.example/icon.png", title = "title")))
        assertEquals(InboxAdapter.TEXT_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun createViewHolder_captionedViewType_isCaptionedHolder() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        assertTrue(holder is CaptionedInboxViewHolder)
    }

    @Test
    fun fillView_bindsTitleAndBody() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "Card title"), 0)
        assertEquals("Card title", holder.itemView.findViewById<TextView>(R.id.inboxCaptionedTitle).text.toString())
        assertEquals("body text", holder.itemView.findViewById<TextView>(R.id.inboxCaptionedBody).text.toString())
    }

    @Test
    fun fillView_iconShownOnlyWhenDistinctFromHero() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val icon = holder.itemView.findViewById<View>(R.id.inboxCaptionedIcon)

        // Hero (b) and icon (imageUrl) differ -> icon visible.
        holder.fillView(msg(bannerUrl = "https://img.example/hero.png", imageUrl = "https://img.example/icon.png", title = "t"), 0)
        assertEquals(View.VISIBLE, icon.visibility)

        // Single picture: hero falls back to the icon -> icon hidden.
        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "t"), 0)
        assertEquals(View.GONE, icon.visibility)
    }

    @Test
    fun fillView_unreadDotGoneWhenRead() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val dot = holder.itemView.findViewById<View>(R.id.inboxCaptionedUnreadDot)

        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "t"), 0)
        assertEquals(View.VISIBLE, dot.visibility)

        // An inline button tap marks the message READ — the dot must go out then too.
        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "t", read = true), 0)
        assertEquals(View.GONE, dot.visibility)

        // A row tap sets OPEN, which reports both isRead and isActionPerformed.
        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "t", read = true, actionPerformed = true), 0)
        assertEquals(View.GONE, dot.visibility)
    }

    @Test
    fun fillView_togglesPinChipByPinnedFlag() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val chip = holder.itemView.findViewById<View>(R.id.inboxCaptionedPinChip)

        holder.fillView(msg(actionParams = """{"pinned":true}""", imageUrl = "https://img.example/i.png", title = "t"), 0)
        assertEquals(View.VISIBLE, chip.visibility)

        holder.fillView(msg(imageUrl = "https://img.example/i.png", title = "t"), 0)
        assertEquals(View.GONE, chip.visibility)
    }

    // ---- inline CTA buttons ----

    private fun buttonsRow(holder: View): android.widget.LinearLayout =
        holder.findViewById(R.id.inboxCaptionedButtonsRow)

    @Test
    fun fillView_rendersInlineButtons() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val m = msg(
            actionParams = """{"buttons":[{"title":"Open","url":"https://x.example/a"},{"title":"Later","action":"markRead"}]}""",
            imageUrl = "https://img.example/i.png",
            title = "t"
        )
        holder.fillView(m, 0)

        val row = buttonsRow(holder.itemView)
        assertEquals(View.VISIBLE, row.visibility)
        assertEquals(2, row.childCount)
        assertEquals("Open", (row.getChildAt(0) as TextView).text.toString())
        assertEquals("Later", (row.getChildAt(1) as TextView).text.toString())
        assertTrue(row.getChildAt(0).hasOnClickListeners())
    }

    @Test
    fun fillView_capsButtonsAtThree() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val five = (1..5).joinToString(",") { """{"title":"B$it","url":"https://x.example/$it"}""" }
        holder.fillView(msg(actionParams = """{"buttons":[$five]}""", imageUrl = "https://img.example/i.png", title = "t"), 0)
        assertEquals(3, buttonsRow(holder.itemView).childCount)
    }

    @Test
    fun buttonTap_hostListenerReturningFalse_consumesTheTap() {
        var seenTitle: String? = null
        PushwooshInboxUi.onButtonClickListener = OnInboxButtonClickListener { message, button ->
            assertNotNull(message)
            seenTitle = button.title
            false
        }
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        holder.fillView(msg(
            actionParams = """{"buttons":[{"title":"Read","action":"markRead"}]}""",
            imageUrl = "https://img.example/i.png",
            title = "t"
        ), 0)

        // The SDK default would hit PushwooshInbox (uninitialized in this test and
        // would throw); a consumed tap must not reach it.
        buttonsRow(holder.itemView).getChildAt(0).performClick()

        assertEquals("Read", seenTitle)
        assertFalse(seenTitle == null)
    }

    @Test
    fun fillView_noButtons_hidesRowAndClearsRecycledButtons() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAPTIONED_VIEW_TYPE)
        val withButtons = msg(
            actionParams = """{"buttons":[{"title":"Open","url":"https://x.example/a"}]}""",
            imageUrl = "https://img.example/i.png",
            title = "t"
        )
        holder.fillView(withButtons, 0)
        assertEquals(1, buttonsRow(holder.itemView).childCount)

        // Recycled with a button-less message: row hidden, stale buttons gone.
        holder.fillView(msg(imageUrl = "https://img.example/i.png", title = "t"), 0)
        assertEquals(View.GONE, buttonsRow(holder.itemView).visibility)
        assertEquals(0, buttonsRow(holder.itemView).childCount)
    }
}
