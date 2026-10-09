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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The toolbar keys of [PushwooshInboxStyle] each paint one thing and nothing else:
 * barBackgroundColor the bar, barTextColor the title, barAccentColor the navigation icon.
 * backgroundColor paints the screen and the list and leaves the bar alone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxToolbarStyleTest {

    private companion object {
        const val ORANGE = 0xFFFF6600.toInt()
        const val GREEN = 0xFF00AA00.toInt()
        const val CREAM = 0xFFFFEECC.toInt()
    }

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.barBackgroundColor = null
        PushwooshInboxStyle.barAccentColor = null
        PushwooshInboxStyle.barTextColor = null
        PushwooshInboxStyle.showToolbar = null
    }

    /** Builds the stock inbox screen with the backend stubbed out, and hands it to the body. */
    private fun withInbox(body: (InboxActivity) -> Unit) =
        mockStatic(PushwooshInbox::class.java).use {
            body(Robolectric.buildActivity(InboxActivity::class.java).setup().get())
        }

    private fun toolbar(activity: InboxActivity): Toolbar = activity.findViewById(R.id.inboxToolbar)

    private fun titleView(bar: Toolbar): TextView =
        (0 until bar.childCount)
            .map { bar.getChildAt(it) }
            .filterIsInstance<TextView>()
            .first { it.text == bar.title }

    private fun bgColor(view: View): Int = (view.background as ColorDrawable).color

    /** The flat color a view ended up with, or null when it was never painted a flat color. */
    private fun bgColorOrNull(view: View): Int? = (view.background as? ColorDrawable)?.color

    private fun inboxRoot(activity: InboxActivity): View =
        activity.supportFragmentManager.findFragmentByTag(InboxActivity.FRAGMENT_TAG)!!.requireView()

    private fun navigationIconTint(bar: Toolbar): Int? {
        val filter = bar.navigationIcon?.colorFilter as? PorterDuffColorFilter ?: return null
        return Shadows.shadowOf(filter).color
    }

    private fun themeColor(context: Context, @AttrRes attr: Int): Int {
        val typed = context.theme.obtainStyledAttributes(intArrayOf(attr))
        try {
            return typed.getColor(0, 0)
        } finally {
            typed.recycle()
        }
    }

    @Test
    fun barBackgroundColorOnly_paintsBarNotScreen() {
        PushwooshInboxStyle.barBackgroundColor = ORANGE

        withInbox { activity ->
            assertEquals("toolbar takes the bar color", ORANGE, bgColor(toolbar(activity)))
            assertEquals(
                "app bar takes the bar color",
                ORANGE,
                bgColor(activity.findViewById(R.id.inboxAppBarLayout))
            )
            assertNull(
                "the list is not painted by a bar key",
                activity.findViewById<View>(R.id.inboxRecyclerView).background
            )
        }
    }

    @Test
    fun barTextColorOnly_paintsTitleNotArrow() {
        PushwooshInboxStyle.barTextColor = GREEN

        withInbox { activity ->
            val bar = toolbar(activity)
            assertEquals("title takes the bar text color", GREEN, titleView(bar).currentTextColor)
            assertNull("the navigation icon is not tinted by the title color", navigationIconTint(bar))
            assertEquals(
                "the bar keeps the theme primary color",
                themeColor(activity, android.R.attr.colorPrimary),
                bgColor(bar)
            )
        }
    }

    @Test
    fun barAccentColorOnly_tintsArrowNotTitle() {
        PushwooshInboxStyle.barAccentColor = GREEN

        withInbox { activity ->
            val bar = toolbar(activity)
            assertEquals("the navigation icon takes the bar accent", GREEN, navigationIconTint(bar))
            assertEquals("the title stays white", Color.WHITE, titleView(bar).currentTextColor)
        }
    }

    @Test
    fun backgroundColorOnly_paintsScreenAndStatesNotBar() {
        PushwooshInboxStyle.backgroundColor = CREAM

        withInbox { activity ->
            assertEquals(
                "the list takes the background color",
                CREAM,
                bgColor(activity.findViewById(R.id.inboxRecyclerView))
            )
            assertEquals(
                "the empty state takes the background color",
                CREAM,
                bgColor(activity.findViewById(R.id.inboxEmpty))
            )
            assertEquals(
                "the error state takes the background color",
                CREAM,
                bgColor(activity.findViewById(R.id.inboxError))
            )
            assertEquals(
                "the bar keeps the theme primary color",
                themeColor(activity, android.R.attr.colorPrimary),
                bgColor(toolbar(activity))
            )
        }
    }

    @Test
    fun transparentBarBackgroundColorIsAColorNotAnAbsentKey() {
        PushwooshInboxStyle.barBackgroundColor = Color.TRANSPARENT

        withInbox { activity ->
            assertEquals("a transparent key is honored", Color.TRANSPARENT, bgColor(toolbar(activity)))
        }
    }

    @Test
    fun barBackgroundColorWithBackgroundColor_eachPaintsItsOwnSurface() {
        PushwooshInboxStyle.barBackgroundColor = ORANGE
        PushwooshInboxStyle.backgroundColor = CREAM

        withInbox { activity ->
            assertEquals(
                "the bar takes the bar color, not the screen color",
                ORANGE,
                bgColor(toolbar(activity))
            )
            assertEquals(
                "the app bar takes the bar color",
                ORANGE,
                bgColor(activity.findViewById(R.id.inboxAppBarLayout))
            )
            assertEquals("the screen takes the screen color", CREAM, bgColor(inboxRoot(activity)))
            assertEquals(
                "the list takes the screen color",
                CREAM,
                bgColor(activity.findViewById(R.id.inboxRecyclerView))
            )
        }
    }

    @Test
    fun embeddedWithoutToolbar_barKeysPaintNothing() {
        PushwooshInboxStyle.showToolbar = false
        PushwooshInboxStyle.barBackgroundColor = ORANGE
        PushwooshInboxStyle.barAccentColor = GREEN
        PushwooshInboxStyle.barTextColor = GREEN

        withInbox { activity ->
            val bar = toolbar(activity)
            assertEquals(
                "the app bar is hidden when the host owns the toolbar",
                View.GONE,
                activity.findViewById<View>(R.id.inboxAppBarLayout).visibility
            )
            assertNull("there is no navigation icon to tint, so no bar key may NPE", bar.navigationIcon)
            assertNotEquals(
                "a hidden app bar must not be painted by the bar color",
                ORANGE,
                bgColorOrNull(activity.findViewById(R.id.inboxAppBarLayout))
            )
            assertNotEquals(
                "a hidden toolbar must not be painted by the bar color",
                ORANGE,
                bgColorOrNull(bar)
            )
            assertNotEquals(
                "a hidden title must not be painted by the bar text color",
                GREEN,
                titleView(bar).currentTextColor
            )
            assertNull("no bar key leaks onto the list", activity.findViewById<View>(R.id.inboxRecyclerView).background)
        }
    }

    /**
     * SwipeRefreshLayout exposes no getter for its scheme colors; read them off the ring of the
     * drawable it spins.
     */
    private fun spinnerColors(layout: SwipeRefreshLayout): IntArray {
        val progressField = SwipeRefreshLayout::class.java.getDeclaredField("mProgress")
        progressField.isAccessible = true
        val progress = progressField.get(layout) as CircularProgressDrawable
        val ringField = CircularProgressDrawable::class.java.getDeclaredField("mRing")
        ringField.isAccessible = true
        val ring = ringField.get(progress)
        val colorsField = ring.javaClass.getDeclaredField("mColors")
        colorsField.isAccessible = true
        return colorsField.get(ring) as IntArray
    }

    @Test
    fun accentColorPaintsTheLoadingIndicators() {
        PushwooshInboxStyle.accentColor = ORANGE

        withInbox { activity ->
            val refresh = activity.findViewById<SwipeRefreshLayout>(R.id.inboxSwipeRefreshLayout)
            assertEquals(
                "the pull-to-refresh spinner takes the accent",
                listOf(ORANGE),
                spinnerColors(refresh).toList()
            )

            val progress = activity.findViewById<ProgressBar>(R.id.inboxTotalProgressBar)
            assertEquals(
                "the first-load indicator takes the accent",
                ORANGE,
                (progress.indeterminateTintList as ColorStateList).defaultColor
            )
        }
    }

    @Test
    fun clearColors_makesTheNextScreenDefaultIncludingTheBar() {
        PushwooshInboxStyle.barBackgroundColor = ORANGE
        PushwooshInboxStyle.barTextColor = GREEN
        PushwooshInboxStyle.barAccentColor = GREEN
        PushwooshInboxStyle.backgroundColor = CREAM

        PushwooshInboxStyle.clearColors()

        withInbox { activity ->
            val bar = toolbar(activity)
            assertEquals(
                "the bar is back to the theme primary color",
                themeColor(activity, android.R.attr.colorPrimary),
                bgColor(bar)
            )
            assertEquals("the title is back to white", Color.WHITE, titleView(bar).currentTextColor)
            assertNull("the navigation icon is untinted again", navigationIconTint(bar))
            assertNull(
                "the list is unpainted again",
                activity.findViewById<View>(R.id.inboxRecyclerView).background
            )
        }
    }
}
