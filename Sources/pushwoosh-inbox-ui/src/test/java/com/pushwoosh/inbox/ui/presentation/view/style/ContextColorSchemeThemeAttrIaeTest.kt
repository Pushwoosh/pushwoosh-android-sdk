/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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
import android.content.ContextWrapper
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression guard for crash #27 crash-contextcolorscheme-theme-attr-iae.
 *
 * Before the fix, [ContextColorSchemeProvider]'s init block threw
 * `IllegalArgumentException("Unknown attribute please set up <name> into your theme")` when the host
 * theme defined neither the `inbox*` attrs nor their android/appcompat fallback colors: an absent
 * attr resolves to `0`, the `provideDefaultColor` fallback chain recursed to a fallback attr that was
 * also `0` and not covered by the `when`, and the `else` branch threw. That IAE escaped the unwrapped
 * factory call made by `InboxFragment.onAttach` / `AttachmentActivity.onCreate` and crashed the host.
 *
 * This is reachable only on the EMBEDDED path: a host app puts the inbox into an Activity whose theme
 * is non-AppCompat / custom. The standalone `InboxActivity` is immune — its manifest pins
 * `@style/PwInboxTheme`, which defines every inbox attr.
 *
 * The fix degrades an unresolved attr to a neutral fallback ([Color.GRAY]) instead of throwing, so
 * the provider now builds gracefully under any theme. These tests assert that graceful behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContextColorSchemeThemeAttrIaeTest {

    /** Bare non-AppCompat platform theme: no inbox attrs, no appcompat colorAccent/colorControlHighlight. */
    private fun bareThemeContext(): Context {
        val base: Context = RuntimeEnvironment.getApplication()
        val ctx = ContextWrapper(base)
        ctx.theme.applyStyle(android.R.style.Theme, true)
        return ctx
    }

    /** AppCompat theme that defines the inbox attrs (mirrors the standalone PwInboxTheme contract). */
    private fun pwInboxThemeContext(): Context {
        val base: Context = RuntimeEnvironment.getApplication()
        val ctx = ContextWrapper(base)
        ctx.theme.applyStyle(com.pushwoosh.inbox.ui.R.style.PwInboxTheme, true)
        return ctx
    }

    // Verifies that constructing the provider over a foreign theme that resolves no inbox attr (and no
    // fallback color) no longer throws — it degrades unresolved attrs to the neutral fallback instead.
    @Test
    fun foreignTheme_buildsGracefully_noThrow() {
        val provider = ColorSchemeProviderFactory.generateColorScheme(bareThemeContext())
        assertNotNull(provider)
    }

    // Verifies that an attr whose whole fallback chain is unresolved degrades to the documented neutral
    // fallback (Color.GRAY) rather than throwing. accentColor -> colorAccent is exactly such a chain on
    // the bare theme — it is the path that produced the pre-fix IAE.
    @Test
    fun foreignTheme_unresolvedAttrDegradesToNeutralFallback() {
        val provider = ColorSchemeProviderFactory.generateColorScheme(bareThemeContext())
        assertEquals("unresolved accent attr must degrade to the neutral fallback", Color.GRAY, provider.accentColor)
    }

    // Negative control: with a theme that DOES define the inbox attrs, accentColor resolves FROM the
    // theme, not from the fallback — proving the fix is conditional (it degrades only unresolved attrs)
    // and did not clobber the normal resolving path with an unconditional Color.GRAY.
    @Test
    fun negativeControl_pwInboxTheme_resolvesFromThemeNotFallback() {
        val provider = ColorSchemeProviderFactory.generateColorScheme(pwInboxThemeContext())
        assertNotNull(provider)
        assertNotEquals(
            "accent color must resolve from the theme, not degrade to the fallback",
            Color.GRAY,
            provider.accentColor
        )
    }
}
