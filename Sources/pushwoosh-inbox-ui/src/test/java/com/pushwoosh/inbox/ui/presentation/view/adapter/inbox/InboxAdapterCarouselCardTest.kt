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
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Carousel rich card wiring in [InboxAdapter]: a `displayType=carousel`
 * message with decodable slides renders through [CarouselInboxViewHolder]
 * under [InboxAdapter.CAROUSEL_VIEW_TYPE], one gallery page per slide, page
 * dots only past a single slide, and the text block collapsing when the
 * message carries neither title nor body.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxAdapterCarouselCardTest {

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
        read: Boolean = false,
        code: String = "code"
    ): InboxMessage = fakeInboxMessage(
        code = code,
        title = title,
        message = message,
        actionParams = actionParams,
        read = read
    )

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    private fun holder(adapter: InboxAdapter) =
        adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAROUSEL_VIEW_TYPE)

    private val twoSlides =
        """{"displayType":"carousel","carousel":[
             {"image":"https://cdn/1.jpg","title":"New in","url":"https://example.com/1"},
             {"image":"https://cdn/2.jpg"}
           ]}"""

    @Test
    fun getItemViewType_carouselWithSlides_isCarouselViewType() {
        val adapter = newAdapter()
        adapter.setCollection(listOf(msg(actionParams = twoSlides, title = "t")))
        assertEquals(InboxAdapter.CAROUSEL_VIEW_TYPE, adapter.getItemViewType(0))
    }

    @Test
    fun createViewHolder_carouselViewType_isCarouselHolder() {
        assertTrue(holder(newAdapter()) is CarouselInboxViewHolder)
    }

    @Test
    fun fillView_bindsOneGalleryPagePerSlide() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        val gallery = holder.itemView.findViewById<RecyclerView>(R.id.inboxCarouselGallery)
        assertEquals(2, gallery.adapter?.itemCount)
    }

    @Test
    fun fillView_bindsTitleBodyAndDots() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "Card title"), 0)

        assertEquals("Card title", holder.itemView.findViewById<TextView>(R.id.inboxCarouselTitle).text.toString())
        assertEquals("body text", holder.itemView.findViewById<TextView>(R.id.inboxCarouselBody).text.toString())
        val dots = holder.itemView.findViewById<LinearLayout>(R.id.inboxCarouselDots)
        assertEquals(View.VISIBLE, dots.visibility)
        assertEquals(2, dots.childCount)
    }

    @Test
    fun fillView_singleSlide_hidesDots() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"displayType":"carousel","carousel":[{"image":"https://cdn/1.jpg"}]}""", title = "t"),
            0
        )
        assertEquals(View.GONE, holder.itemView.findViewById<LinearLayout>(R.id.inboxCarouselDots).visibility)
    }

    @Test
    fun fillView_noTitleAndNoBody_collapsesTextBlock() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = null, message = ""), 0)

        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxCarouselTextBlock).visibility)
        // No gutter to hold the unread dot once the text block is gone.
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxCarouselUnreadDot).visibility)
    }

    @Test
    fun fillView_bodyOnly_keepsTextBlockAndHidesTitleRow() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = null), 0)

        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.inboxCarouselTextBlock).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.inboxCarouselTitleRow).visibility)
    }

    @Test
    fun fillView_unreadDotGoneWhenRead() {
        val holder = holder(newAdapter())
        val dot = holder.itemView.findViewById<View>(R.id.inboxCarouselUnreadDot)

        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        assertEquals(View.VISIBLE, dot.visibility)

        holder.fillView(msg(actionParams = twoSlides, title = "t", read = true), 0)
        assertEquals(View.GONE, dot.visibility)
    }

    @Test
    fun fillView_togglesPinChipByPinnedFlag() {
        val holder = holder(newAdapter())
        val chip = holder.itemView.findViewById<View>(R.id.inboxCarouselPinChip)

        holder.fillView(
            msg(actionParams = """{"displayType":"carousel","pinned":true,"carousel":[{"image":"https://cdn/1.jpg"}]}""", title = "t"),
            0
        )
        assertEquals(View.VISIBLE, chip.visibility)

        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        assertEquals(View.GONE, chip.visibility)
    }

    /** Binds one gallery page so its click listener can be exercised. */
    private fun bindSlide(holder: BaseRecyclerAdapter.ViewHolder<InboxMessage>, index: Int): View {
        val gallery = holder.itemView.findViewById<RecyclerView>(R.id.inboxCarouselGallery)
        val slideAdapter = gallery.adapter!!
        val slideHolder = slideAdapter.createViewHolder(gallery, slideAdapter.getItemViewType(index))
        slideAdapter.bindViewHolder(slideHolder, index)
        return slideHolder.itemView
    }

    @Test
    fun slideTap_withOwnUrl_opensIt() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)

        // Marking read afterwards reaches PushwooshInbox, which has no SDK behind it here.
        runCatching { bindSlide(holder, 0).performClick() }

        val started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("https://example.com/1", started.dataString)
    }

    @Test
    fun slideTap_withoutUrl_fallsThroughToTheRowAction() {
        var rowClicks = 0
        val adapter = newAdapter()
        adapter.onItemClick = { rowClicks++ }
        adapter.setCollection(listOf(msg(actionParams = twoSlides, title = "t")))
        val holder = holder(adapter)
        adapter.onBindViewHolder(holder, 0)

        // Second slide carries no url of its own.
        bindSlide(holder, 1).performClick()

        assertEquals(1, rowClicks)
        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun slideTap_unsafeScheme_startsNothing() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"displayType":"carousel","carousel":[{"image":"https://cdn/1.jpg","url":"file:///etc/passwd"}]}""",
                title = "t"),
            0
        )

        runCatching { bindSlide(holder, 0).performClick() }

        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun fillView_slideWithNonNetworkImage_isDropped() {
        val holder = holder(newAdapter())
        holder.fillView(
            msg(actionParams = """{"displayType":"carousel","carousel":[{"image":"file:///data/data/app/secret.png"},{"image":"https://cdn/2.jpg"}]}""",
                title = "t"),
            0
        )
        val gallery = holder.itemView.findViewById<RecyclerView>(R.id.inboxCarouselGallery)
        assertEquals(1, gallery.adapter?.itemCount)
    }

    @Test
    fun fillView_rebindWithAnotherMessage_dropsStaleDots() {
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        holder.fillView(
            msg(
                actionParams = """{"displayType":"carousel","carousel":[{"image":"https://cdn/1.jpg"}]}""",
                title = "t",
                code = "another"
            ),
            0
        )
        val dots = holder.itemView.findViewById<LinearLayout>(R.id.inboxCarouselDots)
        assertEquals(0, dots.childCount)
    }

    @Test
    fun fillView_rebindWithSameMessage_keepsTheDotsItAlreadyBuilt() {
        // Any list update rebinds every visible row (setCollection -> notifyDataSetChanged).
        // Rebuilding the dots then would also rewind a half-swiped gallery to slide one.
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        val dots = holder.itemView.findViewById<LinearLayout>(R.id.inboxCarouselDots)
        val firstDot = dots.getChildAt(0)

        holder.fillView(msg(actionParams = twoSlides, title = "t", read = true), 0)

        assertEquals(2, dots.childCount)
        assertSame("dots were rebuilt, so the gallery was rewound too", firstDot, dots.getChildAt(0))
    }

    @Test
    fun fillView_rebindWithSameMessageButEditedSlides_rebuildsDots() {
        // Same code, edited payload: the server may rewrite action_params of an existing row.
        val holder = holder(newAdapter())
        holder.fillView(msg(actionParams = twoSlides, title = "t"), 0)
        holder.fillView(
            msg(
                actionParams = """{"displayType":"carousel","carousel":[
                     {"image":"https://cdn/1.jpg"},
                     {"image":"https://cdn/2.jpg"},
                     {"image":"https://cdn/3.jpg"}
                   ]}""",
                title = "t"
            ),
            0
        )

        assertEquals(3, holder.itemView.findViewById<LinearLayout>(R.id.inboxCarouselDots).childCount)
    }

}
