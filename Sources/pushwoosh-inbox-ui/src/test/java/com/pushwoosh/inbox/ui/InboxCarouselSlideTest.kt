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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mirrors the iOS PushwooshInboxCarouselSlide decoder: slides live in
 * `actionParams["carousel"]` at the root, inside `u` as a dict, or inside `u`
 * as a JSON-encoded string. A slide without an image is dropped; an empty
 * title becomes null; a url without a scheme becomes null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxCarouselSlideTest {

    private fun slides(actionParams: String?) =
        InboxCarouselSlide.decode(fakeInboxMessage(actionParams = actionParams))

    @Test
    fun decode_rootCarousel_returnsSlidesInOrder() {
        val result = slides(
            """{"carousel":[
                 {"image":"https://cdn/1.jpg","title":"New in","url":"myapp://p/1"},
                 {"image":"https://cdn/2.jpg","title":"On sale","url":"myapp://p/2"}
               ]}"""
        )

        assertEquals(2, result.size)
        assertEquals("https://cdn/1.jpg", result[0].imageUrl)
        assertEquals("New in", result[0].title)
        assertEquals("myapp://p/1", result[0].url)
        assertEquals("https://cdn/2.jpg", result[1].imageUrl)
    }

    @Test
    fun decode_carouselInsideUDict_returnsSlides() {
        val result = slides("""{"u":{"carousel":[{"image":"https://cdn/1.jpg"}]}}""")

        assertEquals(1, result.size)
        assertEquals("https://cdn/1.jpg", result[0].imageUrl)
    }

    @Test
    fun decode_carouselInsideUJsonString_returnsSlides() {
        val result = slides("""{"u":"{\"carousel\":[{\"image\":\"https://cdn/1.jpg\"}]}"}""")

        assertEquals(1, result.size)
        assertEquals("https://cdn/1.jpg", result[0].imageUrl)
    }

    @Test
    fun decode_slideWithoutImage_isDropped() {
        val result = slides("""{"carousel":[{"title":"no image"},{"image":"https://cdn/2.jpg"}]}""")

        assertEquals(1, result.size)
        assertEquals("https://cdn/2.jpg", result[0].imageUrl)
    }

    @Test
    fun decode_slideWithEmptyImage_isDropped() {
        val result = slides("""{"carousel":[{"image":""}]}""")

        assertTrue(result.isEmpty())
    }

    @Test
    fun decode_emptyTitleAndSchemelessUrl_becomeNull() {
        val result = slides("""{"carousel":[{"image":"https://cdn/1.jpg","title":"","url":"cdn/relative"}]}""")

        assertEquals(1, result.size)
        assertNull(result[0].title)
        assertNull(result[0].url)
    }

    @Test
    fun decode_slidesPastMaxAreDropped() {
        val items = (1..12).joinToString(",") { """{"image":"https://cdn/$it.jpg"}""" }
        val result = slides("""{"carousel":[$items]}""")

        assertEquals(InboxCarouselSlide.MAX_SLIDES, result.size)
        assertEquals("https://cdn/1.jpg", result[0].imageUrl)
        assertEquals("https://cdn/10.jpg", result[9].imageUrl)
    }

    @Test
    fun decode_droppedSlideDoesNotCountTowardsMax() {
        val valid = (1..11).joinToString(",") { """{"image":"https://cdn/$it.jpg"}""" }
        val result = slides("""{"carousel":[{"title":"no image"},$valid]}""")

        assertEquals(InboxCarouselSlide.MAX_SLIDES, result.size)
        assertEquals("https://cdn/10.jpg", result[9].imageUrl)
    }

    @Test
    fun decode_noActionParams_returnsEmpty() {
        assertTrue(slides(null).isEmpty())
        assertTrue(slides("{}").isEmpty())
        assertTrue(slides("not json").isEmpty())
    }

    @Test
    fun decode_carouselNotAnArray_returnsEmpty() {
        assertTrue(slides("""{"carousel":"nope"}""").isEmpty())
    }
}
