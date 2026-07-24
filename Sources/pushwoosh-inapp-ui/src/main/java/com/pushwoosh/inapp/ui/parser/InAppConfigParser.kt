package com.pushwoosh.inapp.ui.parser

import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.FullscreenContent
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.ModalContent
import com.pushwoosh.inapp.ui.model.StoriesContent
import com.pushwoosh.inapp.ui.model.StoryItem
import com.pushwoosh.internal.utils.PWLog
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "InAppConfigParser"

/**
 * Decodes the native-config JSON into an [InAppMessage], strictly to the typed contract.
 *
 * Presence of `displayType` is the gate — without it this is not a native in-app and the parser
 * returns `null`. Parsing is fail-closed: a missing/malformed required field, an unknown display
 * type, a missing display block, an empty required `items` array, or any coercion attempt
 * (`showClose:"true"`, `duration:"5"`) collapses the whole config to `null`, so callers can safely
 * no-op (the parser never throws). Unknown keys are ignored at every level. The envelope fields
 * (`inAppId`/`maxDisplays`/`cooldown`/`expireDate`/`ttl`) are a deliberate extension over the
 * contract and keep their existing tolerant reading.
 */
object InAppConfigParser {

    fun parse(rawJson: String?): InAppMessage? {
        if (rawJson.isNullOrEmpty()) return null
        val root = try {
            JSONObject(rawJson)
        } catch (e: Exception) {
            return null
        }

        val displayType = (root.opt("displayType") as? String)?.lowercase(Locale.ROOT) ?: return null
        val layout = parseLayout(root, displayType) ?: run {
            PWLog.warn(TAG, "native-config: '$displayType' config is invalid, not shown")
            return null
        }

        return InAppMessage(
            id = root.envelopeString("inAppId"),
            layout = layout,
            maxDisplays = root.envelopeNumber("maxDisplays")?.toInt(),
            cooldownSec = root.envelopeNumber("cooldown")?.toLong(),
            expireEpochSec = parseExpiry(root),
            rawJson = rawJson
        )
    }

    private fun parseLayout(root: JSONObject, displayType: String): InAppLayout? = when (displayType) {
        "banner" -> root.optJSONObject("banner")?.let { parseBanner(it) }?.let { InAppLayout.Banner(it) }
        "carousel" -> root.optJSONObject("carousel")?.let { parseCarousel(it) }?.let { InAppLayout.Carousel(it) }
        "fullscreen" -> root.optJSONObject("fullscreen")?.let { parseFullscreen(it) }?.let { InAppLayout.Fullscreen(it) }
        "modal" -> root.optJSONObject("modal")?.let { parseModal(it) }?.let { InAppLayout.Modal(it) }
        "stories" -> root.optJSONObject("stories")?.let { parseStories(it) }?.let { InAppLayout.Stories(it) }
        else -> null
    }

    // MARK: - Layouts

    private fun parseBanner(dict: JSONObject): BannerContent? {
        val showClose = dict.strictBool("showClose") ?: return null
        val positionStr = dict.strictString("position") ?: return null
        val position = when (positionStr.lowercase(Locale.ROOT)) {
            "top" -> BannerPosition.TOP
            "bottom" -> BannerPosition.BOTTOM
            else -> {
                PWLog.warn(TAG, "native-config: banner 'position' must be top|bottom")
                return null
            }
        }
        val background = dict.color("background") ?: return null
        val action = parseAction(dict.requireObject("action")) ?: return null
        val image = dict.ifPresent("image") { it.strictString("image") ?: return null }
        val title = dict.ifPresent("title") { parseText(it.optJSONObject("title")) ?: return null }
        val message = dict.ifPresent("message") { parseText(it.optJSONObject("message")) ?: return null }
        val autoSec = dict.ifPresent("autoDismiss") {
            it.strictDouble("autoDismiss")?.takeIf { d -> d > 0 } ?: return null
        }
        return BannerContent(
            position = position,
            imageUrl = image,
            title = title,
            message = message,
            backgroundColor = background,
            action = action,
            autoDismissMs = ((autoSec ?: 0.0) * 1000).toLong(),
            showCloseButton = showClose
        )
    }

    private fun parseCarousel(dict: JSONObject): CarouselContent? {
        val showClose = dict.strictBool("showClose") ?: return null
        val items = dict.parseItems { parseCarouselItem(it) } ?: return null
        return CarouselContent(items, showClose)
    }

    private fun parseCarouselItem(dict: JSONObject): CarouselItem? {
        val image = dict.ifPresent("image") { it.strictString("image") ?: return null }
        val title = dict.ifPresent("title") { parseText(it.optJSONObject("title")) ?: return null }
        val message = dict.ifPresent("message") { parseText(it.optJSONObject("message")) ?: return null }
        val action = dict.ifPresent("action") { parseAction(it.optJSONObject("action")) ?: return null }
        return CarouselItem(image, title, message, action)
    }

    private fun parseFullscreen(dict: JSONObject): FullscreenContent? {
        val showClose = dict.strictBool("showClose") ?: return null
        val cover = dict.requireObject("cover") ?: return null
        val background = cover.color("background") ?: return null
        val image = cover.ifPresent("image") { it.strictString("image") ?: return null }
        val buttons = parseButtons(dict) ?: return null
        val title = dict.ifPresent("title") { parseText(it.optJSONObject("title")) ?: return null }
        val message = dict.ifPresent("message") { parseText(it.optJSONObject("message")) ?: return null }
        return FullscreenContent(
            imageUrl = image,
            backgroundColor = background,
            title = title,
            message = message,
            buttons = buttons,
            showCloseButton = showClose
        )
    }

    private fun parseModal(dict: JSONObject): ModalContent? {
        val showClose = dict.strictBool("showClose") ?: return null
        val dim = dict.strictBool("dimBackground") ?: return null
        val background = dict.color("background") ?: return null
        val buttons = parseButtons(dict) ?: return null
        val image = dict.ifPresent("image") { it.strictString("image") ?: return null }
        val title = dict.ifPresent("title") { parseText(it.optJSONObject("title")) ?: return null }
        val message = dict.ifPresent("message") { parseText(it.optJSONObject("message")) ?: return null }
        return ModalContent(
            backgroundColor = background,
            title = title,
            message = message,
            imageUrl = image,
            showCloseButton = showClose,
            buttons = buttons,
            dimsBackground = dim
        )
    }

    private fun parseStories(dict: JSONObject): StoriesContent? {
        val showClose = dict.strictBool("showClose") ?: return null
        val loops = dict.strictBool("loop") ?: return null
        val items = dict.parseItems { parseStoryItem(it) } ?: return null
        return StoriesContent(items, loops, showClose)
    }

    private fun parseStoryItem(dict: JSONObject): StoryItem? {
        val buttons = parseButtons(dict) ?: return null
        val durationSec = dict.strictDouble("duration")?.takeIf { it > 0 } ?: return null
        val durationMs = (durationSec * 1000).toLong().coerceAtMost(StoryItem.MAX_DURATION_MS)
        val image = dict.ifPresent("image") { it.strictString("image") ?: return null }
        val title = dict.ifPresent("title") { parseText(it.optJSONObject("title")) ?: return null }
        val message = dict.ifPresent("message") { parseText(it.optJSONObject("message")) ?: return null }
        return StoryItem(image, title, message, buttons, durationMs)
    }

    // MARK: - Sub-types

    /** `buttons` is required (may be empty); a non-array or any malformed element is invalid. */
    private fun parseButtons(dict: JSONObject): List<InAppButton>? {
        val array = dict.requireArray("buttons") ?: return null
        val out = ArrayList<InAppButton>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: return null
            out.add(parseButton(obj) ?: return null)
        }
        return out
    }

    private fun parseButton(dict: JSONObject): InAppButton? {
        val text = parseText(dict.requireObject("text")) ?: return null
        val background = dict.color("background") ?: return null
        val border = dict.requireObject("border") ?: return null
        val borderColor = border.color("color") ?: return null
        val radius = border.strictDouble("radius") ?: return null
        val action = parseAction(dict.requireObject("action")) ?: return null
        return InAppButton(text, background, borderColor, radius.toFloat(), action)
    }

    private fun parseText(dict: JSONObject?): InAppText? {
        val d = dict ?: return null
        val text = d.strictString("text") ?: return null
        val color = d.color("color") ?: return null
        return InAppText(text, color)
    }

    private fun parseAction(dict: JSONObject?): InAppAction? {
        val d = dict ?: return null
        val type = d.strictString("type") ?: return null
        return when (type.lowercase(Locale.ROOT)) {
            "close" -> InAppAction.Close
            "url" -> d.strictString("url")?.let { InAppAction.Url(it) }
            else -> {
                PWLog.warn(TAG, "native-config: action 'type' must be close|url")
                null
            }
        }
    }

    private fun parseExpiry(root: JSONObject): Long? {
        root.envelopeNumber("expireDate")?.let { return it.toLong() }
        val ttl = root.envelopeNumber("ttl")
        if (ttl != null && ttl > 0) {
            return System.currentTimeMillis() / 1000 + ttl.toLong()
        }
        return null
    }
}

// MARK: - Strict content readers (real JSON types only; malformed present values log the key)

private fun JSONObject.strictString(key: String): String? {
    val v = opt(key)
    if (v is String && v.isNotEmpty()) return v
    PWLog.warn(TAG, "native-config: '$key' is missing or not a non-empty string")
    return null
}

private fun JSONObject.strictBool(key: String): Boolean? {
    val v = opt(key)
    if (v is Boolean) return v
    PWLog.warn(TAG, "native-config: '$key' is missing or not a boolean")
    return null
}

private fun JSONObject.strictDouble(key: String): Double? {
    val v = opt(key)
    if (v is Number) return v.toDouble()
    PWLog.warn(TAG, "native-config: '$key' is missing or not a number")
    return null
}

/** A required nested object; a missing key or non-object value logs the key and fails closed. */
private fun JSONObject.requireObject(key: String): JSONObject? =
    optJSONObject(key) ?: run {
        PWLog.warn(TAG, "native-config: '$key' is missing or not an object")
        null
    }

/** A required array; a missing key or non-array value logs the key and fails closed. */
private fun JSONObject.requireArray(key: String): JSONArray? =
    optJSONArray(key) ?: run {
        PWLog.warn(TAG, "native-config: '$key' is missing or not an array")
        null
    }

private fun JSONObject.color(key: String): Int? {
    val hex = strictString(key) ?: return null
    return InAppColorParser.parse(hex) ?: run {
        PWLog.warn(TAG, "native-config: '$key' is not a valid #RGB/#RGBA/#RRGGBB/#RRGGBBAA color")
        null
    }
}

/**
 * Runs [read] only when [key] is present and non-null; absent → `null`. Because this is `inline`,
 * a present-but-malformed value lets [read] hit its own `?: return null`, which bails the enclosing
 * parse function (fail-closed) instead of silently treating broken as absent.
 */
private inline fun <T : Any> JSONObject.ifPresent(key: String, read: (JSONObject) -> T?): T? {
    if (!has(key) || isNull(key)) return null
    return read(this)
}

/** Reads a required, non-empty `items` array where every element must parse. */
private fun <T> JSONObject.parseItems(parse: (JSONObject) -> T?): List<T>? {
    val array = requireArray("items") ?: return null
    if (array.length() == 0) {
        PWLog.warn(TAG, "native-config: 'items' must not be empty")
        return null
    }
    val out = ArrayList<T>(array.length())
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: return null
        out.add(parse(obj) ?: return null)
    }
    return out
}

// MARK: - Envelope readers (tolerant; contract-external, intentionally unchanged)

private fun JSONObject.envelopeString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key, "").ifEmpty { null }
}

private fun JSONObject.envelopeNumber(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}
