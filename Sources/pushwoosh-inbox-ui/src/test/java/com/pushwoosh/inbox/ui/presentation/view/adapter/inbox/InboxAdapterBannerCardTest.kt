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
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Banner rich card wiring in [InboxAdapter]: a message with
 * `displayType=banner` and a hero image renders through
 * [BannerInboxViewHolder] under [InboxAdapter.BANNER_VIEW_TYPE]; everything
 * else keeps the legacy row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxAdapterBannerCardTest {

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
        read: Boolean = false,
        actionPerformed: Boolean = false
    ): InboxMessage = fakeInboxMessage(
        title = "title",
        actionParams = actionParams,
        bannerUrl = bannerUrl,
        read = read,
        actionPerformed = actionPerformed
    )

    private fun bannerMsg(read: Boolean = false, actionPerformed: Boolean = false, pinned: Boolean = false): InboxMessage {
        val pinnedPart = if (pinned) ""","pinned":true""" else ""
        return msg(
            actionParams = """{"displayType":"banner"$pinnedPart}""",
            bannerUrl = "https://img.example/hero.png",
            read = read,
            actionPerformed = actionPerformed
        )
    }

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    @Test
    fun getItemViewType_bannerMessage_isBannerViewType() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(bannerMsg(), msg()))
        assertEquals(InboxAdapter.BANNER_VIEW_TYPE, adapter.getItemViewType(0))
        assertEquals(InboxAdapter.TEXT_VIEW_TYPE, adapter.getItemViewType(1))
    }

    @Test
    fun getItemViewType_bannerWithoutImage_degradesToClassicCard() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(actionParams = """{"displayType":"banner"}""")))
        assertEquals(InboxAdapter.CLASSIC_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun createViewHolder_bannerViewType_isBannerHolder() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.BANNER_VIEW_TYPE)
        assertTrue(holder is BannerInboxViewHolder)
    }

    @Test
    fun fillView_togglesUnreadDotByReadState() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.BANNER_VIEW_TYPE)
        val dot = holder.itemView.findViewById<View>(R.id.inboxBannerUnreadDot)

        holder.fillView(bannerMsg(read = false), 0)
        assertEquals(View.VISIBLE, dot.visibility)

        holder.fillView(bannerMsg(read = true), 0)
        assertEquals(View.GONE, dot.visibility)

        // A row tap sets OPEN, which reports both isRead and isActionPerformed.
        holder.fillView(bannerMsg(read = true, actionPerformed = true), 0)
        assertEquals(View.GONE, dot.visibility)
    }

    @Test
    fun fillView_togglesPinChipByPinnedFlag() {
        val adapter = newAdapter()
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.BANNER_VIEW_TYPE)
        val chip = holder.itemView.findViewById<View>(R.id.inboxBannerPinChip)

        holder.fillView(bannerMsg(pinned = true), 0)
        assertEquals(View.VISIBLE, chip.visibility)

        holder.fillView(bannerMsg(pinned = false), 0)
        assertEquals(View.GONE, chip.visibility)
    }
}
