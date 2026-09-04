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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mirrors the iOS PushwooshInboxVideoContent decoder: the descriptor lives in
 * `actionParams["video"]` at the root, inside `u` as a dict, or inside `u` as
 * a JSON-encoded string. Only http/https urls are accepted — the url is fed to
 * a player, which would happily load a local `file://` from a payload string.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxVideoContentTest {

    private fun content(actionParams: String?) =
        InboxVideoContent.decode(fakeInboxMessage(actionParams = actionParams))

    @Test
    fun decode_rootVideo_returnsUrlAndPoster() {
        val result = content("""{"video":{"url":"https://cdn/clip.mp4","poster":"https://cdn/p.jpg"}}""")

        assertNotNull(result)
        assertEquals("https://cdn/clip.mp4", result!!.videoUrl)
        assertEquals("https://cdn/p.jpg", result.posterUrl)
    }

    @Test
    fun decode_videoInsideUDict_returnsContent() {
        val result = content("""{"u":{"video":{"url":"https://cdn/clip.mp4"}}}""")

        assertNotNull(result)
        assertEquals("https://cdn/clip.mp4", result!!.videoUrl)
        assertNull(result.posterUrl)
    }

    @Test
    fun decode_videoInsideUJsonString_returnsContent() {
        val result = content("""{"u":"{\"video\":{\"url\":\"http://cdn/clip.mp4\"}}"}""")

        assertNotNull(result)
        assertEquals("http://cdn/clip.mp4", result!!.videoUrl)
    }

    @Test
    fun decode_emptyPoster_becomesNull() {
        val result = content("""{"video":{"url":"https://cdn/clip.mp4","poster":""}}""")

        assertNotNull(result)
        assertNull(result!!.posterUrl)
    }

    @Test
    fun decode_nonNetworkScheme_returnsNull() {
        assertNull(content("""{"video":{"url":"file:///etc/passwd"}}"""))
        assertNull(content("""{"video":{"url":"content://media/1"}}"""))
        assertNull(content("""{"video":{"url":"myapp://clip"}}"""))
    }

    @Test
    fun decode_missingOrEmptyUrl_returnsNull() {
        assertNull(content("""{"video":{"poster":"https://cdn/p.jpg"}}"""))
        assertNull(content("""{"video":{"url":""}}"""))
    }

    @Test
    fun decode_noActionParams_returnsNull() {
        assertNull(content(null))
        assertNull(content("{}"))
        assertNull(content("not json"))
    }
}
