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

import com.pushwoosh.inbox.data.InboxMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mirrors the iOS PushwooshInboxButton decode contract: buttons arrive in
 * `actionParams["buttons"]` (root, `u` dict, or `u` as a JSON-encoded string);
 * a button needs a non-empty title; the action resolves as explicit
 * `dismiss`/`markRead` token first, then a scheme-carrying `url`, else custom.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InboxCardButtonTest {

    private fun msg(actionParams: String? = null): InboxMessage =
        fakeInboxMessage(title = "title", actionParams = actionParams)

    // ---- decode(): wire shapes ----

    @Test
    fun decode_buttonsAtRoot() {
        val m = msg("""{"buttons":[{"title":"Open","url":"https://x.example/a"}]}""")
        val buttons = InboxCardButton.decode(m)
        assertEquals(1, buttons.size)
        assertEquals("Open", buttons[0].title)
    }

    @Test
    fun decode_buttonsInUDict() {
        val m = msg("""{"u":{"buttons":[{"title":"Open","url":"https://x.example/a"}]}}""")
        assertEquals(1, InboxCardButton.decode(m).size)
    }

    @Test
    fun decode_buttonsInUJsonString() {
        val m = msg("""{"u":"{\"buttons\":[{\"title\":\"Open\",\"url\":\"https://x.example/a\"}]}"}""")
        assertEquals(1, InboxCardButton.decode(m).size)
    }

    @Test
    fun decode_missingOrEmptyTitle_isSkipped() {
        val m = msg("""{"buttons":[{"url":"https://x.example/a"},{"title":"","url":"https://x.example/b"},{"title":"OK"}]}""")
        val buttons = InboxCardButton.decode(m)
        assertEquals(1, buttons.size)
        assertEquals("OK", buttons[0].title)
    }

    @Test
    fun decode_noButtonsAnywhere_isEmpty() {
        assertTrue(InboxCardButton.decode(msg()).isEmpty())
        assertTrue(InboxCardButton.decode(msg("""{"foo":"bar"}""")).isEmpty())
        assertTrue(InboxCardButton.decode(msg("broken {{{")).isEmpty())
    }

    // ---- action resolution ----

    @Test
    fun action_explicitTokensWinOverUrl() {
        val m = msg("""{"buttons":[
            {"title":"Close","action":"dismiss","url":"https://x.example/ignored"},
            {"title":"Read","action":"markRead"}
        ]}""")
        val buttons = InboxCardButton.decode(m)
        assertTrue(buttons[0].action is InboxCardButton.Action.Dismiss)
        assertTrue(buttons[1].action is InboxCardButton.Action.MarkRead)
    }

    @Test
    fun action_tokenIsCaseInsensitive() {
        val m = msg("""{"buttons":[{"title":"Read","action":"MARKREAD"},{"title":"Close","action":"Dismiss"}]}""")
        val buttons = InboxCardButton.decode(m)
        assertTrue(buttons[0].action is InboxCardButton.Action.MarkRead)
        assertTrue(buttons[1].action is InboxCardButton.Action.Dismiss)
    }

    @Test
    fun action_urlWithScheme_isOpenUrl() {
        val m = msg("""{"buttons":[{"title":"Open","url":"myapp://path"}]}""")
        val action = InboxCardButton.decode(m)[0].action
        assertTrue(action is InboxCardButton.Action.OpenUrl)
        assertEquals("myapp://path", (action as InboxCardButton.Action.OpenUrl).url)
    }

    @Test
    fun action_schemelessUrl_fallsToCustom() {
        val m = msg("""{"buttons":[{"title":"Open","url":"example.com/path"}]}""")
        assertTrue(InboxCardButton.decode(m)[0].action is InboxCardButton.Action.Custom)
    }

    @Test
    fun action_noUrlNoToken_isCustomWithPayload() {
        val m = msg("""{"buttons":[{"title":"Save","action":"custom","tag":"save-for-later"}]}""")
        val action = InboxCardButton.decode(m)[0].action
        assertTrue(action is InboxCardButton.Action.Custom)
        val payload = (action as InboxCardButton.Action.Custom).payload
        assertEquals("save-for-later", payload.optString("tag"))
        assertFalse(payload.has("title"))
        assertFalse(payload.has("action"))
    }

    // ---- URL scheme safety gate (mirrors iOS openExternalURL block list) ----

    @Test
    fun isSafeUrl_blocksLocalAndScriptSchemes() {
        for (bad in listOf("file:///etc/passwd", "javascript:alert(1)", "data:text/html,x", "intent://scan/#Intent;end")) {
            assertFalse(bad, InboxCardButton.isSafeUrl(bad))
        }
    }

    @Test
    fun isSafeUrl_allowsDeepLinksAndWeb() {
        for (ok in listOf("https://x.example/a", "http://x.example", "myapp://path", "mailto:a@b.c", "tel:+123")) {
            assertTrue(ok, InboxCardButton.isSafeUrl(ok))
        }
    }
}
