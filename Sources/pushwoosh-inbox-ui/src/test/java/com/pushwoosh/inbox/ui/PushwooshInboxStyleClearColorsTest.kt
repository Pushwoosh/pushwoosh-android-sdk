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

package com.pushwoosh.inbox.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * clearColors() is what a cross-platform plugin calls between two presentInboxUI calls: after it,
 * no color key of a previous call may survive. The toolbar keys were the ones that did.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushwooshInboxStyleClearColorsTest {

    private companion object {
        const val ORANGE = 0xFFFF6600.toInt()
    }

    @After
    fun tearDown() {
        PushwooshInboxStyle.clearColors()
        PushwooshInboxStyle.barBackgroundColor = null
        PushwooshInboxStyle.barAccentColor = null
        PushwooshInboxStyle.barTextColor = null
        PushwooshInboxStyle.barTitle = null
    }

    @Test
    fun clearColors_clearsTheToolbarKeysToo() {
        PushwooshInboxStyle.accentColor = ORANGE
        PushwooshInboxStyle.backgroundColor = ORANGE
        PushwooshInboxStyle.barBackgroundColor = ORANGE
        PushwooshInboxStyle.barAccentColor = ORANGE
        PushwooshInboxStyle.barTextColor = ORANGE

        PushwooshInboxStyle.clearColors()

        assertNull(PushwooshInboxStyle.accentColor)
        assertNull(PushwooshInboxStyle.backgroundColor)
        assertNull(PushwooshInboxStyle.barBackgroundColor)
        assertNull(PushwooshInboxStyle.barAccentColor)
        assertNull(PushwooshInboxStyle.barTextColor)
    }

    @Test
    fun clearColors_leavesTheBarTitleAlone() {
        PushwooshInboxStyle.barTitle = "Messages"

        PushwooshInboxStyle.clearColors()

        assertEquals("the bar title is not a color", "Messages", PushwooshInboxStyle.barTitle)
    }
}
