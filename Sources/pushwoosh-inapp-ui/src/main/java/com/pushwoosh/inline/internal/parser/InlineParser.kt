package com.pushwoosh.inline.internal.parser

import com.pushwoosh.inapp.ui.parser.InAppColorParser
import com.pushwoosh.inline.internal.model.InlineAction
import com.pushwoosh.inline.internal.model.InlineAlign
import com.pushwoosh.inline.internal.model.InlineAxis
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.model.InlineBorder
import com.pushwoosh.inline.internal.model.InlineClose
import com.pushwoosh.inline.internal.model.InlineClosePosition
import com.pushwoosh.inline.internal.model.InlineColor
import com.pushwoosh.inline.internal.model.InlineDistribution
import com.pushwoosh.inline.internal.model.InlineFormat
import com.pushwoosh.inline.internal.model.InlineGradient
import com.pushwoosh.inline.internal.model.InlineInsets
import com.pushwoosh.inline.internal.model.InlineNode
import com.pushwoosh.inline.internal.model.InlinePosition
import com.pushwoosh.inline.internal.model.InlineShadow
import com.pushwoosh.inline.internal.model.InlineStackAlign
import com.pushwoosh.inline.internal.model.InlineStyle
import com.pushwoosh.inline.internal.model.InlineStyleOverride
import com.pushwoosh.inline.internal.model.InlineWeight
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONArray
import org.json.JSONObject

/**
 * Decodes one banner of a slots response. Fail-closed: a banner with any unknown node, format or
 * enum value or a wrongly typed known key is dropped and the failing path is logged; unknown keys
 * are ignored and JSON `null` counts as absent.
 */
internal object InlineParser {

    private const val TAG = "InlineParser"

    private const val MAX_DEPTH = 16
    private const val MAX_LENGTH = 4096.0
    private const val MAX_FONT_SIZE = 256.0
    private const val MIN_ASPECT = 0.1
    private const val MAX_ASPECT = 20.0
    private const val MIN_PAGER_INTERVAL = 1.0
    private const val DEFAULT_FONT_SIZE = 15.0

    /** The RFC 3986 scheme rule relaxed to what Android intents accept (`_`). */
    private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.\\-_]*):")

    private val LENGTHS = 0.0..MAX_LENGTH
    private val OFFSETS = -MAX_LENGTH..MAX_LENGTH

    private val WEIGHTS = mapOf(
        "regular" to InlineWeight.REGULAR,
        "medium" to InlineWeight.MEDIUM,
        "semibold" to InlineWeight.SEMIBOLD,
        "bold" to InlineWeight.BOLD,
        "heavy" to InlineWeight.HEAVY,
        "black" to InlineWeight.BLACK,
    )
    private val TEXT_ALIGNS = mapOf("left" to InlineAlign.LEFT, "center" to InlineAlign.CENTER, "right" to InlineAlign.RIGHT)
    private val AXES = mapOf("row" to InlineAxis.ROW, "column" to InlineAxis.COLUMN, "overlay" to InlineAxis.OVERLAY)
    private val STACK_ALIGNS = mapOf(
        "start" to InlineStackAlign.START,
        "center" to InlineStackAlign.CENTER,
        "end" to InlineStackAlign.END,
        "fill" to InlineStackAlign.FILL,
    )
    private val DISTRIBUTIONS = mapOf("fill" to InlineDistribution.FILL, "equal" to InlineDistribution.EQUAL)
    private val POSITIONS = mapOf(
        "fill" to InlinePosition.FILL,
        "top" to InlinePosition.TOP,
        "bottom" to InlinePosition.BOTTOM,
        "center" to InlinePosition.CENTER,
        "topLeft" to InlinePosition.TOP_LEFT,
        "topRight" to InlinePosition.TOP_RIGHT,
        "bottomLeft" to InlinePosition.BOTTOM_LEFT,
        "bottomRight" to InlinePosition.BOTTOM_RIGHT,
    )
    private val CLOSE_POSITIONS = mapOf("topRight" to InlineClosePosition.TOP_RIGHT, "topLeft" to InlineClosePosition.TOP_LEFT)

    private class Fail(val path: String) : Exception(path)

    /** The scheme [url] starts with, as written; `null` = none. One rule for native and HTML banner links. */
    fun schemeOf(url: String): String? = SCHEME.find(url)?.groupValues?.get(1)

    /** The banner the server put into [slotId]; `null` when the slot is empty or the banner breaks the contract. */
    fun parse(raw: Any?, slotId: String): InlineBanner? {
        if (raw == null || raw == JSONObject.NULL) return null
        return try {
            parseBanner(raw, slotId)
        } catch (fail: Fail) {
            PWLog.warn(TAG, "Invalid inline banner for slot '$slotId', dropped at: ${fail.path}")
            null
        }
    }

    private fun parseBanner(raw: Any, slotId: String): InlineBanner {
        val dict = raw as? JSONObject ?: throw Fail("banner")
        val bannerId = dict.optionalString("bannerId", "bannerId")?.takeIf { it.isNotEmpty() } ?: throw Fail("bannerId")
        val version = dict.optionalInt("version", "version")?.takeIf { it >= 0 } ?: throw Fail("version")
        val maxHeight = dict.optionalLength("maxHeight", "maxHeight")
        if (maxHeight != null && maxHeight <= 0) throw Fail("maxHeight")
        val expireDate = dict.optionalLong("expireDate", "expireDate")
        val isControl = dict.optionalBool("control", "control") ?: false
        var format = InlineFormat.NONE
        var html: String? = null
        var root: InlineNode? = null
        if (!isControl) {
            when (dict.optionalString("format", "format")) {
                "native" -> {
                    format = InlineFormat.NATIVE
                    val native = dict.present("native") ?: throw Fail("native")
                    root = node(native.asObject("native").opt("root"), "native.root", 0)
                }
                "html" -> {
                    format = InlineFormat.HTML
                    html = dict.optionalString("html", "html")?.takeIf { it.isNotEmpty() } ?: throw Fail("html")
                }
                else -> throw Fail("format")
            }
        }
        return InlineBanner(
            slotId = slotId,
            bannerId = bannerId,
            version = version,
            format = format,
            html = html,
            root = root,
            isControl = isControl,
            expireDate = expireDate,
            maxHeight = maxHeight?.toFloat(),
            action = dict.present("action")?.let { bannerAction(it) },
            close = dict.present("close")?.let { close(it) },
            isTest = dict.optionalBool("test", "test") ?: false,
            payload = payload(dict, bannerId),
            raw = dict,
        )
    }

    /** Keeps the string values; a value of another type drops only its own key. */
    private fun payload(dict: JSONObject, bannerId: String): Map<String, String> {
        val raw = dict.present("payload") ?: return emptyMap()
        if (raw !is JSONObject) {
            PWLog.warn(TAG, "Inline banner '$bannerId' payload is not an object, ignored")
            return emptyMap()
        }
        val payload = LinkedHashMap<String, String>()
        val dropped = ArrayList<String>()
        for (key in raw.keys()) {
            val value = raw.opt(key)
            if (value is String) payload[key] = value else dropped.add(key)
        }
        if (dropped.isNotEmpty()) {
            PWLog.warn(TAG, "Inline banner '$bannerId' payload keys ${dropped.sorted()} are not strings, dropped")
        }
        return payload
    }

    private fun close(raw: Any): InlineClose? {
        val dict = raw.asObject("close")
        if (dict.optionalBool("enabled", "close.enabled") != true) return null
        return InlineClose(
            enabled = true,
            position = dict.optionalEnum("position", "close.position", CLOSE_POSITIONS) ?: InlineClosePosition.TOP_RIGHT,
            color = dict.optionalColor("color", "close.color") ?: InlineClose.DEFAULT_COLOR,
            background = dict.optionalColor("background", "close.background") ?: InlineClose.DEFAULT_BACKGROUND,
            cornerRadius = dict.optionalLength("cornerRadius", "close.cornerRadius")?.toFloat() ?: 14f,
        )
    }

    // Nodes

    private fun node(raw: Any?, label: String, depth: Int): InlineNode {
        if (depth >= MAX_DEPTH) throw Fail("$label (too deep)")
        val dict = raw as? JSONObject ?: throw Fail("$label.type")
        val type = dict.opt("type") as? String ?: throw Fail("$label.type")
        val dark = dict.present("dark")?.let { it as? JSONObject ?: throw Fail("$label.dark") }
        val id = dict.optionalString("id", "$label.id") ?: type
        val style = style(dict, label)
        val action = dict.present("action")?.let { action(it, "$label.action") }
        val position = dict.optionalEnum("position", "$label.position", POSITIONS) ?: InlinePosition.FILL
        return when (type) {
            "text" -> {
                val override = darkOverride(dark, label, "color")
                val lines = dict.optionalInt("lines", "$label.lines") ?: 0
                if (lines < 0) throw Fail("$label.lines")
                InlineNode.Text(
                    id = id, style = style, dark = override, action = action, position = position,
                    text = dict.opt("text") as? String ?: throw Fail("$label.text"),
                    color = themedColor(dict, override?.color, "color", label),
                    size = fontSize(dict, label),
                    weight = dict.optionalEnum("weight", "$label.weight", WEIGHTS) ?: InlineWeight.REGULAR,
                    align = dict.optionalEnum("align", "$label.align", TEXT_ALIGNS),
                    lines = lines,
                    strike = dict.optionalBool("strike", "$label.strike") ?: false,
                )
            }
            "image" -> InlineNode.Image(
                id = id, style = style, dark = darkOverride(dark, label), action = action, position = position,
                url = url(dict, "url", label),
                aspect = aspect(dict, label),
            )
            "button" -> {
                val override = darkOverride(dark, label, "color")
                InlineNode.Button(
                    id = id, style = style, dark = override, action = action, position = position,
                    title = (dict.opt("title") as? String)?.takeIf { it.isNotEmpty() } ?: throw Fail("$label.title"),
                    color = themedColor(dict, override?.color, "color", label),
                    size = fontSize(dict, label),
                    weight = dict.optionalEnum("weight", "$label.weight", WEIGHTS) ?: InlineWeight.SEMIBOLD,
                )
            }
            "video" -> InlineNode.Video(
                id = id, style = style, dark = darkOverride(dark, label), action = action, position = position,
                url = url(dict, "url", label),
                poster = dict.present("poster")?.let { url(dict, "poster", label) },
                aspect = aspect(dict, label) ?: (16f / 9f),
            )
            "stack" -> InlineNode.Stack(
                id = id, style = style, dark = darkOverride(dark, label), action = action, position = position,
                axis = dict.optionalEnum("axis", "$label.axis", AXES) ?: InlineAxis.COLUMN,
                spacing = dict.optionalLength("spacing", "$label.spacing")?.toFloat() ?: 0f,
                align = dict.optionalEnum("align", "$label.align", STACK_ALIGNS) ?: InlineStackAlign.FILL,
                distribution = dict.optionalEnum("distribution", "$label.distribution", DISTRIBUTIONS)
                    ?: InlineDistribution.FILL,
                children = nodes(dict, "children", label, depth),
            )
            "pager" -> {
                val override = darkOverride(dark, label, "indicatorColor")
                val interval = dict.optionalNumber("interval", "$label.interval")
                if (interval != null && interval < MIN_PAGER_INTERVAL) throw Fail("$label.interval")
                InlineNode.Pager(
                    id = id, style = style, dark = override, action = action, position = position,
                    pages = nodes(dict, "pages", label, depth),
                    intervalSec = interval?.toFloat(),
                    indicatorColor = themedColor(dict, override?.indicatorColor, "indicatorColor", label),
                )
            }
            "spacer" -> InlineNode.Spacer(
                id = id, style = style, dark = darkOverride(dark, label), action = action, position = position,
                size = dict.optionalLength("size", "$label.size")?.toFloat(),
            )
            "divider" -> {
                val override = darkOverride(dark, label, "color")
                val thickness = dict.optionalLength("thickness", "$label.thickness") ?: 1.0
                if (thickness <= 0) throw Fail("$label.thickness")
                InlineNode.Divider(
                    id = id, style = style, dark = override, action = action, position = position,
                    thickness = thickness.toFloat(),
                    color = themedColor(dict, override?.color, "color", label),
                )
            }
            else -> throw Fail("$label.type")
        }
    }

    private fun nodes(dict: JSONObject, key: String, label: String, depth: Int): List<InlineNode> {
        val list = dict.opt(key) as? JSONArray
        if (list == null || list.length() == 0) throw Fail("$label.$key")
        return List(list.length()) { node(list.opt(it), "$label.$key[$it]", depth + 1) }
    }

    /**
     * The node's `dark` keys. The style keys apply to every node type; [colorKey] is the one
     * color the node type themes (`color` or `indicatorColor`), other color keys are ignored.
     */
    private fun darkOverride(dark: JSONObject?, label: String, colorKey: String? = null): InlineStyleOverride? {
        if (dark == null) return null
        val path = "$label.dark"
        return InlineStyleOverride(
            background = dark.optionalColor("background", "$path.background"),
            gradient = dark.present("gradient")?.let { gradient(it, "$path.gradient") },
            backgroundImage = dark.present("backgroundImage")?.let { url(dark, "backgroundImage", path) },
            shadow = dark.present("shadow")?.let { shadow(it, "$path.shadow") },
            border = dark.present("border")?.let { border(it, "$path.border") },
            glass = dark.present("glass")?.let { glass(it, "$path.glass") },
            color = if (colorKey == "color") dark.optionalColor("color", "$path.color") else null,
            indicatorColor = if (colorKey == "indicatorColor") {
                dark.optionalColor("indicatorColor", "$path.indicatorColor")
            } else {
                null
            },
        )
    }

    /** The light color with its dark variant; a dark-only color stays in the override for the renderer's default. */
    private fun themedColor(dict: JSONObject, dark: Int?, key: String, label: String): InlineColor? =
        dict.optionalColor(key, "$label.$key")?.let { InlineColor(it, dark) }

    private fun fontSize(dict: JSONObject, label: String): Float {
        val size = dict.optionalLength("size", "$label.size", 0.0..MAX_FONT_SIZE) ?: DEFAULT_FONT_SIZE
        if (size <= 0) throw Fail("$label.size")
        return size.toFloat()
    }

    private fun aspect(dict: JSONObject, label: String): Float? {
        val aspect = dict.optionalNumber("aspect", "$label.aspect") ?: return null
        if (aspect !in MIN_ASPECT..MAX_ASPECT) throw Fail("$label.aspect")
        return aspect.toFloat()
    }

    // Actions

    /** The action of a tap on the banner outside elements with their own; `close` belongs to nodes only. */
    private fun bannerAction(raw: Any): InlineAction? {
        val action = action(raw, "action")
        if (action == InlineAction.Close) {
            PWLog.warn(TAG, "action: close is allowed on nodes only, ignored")
            return null
        }
        return action
    }

    /** An unknown type is ignored and the banner stays. */
    private fun action(raw: Any, label: String): InlineAction? {
        val dict = raw as? JSONObject ?: throw Fail(label)
        return when (val type = dict.opt("type") as? String ?: throw Fail("$label.type")) {
            "url" -> {
                val url = link(dict, "value", label)
                val scheme = linkSchemeOf(url)?.lowercase() ?: throw Fail("$label.value")
                if (scheme != "http" && scheme != "https") throw Fail("$label.value")
                InlineAction.Url(url)
            }
            "deeplink" -> InlineAction.Deeplink(link(dict, "value", label))
            "close" -> InlineAction.Close
            "postEvent" -> {
                val event = (dict.opt("event") as? String)?.takeIf { it.isNotEmpty() } ?: throw Fail("$label.event")
                val attributes = dict.present("attributes")?.let { it as? JSONObject ?: throw Fail("$label.attributes") }
                InlineAction.PostEvent(event, attributes)
            }
            "copy" -> InlineAction.Copy(
                text = (dict.opt("text") as? String)?.takeIf { it.isNotEmpty() } ?: throw Fail("$label.text"),
                toast = dict.optionalString("toast", "$label.toast"),
            )
            else -> {
                PWLog.warn(TAG, "$label: unknown type '$type' ignored")
                null
            }
        }
    }

    // Style

    private fun style(dict: JSONObject, label: String) = InlineStyle(
        background = dict.optionalColor("background", "$label.background"),
        cornerRadius = dict.optionalLength("cornerRadius", "$label.cornerRadius")?.toFloat() ?: 0f,
        padding = dict.present("padding")?.let { padding(it, "$label.padding") } ?: InlineInsets.ZERO,
        gradient = dict.present("gradient")?.let { gradient(it, "$label.gradient") },
        backgroundImage = dict.present("backgroundImage")?.let { url(dict, "backgroundImage", label) },
        shadow = dict.present("shadow")?.let { shadow(it, "$label.shadow") },
        border = dict.present("border")?.let { border(it, "$label.border") },
        glass = dict.present("glass")?.let { glass(it, "$label.glass") } ?: false,
    )

    /** `{"colors": ["#hex", ...], "angle": 90}`: at least two colors, angle in degrees. */
    private fun gradient(raw: Any, label: String): InlineGradient {
        val dict = raw.asObject(label)
        val list = dict.opt("colors") as? JSONArray
        if (list == null || list.length() < 2) throw Fail("$label.colors")
        val colors = List(list.length()) { color(list.opt(it)) ?: throw Fail("$label.colors") }
        return InlineGradient(colors, dict.optionalNumber("angle", "$label.angle")?.toFloat() ?: 90f)
    }

    private fun shadow(raw: Any, label: String): InlineShadow {
        val dict = raw.asObject(label)
        return InlineShadow(
            color = dict.optionalColor("color", "$label.color") ?: InlineShadow.DEFAULT_COLOR,
            radius = dict.optionalLength("radius", "$label.radius")?.toFloat() ?: 8f,
            x = dict.optionalLength("x", "$label.x", OFFSETS)?.toFloat() ?: 0f,
            y = dict.optionalLength("y", "$label.y", OFFSETS)?.toFloat() ?: 4f,
        )
    }

    private fun border(raw: Any, label: String): InlineBorder {
        val dict = raw.asObject(label)
        val width = dict.optionalLength("width", "$label.width") ?: 1.0
        val color = dict.optionalColor("color", "$label.color")
        if (width <= 0 || color == null) throw Fail(label)
        return InlineBorder(width.toFloat(), color)
    }

    /** `true`/`false`, or an object with an optional `tint` color that turns glass on. */
    private fun glass(raw: Any, label: String): Boolean {
        if (raw is Boolean) return raw
        raw.asObject(label).optionalColor("tint", "$label.tint")
        return true
    }

    /** `8`, `[vertical, horizontal]` or `[top, right, bottom, left]`. */
    private fun padding(raw: Any, label: String): InlineInsets {
        val values = when (raw) {
            is JSONArray -> List(raw.length()) { number(raw.opt(it)) ?: throw Fail(label) }
            else -> listOf(number(raw) ?: throw Fail(label))
        }
        if (values.any { it !in LENGTHS }) throw Fail(label)
        val v = values.map { it.toFloat() }
        return when (v.size) {
            1 -> InlineInsets(v[0], v[0], v[0], v[0])
            2 -> InlineInsets(v[0], v[1], v[0], v[1])
            4 -> InlineInsets(v[0], v[1], v[2], v[3])
            else -> throw Fail(label)
        }
    }

    /** A media URL with a scheme; spaces and non-ASCII characters are percent-encoded. */
    private fun url(dict: JSONObject, key: String, label: String): String {
        val string = (dict.opt(key) as? String)?.takeIf { it.isNotEmpty() } ?: throw Fail("$label.$key")
        val encoded = percentEncode(string)
        if (linkSchemeOf(encoded) == null) throw Fail("$label.$key")
        return encoded
    }

    /** An action link with a scheme, kept byte-for-byte so that it reaches `ACTION_VIEW` unchanged. */
    private fun link(dict: JSONObject, key: String, label: String): String {
        val string = (dict.opt(key) as? String)?.takeIf { it.isNotEmpty() } ?: throw Fail("$label.$key")
        if (linkSchemeOf(string) == null) throw Fail("$label.$key")
        return string
    }

    /** The scheme of a banner link, which needs a non-empty rest after it. */
    private fun linkSchemeOf(string: String): String? = schemeOf(string)?.takeIf { it.length + 1 < string.length }

    private fun percentEncode(string: String): String {
        val out = StringBuilder()
        for (byte in string.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            if (c < 0x80 && c.toChar().let { it.isLetterOrDigit() || it in URL_ALLOWED }) {
                out.append(c.toChar())
            } else {
                out.append('%').append(HEX[c shr 4]).append(HEX[c and 0xF])
            }
        }
        return out.toString()
    }

    private const val URL_ALLOWED = "-._~!$&'()*+,;=:@/?#[]%"
    private const val HEX = "0123456789ABCDEF"

    // Values

    private fun JSONObject.present(key: String): Any? = opt(key)?.takeIf { it != JSONObject.NULL }

    private fun Any.asObject(label: String): JSONObject = this as? JSONObject ?: throw Fail(label)

    private fun number(raw: Any?): Double? = (raw as? Number)?.toDouble()?.takeIf { it.isFinite() }

    private fun color(raw: Any?): Int? = (raw as? String)?.takeIf { it.startsWith("#") }?.let { InAppColorParser.parse(it) }

    private fun JSONObject.optionalNumber(key: String, label: String): Double? {
        val raw = present(key) ?: return null
        return number(raw) ?: throw Fail(label)
    }

    /** A length in dp; outside [range] the banner is dropped. */
    private fun JSONObject.optionalLength(key: String, label: String, range: ClosedFloatingPointRange<Double> = LENGTHS): Double? {
        val length = optionalNumber(key, label) ?: return null
        if (length !in range) throw Fail(label)
        return length
    }

    private fun JSONObject.optionalLong(key: String, label: String): Long? {
        val raw = present(key) ?: return null
        return when (raw) {
            is Int -> raw.toLong()
            is Long -> raw
            else -> number(raw)?.takeIf { it == Math.floor(it) && it in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble() }
                ?.toLong()
        } ?: throw Fail(label)
    }

    private fun JSONObject.optionalInt(key: String, label: String): Int? {
        val value = optionalLong(key, label) ?: return null
        if (value !in Int.MIN_VALUE..Int.MAX_VALUE) throw Fail(label)
        return value.toInt()
    }

    private fun JSONObject.optionalString(key: String, label: String): String? {
        val raw = present(key) ?: return null
        return raw as? String ?: throw Fail(label)
    }

    private fun JSONObject.optionalBool(key: String, label: String): Boolean? {
        val raw = present(key) ?: return null
        return raw as? Boolean ?: throw Fail(label)
    }

    private fun <T> JSONObject.optionalEnum(key: String, label: String, values: Map<String, T>): T? {
        val string = optionalString(key, label) ?: return null
        return values[string] ?: throw Fail(label)
    }

    private fun JSONObject.optionalColor(key: String, label: String): Int? {
        val raw = present(key) ?: return null
        return color(raw) ?: throw Fail(label)
    }
}
