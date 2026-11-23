package com.pushwoosh.inline.internal.html

import com.pushwoosh.inline.internal.html.InlineLinkPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class InlineLinkPolicyTest {

    // Verifies that web and app links (also a scheme with '_', as native banners accept) open outside the banner, page-internal links are consumed and pushwoosh://close closes.
    @Test
    fun testLinkDecisions() {
        val cases = listOf(
            "https://x" to Decision.OPEN_EXTERNAL,
            "http://x/y?z=1" to Decision.OPEN_EXTERNAL,
            "myapp://p" to Decision.OPEN_EXTERNAL,
            "my_app://screen" to Decision.OPEN_EXTERNAL,
            "mailto:a@b.c" to Decision.OPEN_EXTERNAL,
            "javascript:a()" to Decision.CONSUME,
            "JavaScript:a()" to Decision.CONSUME,
            "about:blank" to Decision.CONSUME,
            "data:x" to Decision.CONSUME,
            "blob:x" to Decision.CONSUME,
            "/rel" to Decision.CONSUME,
            "rel/page.html" to Decision.CONSUME,
            "" to Decision.CONSUME,
            "file:///x" to Decision.CONSUME,
            "pushwoosh://close" to Decision.CLOSE,
            "pushwoosh://close/" to Decision.CLOSE,
            "pushwoosh://unknown" to Decision.CONSUME,
        )
        for ((url, expected) in cases) {
            assertEquals("case '$url'", expected, InlineLinkPolicy.decide(url))
        }
    }
}
