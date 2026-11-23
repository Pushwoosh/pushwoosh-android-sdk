package com.pushwoosh.inline.internal.html

import com.pushwoosh.inline.internal.parser.InlineParser
import java.util.Locale

/** What an HTML banner does with a link the page navigates to; the banner never navigates itself. */
internal object InlineLinkPolicy {

    enum class Decision {
        /** Handed to the system; the banner stays. */
        OPEN_EXTERNAL,

        /** Ignored: page-internal, local or unknown SDK links. */
        CONSUME,

        /** `pushwoosh://close`: the page closes the banner. */
        CLOSE,
    }

    private val consumedSchemes = setOf("javascript", "about", "data", "blob", "file")

    fun decide(url: String): Decision {
        val scheme = InlineParser.schemeOf(url.trim())?.lowercase(Locale.ROOT) ?: return Decision.CONSUME
        return when {
            scheme == PUSHWOOSH_SCHEME -> if (isClose(url.trim())) Decision.CLOSE else Decision.CONSUME
            scheme in consumedSchemes -> Decision.CONSUME
            else -> Decision.OPEN_EXTERNAL
        }
    }

    private fun isClose(url: String): Boolean {
        val host = url.substringAfter("://", "").takeWhile { it != '/' && it != '?' && it != '#' }
        return host.equals(CLOSE_HOST, ignoreCase = true)
    }

    private const val PUSHWOOSH_SCHEME = "pushwoosh"
    private const val CLOSE_HOST = "close"
}
