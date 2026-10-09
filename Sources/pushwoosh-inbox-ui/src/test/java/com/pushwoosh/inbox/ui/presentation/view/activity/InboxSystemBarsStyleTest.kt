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

import androidx.core.view.WindowCompat
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Below API 35 the inbox colors the system bars by what sits on them: the toolbar covers the
 * status bar, so barBackgroundColor owns it; the screen reaches the navigation bar, so
 * backgroundColor owns it. With the toolbar hidden the screen reaches the status bar too and
 * takes it over. A bar nobody claims keeps whatever the theme gave it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.LEGACY)
class InboxSystemBarsStyleTest {

    private companion object {
        const val ORANGE = 0xFFFF6600.toInt()
        const val CREAM = 0xFFFFEECC.toInt()
        const val NAVY = 0xFF102030.toInt()
    }

    private data class Bars(val status: Int, val navigation: Int, val lightStatusIcons: Boolean)

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.barBackgroundColor = null
        PushwooshInboxStyle.barAccentColor = null
        PushwooshInboxStyle.barTextColor = null
    }

    /** Opens the inbox with the given keys and reads the window's bar colors back. */
    @Suppress("DEPRECATION")
    private fun barsWith(configure: () -> Unit): Bars {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.barBackgroundColor = null
        configure()
        return mockStatic(PushwooshInbox::class.java).use {
            val window = Robolectric.buildActivity(InboxActivity::class.java).setup().get().window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            Bars(
                window.statusBarColor,
                window.navigationBarColor,
                insetsController.isAppearanceLightStatusBars
            )
        }
    }

    @Test
    fun barBackgroundColorOwnsTheStatusBarOnly() {
        val untouched = barsWith { }

        val styled = barsWith { PushwooshInboxStyle.barBackgroundColor = ORANGE }

        assertEquals("status bar follows the bar color", ORANGE, styled.status)
        assertEquals("navigation bar is left alone", untouched.navigation, styled.navigation)
    }

    @Test
    fun backgroundColorOwnsTheNavigationBarOnly() {
        val untouched = barsWith { }

        val styled = barsWith { PushwooshInboxStyle.backgroundColor = CREAM }

        assertEquals("navigation bar follows the screen color", CREAM, styled.navigation)
        assertEquals("status bar is left alone", untouched.status, styled.status)
    }

    @Test
    fun hiddenToolbarGivesTheStatusBarToTheScreenColor() {
        val styled = barsWith {
            PushwooshInboxStyle.showToolbar = false
            PushwooshInboxStyle.backgroundColor = CREAM
            PushwooshInboxStyle.barBackgroundColor = NAVY
        }

        assertEquals("status bar follows the screen color", CREAM, styled.status)
        assertTrue("icon contrast is read off the color that is actually visible", styled.lightStatusIcons)
    }

    @Test
    fun hiddenToolbarKeepsItsColorOffTheStatusBar() {
        val untouched = barsWith { }

        val styled = barsWith {
            PushwooshInboxStyle.showToolbar = false
            PushwooshInboxStyle.barBackgroundColor = NAVY
        }

        assertEquals("a hidden toolbar colors nothing", untouched.status, styled.status)
        assertEquals("icon contrast stays as the theme left it", untouched.lightStatusIcons, styled.lightStatusIcons)
    }
}
