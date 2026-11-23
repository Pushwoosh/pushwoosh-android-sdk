package com.pushwoosh.inline.internal.model

import androidx.annotation.ColorInt
import com.pushwoosh.inline.PushwooshInlineBanner
import org.json.JSONObject

internal enum class InlineFormat { NATIVE, HTML, NONE }

/**
 * The banner stored for one slot. [raw] is the banner object as the server sent it, kept to
 * persist it as is. [format] is [InlineFormat.NONE] for a control-group banner.
 */
internal class InlineBanner(
    val slotId: String,
    val bannerId: String,
    val version: Int,
    val format: InlineFormat,
    val html: String?,
    val root: InlineNode?,
    val isControl: Boolean,
    /** Unix epoch seconds from which the banner is not shown; `null` = never expires. */
    val expireDate: Long?,
    /** The slot never grows taller than this (dp); the rest is clipped. */
    val maxHeight: Float?,
    /** Runs on a tap anywhere on the banner outside elements with their own action. */
    val action: InlineAction?,
    /** Close button drawn over the banner; `null` = none. */
    val close: InlineClose?,
    val isTest: Boolean,
    val payload: Map<String, String>,
    val raw: JSONObject,
) {
    fun isExpired(nowSec: Long): Boolean = expireDate != null && nowSec >= expireDate

    /** Same banner content as [o] for show deduplication. */
    fun sameShown(o: InlineBanner?): Boolean =
        o != null && bannerId == o.bannerId && version == o.version && isTest == o.isTest

    fun toPublic(): PushwooshInlineBanner = PushwooshInlineBanner(
        slotId = slotId,
        bannerId = bannerId,
        version = version,
        format = raw.opt("format") as? String ?: "",
        html = html,
        payload = payload,
        expireDate = expireDate,
        isTest = isTest,
        json = raw.toString(),
    )
}

/** A color with an optional dark-theme variant; [dark] `null` = the same in both themes. */
internal data class InlineColor(@ColorInt val light: Int, @ColorInt val dark: Int?)

/** Insets in dp, CSS order of the source payload. */
internal data class InlineInsets(val top: Float, val right: Float, val bottom: Float, val left: Float) {
    companion object {
        val ZERO = InlineInsets(0f, 0f, 0f, 0f)
    }
}

/** [angle] is in degrees clockwise from left-to-right: 90 runs top to bottom. */
internal data class InlineGradient(val colors: List<Int>, val angle: Float = 90f)

internal data class InlineShadow(
    @ColorInt val color: Int = DEFAULT_COLOR,
    val radius: Float = 8f,
    val x: Float = 0f,
    val y: Float = 4f,
) {
    companion object {
        const val DEFAULT_COLOR = 0x33000000
    }
}

internal data class InlineBorder(val width: Float = 1f, @ColorInt val color: Int)

/** Light-theme style of a node; lengths in dp. */
internal data class InlineStyle(
    @ColorInt val background: Int? = null,
    val cornerRadius: Float = 0f,
    val padding: InlineInsets = InlineInsets.ZERO,
    val gradient: InlineGradient? = null,
    val backgroundImage: String? = null,
    val shadow: InlineShadow? = null,
    val border: InlineBorder? = null,
    val glass: Boolean = false,
)

/**
 * The node's `dark` keys; a non-null field replaces the light value in dark theme. [color] and
 * [indicatorColor] are also folded into the node's [InlineColor] values.
 */
internal data class InlineStyleOverride(
    @ColorInt val background: Int? = null,
    val gradient: InlineGradient? = null,
    val backgroundImage: String? = null,
    val shadow: InlineShadow? = null,
    val border: InlineBorder? = null,
    val glass: Boolean? = null,
    @ColorInt val color: Int? = null,
    @ColorInt val indicatorColor: Int? = null,
)

/** Where a child sits inside an overlay stack. */
internal enum class InlinePosition { FILL, TOP, BOTTOM, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

internal enum class InlineWeight { REGULAR, MEDIUM, SEMIBOLD, BOLD, HEAVY, BLACK }

internal enum class InlineAlign { LEFT, CENTER, RIGHT }

internal enum class InlineAxis { ROW, COLUMN, OVERLAY }

internal enum class InlineStackAlign { START, CENTER, END, FILL }

internal enum class InlineDistribution { FILL, EQUAL }

internal sealed class InlineNode {
    /** The node's `id`, or its type when it has none; reported with its clicks. */
    abstract val id: String
    abstract val style: InlineStyle
    abstract val dark: InlineStyleOverride?
    abstract val action: InlineAction?
    abstract val position: InlinePosition

    /** [align] `null` = natural; [lines] 0 = unlimited; [size] in sp. */
    data class Text(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val text: String,
        val color: InlineColor?,
        val size: Float = 15f,
        val weight: InlineWeight = InlineWeight.REGULAR,
        val align: InlineAlign?,
        val lines: Int = 0,
        val strike: Boolean,
    ) : InlineNode()

    /** [aspect] is width / height; `null` = take the ratio of the loaded image. */
    data class Image(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val url: String,
        val aspect: Float?,
    ) : InlineNode()

    data class Button(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val title: String,
        val color: InlineColor?,
        val size: Float = 15f,
        val weight: InlineWeight = InlineWeight.SEMIBOLD,
    ) : InlineNode()

    /** Muted, looping, plays while at least half of its height is on screen. */
    data class Video(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val url: String,
        val poster: String?,
        val aspect: Float = 16f / 9f,
    ) : InlineNode()

    data class Stack(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val axis: InlineAxis,
        val spacing: Float,
        val align: InlineStackAlign,
        val distribution: InlineDistribution,
        val children: List<InlineNode>,
    ) : InlineNode()

    /** [intervalSec] `null` = only the user turns pages. */
    data class Pager(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val pages: List<InlineNode>,
        val intervalSec: Float?,
        val indicatorColor: InlineColor?,
    ) : InlineNode()

    /** [size] along the parent stack axis; `null` takes the free space. */
    data class Spacer(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val size: Float?,
    ) : InlineNode()

    data class Divider(
        override val id: String,
        override val style: InlineStyle,
        override val dark: InlineStyleOverride?,
        override val action: InlineAction?,
        override val position: InlinePosition,
        val thickness: Float = 1f,
        val color: InlineColor?,
    ) : InlineNode()
}

internal sealed class InlineAction {
    /** A web page (http/https only); the banner stays in its slot. */
    data class Url(val uri: String) : InlineAction()

    /** An app link of any scheme, handed to the system. */
    data class Deeplink(val uri: String) : InlineAction()

    /** Dismisses the banner; allowed on nodes only. */
    object Close : InlineAction()

    data class PostEvent(val event: String, val attributes: JSONObject?) : InlineAction()

    data class Copy(val text: String, val toast: String?) : InlineAction()
}

internal enum class InlineClosePosition { TOP_RIGHT, TOP_LEFT }

internal data class InlineClose(
    val enabled: Boolean,
    val position: InlineClosePosition = InlineClosePosition.TOP_RIGHT,
    @ColorInt val color: Int = DEFAULT_COLOR,
    @ColorInt val background: Int = DEFAULT_BACKGROUND,
    val cornerRadius: Float = 14f,
) {
    companion object {
        const val DEFAULT_COLOR = 0xFFFFFFFF.toInt()
        const val DEFAULT_BACKGROUND = 0x80000000.toInt()
    }
}
