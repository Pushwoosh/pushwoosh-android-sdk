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
import androidx.recyclerview.widget.RecyclerView
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.PushwooshInboxUi
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.fakeInboxMessage
import com.pushwoosh.inbox.ui.model.repository.InboxRepository
import com.pushwoosh.inbox.ui.InboxCardButton
import com.pushwoosh.inbox.ui.OnInboxButtonClickListener
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxVideoActivity
import com.pushwoosh.inbox.ui.presentation.view.adapter.BaseRecyclerAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * SDK-997: any tap on a card reports the message as opened (status 3), so the Control Panel open
 * counter follows every interaction — inline buttons of every kind, carousel slides, the video
 * poster, the banner attachment and the swipe — not just a plain row tap.
 *
 * The status call is routed through [PushwooshInbox.markMessageOpened], which reports the open
 * without performing the message's own `l`/`rm` action — the element already navigates, and
 * [PushwooshInbox.performAction] here would open a second destination on top of it. It is
 * reported before the host is asked and before any guard, mirroring iOS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxCardOpenStatusTest {

    private companion object {
        const val CODE = "code-997"
        const val HTTPS_BUTTON = """{"buttons":[{"title":"Open","url":"https://example.com"}]}"""
        const val UNSAFE_BUTTON = """{"buttons":[{"title":"X","url":"file:///etc/passwd"}]}"""
        const val CUSTOM_BUTTON = """{"buttons":[{"title":"Rate","action":"custom","payload":{"k":"v"}}]}"""
        const val DISMISS_BUTTON = """{"buttons":[{"title":"Dismiss","action":"dismiss"}]}"""
        const val MARK_READ_BUTTON = """{"buttons":[{"title":"Mark read","action":"markread"}]}"""
        const val VIDEO_PARAMS =
            """{"displayType":"video","video":{"url":"https://cdn/clip.mp4","poster":"https://cdn/p.jpg"}}"""
        const val TWO_SLIDES =
            """{"displayType":"carousel","carousel":[
                 {"image":"https://cdn/1.jpg","title":"New in","url":"https://example.com/1"},
                 {"image":"https://cdn/2.jpg"}
               ]}"""
    }

    @After
    fun tearDown() {
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

    private fun msg(actionParams: String?): InboxMessage = fakeInboxMessage(
        code = CODE,
        title = "t",
        message = "body text",
        actionParams = actionParams
    )

    private fun newAdapter(): InboxAdapter = InboxAdapter(ctx(), fakeColorScheme()) { _, _ -> }

    private fun holder(viewType: Int) =
        newAdapter().onCreateViewHolder(FrameLayout(ctx()), viewType)

    /** Binds a classic card and returns its first inline button. */
    private fun firstButton(actionParams: String): View {
        val holder = holder(InboxAdapter.CLASSIC_VIEW_TYPE)
        holder.fillView(msg(actionParams), 0)
        return holder.itemView.findViewById<LinearLayout>(R.id.inboxClassicButtonsRow).getChildAt(0)
    }

    /** Binds one carousel gallery page so its click listener can be exercised. */
    private fun slide(holder: BaseRecyclerAdapter.ViewHolder<InboxMessage>, index: Int): View {
        val gallery = holder.itemView.findViewById<RecyclerView>(R.id.inboxCarouselGallery)
        val slideAdapter = gallery.adapter!!
        val slideHolder = slideAdapter.createViewHolder(gallery, slideAdapter.getItemViewType(index))
        slideAdapter.bindViewHolder(slideHolder, index)
        return slideHolder.itemView
    }

    private fun startedActivity() =
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity

    private inline fun withInboxStatic(body: (MockedStatic<PushwooshInbox>) -> Unit) =
        mockStatic(PushwooshInbox::class.java).use(body)

    // --- path 1: inline .OpenUrl button ---

    @Test
    fun inlineOpenUrlButtonTap_marksMessageOpened() {
        val button = firstButton(HTTPS_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
            inbox.verify({ PushwooshInbox.performAction(anyString()) }, never())
        }

        assertEquals("https://example.com", startedActivity()?.dataString)
    }

    @Test
    fun unsafeUrlButtonTap_marksOpenedAndOpensNothing() {
        val button = firstButton(UNSAFE_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        assertNull("the unsafe scheme is still refused", startedActivity())
    }

    @Test
    fun hostConsumingTheTap_stillMarksOpened() {
        PushwooshInboxUi.onButtonClickListener =
            OnInboxButtonClickListener { _, _ -> false }
        val button = firstButton(HTTPS_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        assertNull("the host navigates instead of us", startedActivity())
    }

    // --- path 3: .Custom button ---

    @Test
    fun customButtonTap_marksMessageOpened() {
        var seen: InboxCardButton? = null
        PushwooshInboxUi.onButtonClickListener =
            OnInboxButtonClickListener { _, button -> seen = button; true }
        val button = firstButton(CUSTOM_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        assertNotNull("the host keeps receiving the custom payload", seen)
    }

    // --- the remaining button kinds: the open ships, their own semantics stay ---

    @Test
    fun dismissButtonTap_marksOpenedAndDeletes() {
        val button = firstButton(DISMISS_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.deleteMessage(CODE) }, times(1))
        }
    }

    @Test
    fun markReadButtonTap_marksOpenedInsteadOfRead() {
        val button = firstButton(MARK_READ_BUTTON)

        withInboxStatic { inbox ->
            button.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            // The open already makes the message read; a READ report would be a lower status.
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }
    }

    // --- path 2: carousel slide ---

    @Test
    fun slideTapWithOwnUrl_marksMessageOpened() {
        val holder = holder(InboxAdapter.CAROUSEL_VIEW_TYPE)
        holder.fillView(msg(TWO_SLIDES), 0)
        val firstSlide = slide(holder, 0)

        withInboxStatic { inbox ->
            firstSlide.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        assertEquals("https://example.com/1", startedActivity()?.dataString)
    }

    @Test
    fun slideTapWithoutUrl_doesNotMarkOpenedItself() {
        val adapter = newAdapter()
        var rowClicks = 0
        adapter.onItemClick = { rowClicks++ }
        adapter.setCollection(listOf(msg(TWO_SLIDES)))
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.CAROUSEL_VIEW_TYPE)
        adapter.onBindViewHolder(holder, 0)
        val secondSlide = slide(holder, 1)

        withInboxStatic { inbox ->
            secondSlide.performClick()

            // The row action owns this tap: it runs performAction through the presenter instead.
            inbox.verify({ PushwooshInbox.markMessageOpened(anyString()) }, never())
        }

        assertEquals(1, rowClicks)
    }

    // --- path 4: video poster ---

    @Test
    fun posterTap_marksMessageOpened() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true)
        val holder = holder(InboxAdapter.VIDEO_VIEW_TYPE)
        holder.fillView(msg(VIDEO_PARAMS), 0)
        val poster = holder.itemView.findViewById<View>(R.id.inboxVideoPosterHost)

        withInboxStatic { inbox ->
            poster.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        val started = startedActivity()
        assertNotNull(started)
        assertEquals(InboxVideoActivity::class.java.name, started.component?.className)
    }

    @Test
    fun posterTapWithoutVideoUrl_stillMarksOpened() {
        val holder = holder(InboxAdapter.VIDEO_VIEW_TYPE)
        holder.fillView(msg(null), 0)
        val poster = holder.itemView.findViewById<View>(R.id.inboxVideoPosterHost)

        withInboxStatic { inbox ->
            poster.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.readMessage(anyString()) }, never())
        }

        assertNull("no player to open without a video URL", startedActivity())
    }

    // --- path 5: the banner attachment of a plain card ---

    @Test
    fun bannerAttachmentTap_marksMessageOpened() {
        var openedAttachment: String? = null
        val adapter = InboxAdapter(ctx(), fakeColorScheme()) { url, _ -> openedAttachment = url }
        val holder = adapter.onCreateViewHolder(FrameLayout(ctx()), InboxAdapter.TEXT_VIEW_TYPE)
        holder.fillView(
            fakeInboxMessage(code = CODE, title = "t", message = "body", bannerUrl = "https://cdn/b.jpg"),
            0
        )
        val banner = holder.itemView.findViewById<View>(R.id.inboxBannerImage)

        withInboxStatic { inbox ->
            banner.performClick()

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
        }

        assertEquals("https://cdn/b.jpg", openedAttachment)
    }

    // --- path 6: the swipe ---

    @Test
    fun swipeDelete_marksOpenedAndDeletes() {
        withInboxStatic { inbox ->
            InboxRepository.removeItem(msg(null))

            inbox.verify({ PushwooshInbox.markMessageOpened(CODE) }, times(1))
            inbox.verify({ PushwooshInbox.deleteMessage(CODE) }, times(1))
        }
    }
}
