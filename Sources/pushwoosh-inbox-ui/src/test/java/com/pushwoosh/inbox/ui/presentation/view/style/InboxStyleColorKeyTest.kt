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

package com.pushwoosh.inbox.ui.presentation.view.style

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Every [PushwooshInboxStyle] color key reaches the row on its own, and with no key set the
 * library theme supplies the documented default: unread action icon = accent, read action icon =
 * read date color, unread date = secondary text color.
 *
 * A key set in code outranks the theme attribute it would otherwise come from, and a specific key
 * outranks the general one it defaults to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxStyleColorKeyTest {

    private companion object {
        const val ORANGE = 0xFFFF6600.toInt()
        const val GREEN = 0xFF00AA00.toInt()
        val UNREAD = intArrayOf(android.R.attr.state_selected)
        val READ = intArrayOf()
    }

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.barBackgroundColor = null
        PushwooshInboxStyle.barAccentColor = null
        PushwooshInboxStyle.barTextColor = null
    }

    /** AppCompat theme that defines the inbox attrs — the standalone InboxActivity contract. */
    private fun pwInboxThemeContext(): Context =
        ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.PwInboxTheme)

    /** Bare platform theme: no inbox attrs, no appcompat colorAccent — the embedded worst case. */
    private fun bareThemeContext(): Context =
        ContextThemeWrapper(RuntimeEnvironment.getApplication(), android.R.style.Theme)

    private fun themeColor(context: Context, @AttrRes attr: Int): Int {
        val typed = context.theme.obtainStyledAttributes(intArrayOf(attr))
        try {
            return typed.getColor(0, 0)
        } finally {
            typed.recycle()
        }
    }

    private fun ColorStateList.of(state: IntArray): Int = getColorForState(state, 0)

    @Test
    fun accentColorOnly_paintsUnreadIcon_leavesReadIconAndUnreadDateSecondary() {
        val context = pwInboxThemeContext()
        val secondary = themeColor(context, android.R.attr.textColorSecondary)
        PushwooshInboxStyle.accentColor = ORANGE

        val provider = ColorSchemeProviderFactory.generateColorScheme(context)

        assertEquals("unread action icon takes the accent", ORANGE, provider.imageColor.of(UNREAD))
        assertEquals("read action icon stays secondary", secondary, provider.imageColor.of(READ))
        assertEquals("unread date stays secondary", secondary, provider.dateColor.of(UNREAD))
    }

    @Test
    fun noKeys_unreadIconIsThemeAccent_unreadDateIsSecondary() {
        val context = pwInboxThemeContext()
        val accent = themeColor(context, androidx.appcompat.R.attr.colorAccent)
        val secondary = themeColor(context, android.R.attr.textColorSecondary)

        val provider = ColorSchemeProviderFactory.generateColorScheme(context)

        assertEquals("provider accent is the theme accent", accent, provider.accentColor)
        assertEquals("unread action icon defaults to the accent", accent, provider.imageColor.of(UNREAD))
        assertEquals("unread date defaults to secondary", secondary, provider.dateColor.of(UNREAD))
    }

    @Test
    fun imageTypeColorBeatsAccentColor() {
        PushwooshInboxStyle.accentColor = ORANGE
        PushwooshInboxStyle.imageTypeColor = GREEN

        val provider = ColorSchemeProviderFactory.generateColorScheme(pwInboxThemeContext())

        assertEquals("the specific key wins", GREEN, provider.imageColor.of(UNREAD))
    }

    @Test
    fun readDateColorMovesTheReadActionIcon() {
        PushwooshInboxStyle.readDateColor = GREEN

        val provider = ColorSchemeProviderFactory.generateColorScheme(pwInboxThemeContext())

        assertEquals("read action icon follows the read date color", GREEN, provider.imageColor.of(READ))
    }

    @Test
    fun bareTheme_accentColorStillPaintsUnreadIcon() {
        PushwooshInboxStyle.accentColor = ORANGE

        val provider = ColorSchemeProviderFactory.generateColorScheme(bareThemeContext())

        assertEquals("a code key works under a theme that defines nothing", ORANGE, provider.imageColor.of(UNREAD))
    }

    @Test
    fun bareTheme_noKeys_unreadIconDegradesToNeutralFallback() {
        val provider = ColorSchemeProviderFactory.generateColorScheme(bareThemeContext())

        assertEquals("unresolvable chain degrades, it does not throw", Color.GRAY, provider.imageColor.of(UNREAD))
    }
}
