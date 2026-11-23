package com.pushwoosh.inline

/**
 * A banner the server put into an inline slot. Immutable snapshot handed to the app through
 * [PushwooshInlineListener] and `PushwooshInline.getBanner`.
 *
 * @property slotId the slot the banner belongs to.
 * @property bannerId the banner's identifier.
 * @property version grows every time the banner content changes.
 * @property format `"native"` or `"html"`; empty for a control-group banner that carries no format.
 * @property html the HTML document of an `html` banner, `null` for other formats.
 * @property payload the banner's custom data; only string values are kept.
 * @property expireDate Unix epoch seconds from which the banner is no longer shown; `null` = never expires.
 * @property isTest `true` for a draft sent to a test device; its events are not counted in campaign statistics.
 * @property json the banner object exactly as the server sent it.
 */
class PushwooshInlineBanner internal constructor(
    val slotId: String,
    val bannerId: String,
    val version: Int,
    val format: String,
    val html: String?,
    val payload: Map<String, String>,
    val expireDate: Long?,
    val isTest: Boolean,
    val json: String,
)
