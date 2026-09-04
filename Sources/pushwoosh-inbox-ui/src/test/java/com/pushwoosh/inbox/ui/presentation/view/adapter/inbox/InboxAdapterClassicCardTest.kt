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
import android.widget.LinearLayout
import android.widget.TextView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.After
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
 * Classic rich card wiring in [InboxAdapter]: `displayType=classic`, the
 * text-only heuristic and every degraded rich kind render through
 * [ClassicInboxViewHolder] under [InboxAdapter.CLASSIC_VIEW_TYPE].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxAdapterClassicCardTest {

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
        imageUrl: String? = null,
        title: String? = null,
        read: Boolean = false
    ): InboxMessage = fakeInboxMessage(
        title = title,
        imageUrl = imageUrl,
        message = "body text",
        actionParams = actionParams,
        read = read
    )

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    private fun holder(adapter: InboxAdapter) =
        adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CLASSIC_VIEW_TYPE)

    @Test
    fun getItemViewType_displayTypeClassic_isClassicViewType() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(actionParams = """{"displayType":"classic"}""", title = "t")))
        assertEquals(InboxAdapter.CLASSIC_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun getItemViewType_heuristicWithoutImage_isClassicViewType() {
        PushwooshInboxStyle.richCardsHeuristicEnabled = true
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(title = "title")))
        assertEquals(InboxAdapter.CLASSIC_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun getItemViewType_heuristicOff_plainMessageStaysLegacyRow() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(title = "title")))
        assertEquals(InboxAdapter.TEXT_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun createViewHolder_classicViewType_isClassicHolder() {
        assertTrue(holder(newAdapter()) is ClassicInboxViewHolder)
    }

    @Test
    fun fillView_bindsTitleAndBody() {
        val holder = holder(newAdapter())
        holder.fillView(msg(title = "Card title"), 0)
        assertEquals("Card title", holder.itemView.findViewById<TextView>(R.id.inboxClassicTitle).text.toString())
        assertEquals("body text", holder.itemView.findViewById<TextView>(R.id.inboxClassicBody).text.toString())
    }

    @Test
    fun fillView_withoutTitle_leadsWithTheBodyAndHidesIt() {
        val holder = holder(newAdapter())
        holder.fillView(msg(title = null), 0)
        assertEquals("body text", holder.itemView.findViewById<TextView>(R.id.inboxClassicTitle).text.toString())
        // The same sentence must not be printed twice.
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxClassicBody).visibility)
    }

    @Test
    fun fillView_withoutIconOrAppIcon_avatarShowsTheInitial() {
        // Robolectric's app ships no launcher icon, so this is the last fallback tier.
        val holder = holder(newAdapter())
        holder.fillView(msg(title = "t"), 0)

        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxClassicAvatarImage).visibility)
        val initial = holder.itemView.findViewById<TextView>(R.id.inboxClassicAvatarInitial)
        assertEquals(View.VISIBLE, initial.visibility)
        assertTrue("initial must be a single letter", initial.text.length == 1)
    }

    @Test
    fun fillView_withAppIcon_avatarShowsItInsteadOfTheInitial() {
        val appInfo = ctx().applicationInfo
        val previousIcon = appInfo.icon
        appInfo.icon = android.R.drawable.sym_def_app_icon
        try {
            val holder = holder(newAdapter())
            holder.fillView(msg(title = "t"), 0)

            assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.inboxClassicAvatarImage).visibility)
            assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxClassicAvatarInitial).visibility)
        } finally {
            appInfo.icon = previousIcon
        }
    }

    @Test
    fun fillView_withIconUrl_avatarShowsTheImage() {
        val holder = holder(newAdapter())
        holder.fillView(msg(imageUrl = "https://img.example/icon.png", title = "t"), 0)

        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.inboxClassicAvatarImage).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxClassicAvatarInitial).visibility)
    }

    @Test
    fun buttonTap_unsafeScheme_startsNothing() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"buttons":[{"title":"X","url":"file:///etc/passwd"}]}""", title = "t"),
            0
        )
        val row = holder.itemView.findViewById<LinearLayout>(R.id.inboxClassicButtonsRow)

        runCatching { row.getChildAt(0).performClick() }

        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun buttonTap_httpsUrl_opensIt() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"buttons":[{"title":"Open","url":"https://example.com"}]}""", title = "t"),
            0
        )
        val row = holder.itemView.findViewById<LinearLayout>(R.id.inboxClassicButtonsRow)

        runCatching { row.getChildAt(0).performClick() }

        val started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(started)
        assertEquals("https://example.com", started.dataString)
    }

    @Test
    fun fillView_unreadDotGoneWhenRead() {
        val holder = holder(newAdapter())
        val dot = holder.itemView.findViewById<View>(R.id.inboxClassicUnreadDot)

        holder.fillView(msg(title = "t"), 0)
        assertEquals(View.VISIBLE, dot.visibility)

        holder.fillView(msg(title = "t", read = true), 0)
        assertEquals(View.GONE, dot.visibility)
    }

    @Test
    fun fillView_togglesPinIconByPinnedFlag() {
        val holder = holder(newAdapter())
        val pin = holder.itemView.findViewById<View>(R.id.inboxClassicPinIcon)

        holder.fillView(msg(actionParams = """{"pinned":true}""", title = "t"), 0)
        assertEquals(View.VISIBLE, pin.visibility)

        holder.fillView(msg(title = "t"), 0)
        assertEquals(View.GONE, pin.visibility)
    }

    @Test
    fun fillView_rendersInlineButtons() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"buttons":[{"title":"Open","url":"https://example.com"},{"title":"Dismiss","action":"dismiss"}]}""",
                title = "t"),
            0
        )
        val row = holder.itemView.findViewById<LinearLayout>(R.id.inboxClassicButtonsRow)
        assertEquals(View.VISIBLE, row.visibility)
        assertEquals(2, row.childCount)
        assertEquals("Open", (row.getChildAt(0) as TextView).text.toString())
    }

    @Test
    fun fillView_withoutButtons_hidesTheRow() {
        val holder = holder(newAdapter())
        holder.fillView(msg(title = "t"), 0)
        assertEquals(View.GONE, holder.itemView.findViewById<LinearLayout>(R.id.inboxClassicButtonsRow).visibility)
    }

}
