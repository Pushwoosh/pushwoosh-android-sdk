package com.pushwoosh.inline.internal.view

import android.content.Context
import android.view.View
import androidx.annotation.VisibleForTesting
import com.pushwoosh.inline.internal.html.InlineHtmlFactory
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.model.InlineFormat

/** The drawn banner a slot view hosts. */
internal interface InlineContent {
    /** Measured by the slot at its width with an unspecified height. */
    val view: View

    /** `false` while the banner cannot count as shown yet (an HTML document before its first height report). */
    val isReady: Boolean

    /**
     * The slot is about to stop showing the content: what the user did on it and is still held back is
     * reported now, while the slot routes its calls. [dispose] follows, possibly after an animation.
     */
    fun retire() = Unit

    /** Called once the slot no longer shows the content. */
    fun dispose()
}

/** What the content tells its slot. Calls from content the slot no longer shows are ignored. */
internal interface InlineContentHost {
    /** The content needs another height at the same width (an image loaded, an HTML height report). */
    fun onContentSizeChanged()

    /** A tap: [elementId] `null` = the banner itself; [action] = what the tapped element does. */
    fun onClick(elementId: String?, action: InlineAction?)

    /** The user closed the banner. */
    fun onClose()

    /** The content became [InlineContent.isReady]. */
    fun onContentReady()
}

/** Draws a banner with views of [context]; `null` = it cannot be drawn and the slot stays collapsed. */
internal interface InlineContentFactory {
    fun create(context: Context, banner: InlineBanner, dark: Boolean, host: InlineContentHost): InlineContent?
}

/** The factory slot views use: one factory per banner format; a format without one draws nothing. */
internal object InlineContentFactories : InlineContentFactory {
    private val byFormat: Map<InlineFormat, InlineContentFactory> = mapOf(
        InlineFormat.NATIVE to NativeBannerRenderer(),
        InlineFormat.HTML to InlineHtmlFactory(),
    )

    /** Draws every format instead of the real factories; `null` = the real ones. */
    @VisibleForTesting
    var override: InlineContentFactory? = null

    override fun create(context: Context, banner: InlineBanner, dark: Boolean, host: InlineContentHost): InlineContent? =
        (override ?: byFormat[banner.format])?.create(context, banner, dark, host)
}
