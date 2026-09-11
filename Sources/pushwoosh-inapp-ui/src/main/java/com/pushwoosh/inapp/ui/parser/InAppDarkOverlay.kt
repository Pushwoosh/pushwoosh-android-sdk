package com.pushwoosh.inapp.ui.parser

import org.json.JSONArray
import org.json.JSONObject

/**
 * Applies the sparse `dark` overlay of a native-config display block (SDK-971).
 *
 * Only visual keys cross over from `dark`: color leaves (`background`, `color`) and media leaves
 * (`image`, `poster`, `fallback`); nested objects (`title`, `cover`, `border`, …) merge
 * recursively, `buttons`/`items` arrays positionally with equal length required. Everything
 * else — texts, `action`, timings, unknown keys, JSON nulls — is ignored silently, so a theme
 * can never change what the message says or does. A scalar in `dark` where the light value is
 * an object (e.g. `"title": "#FFFFFF"`) is silently ignored, neither applied nor treated as
 * damage. The `dark` overlay may introduce optional visual leaves absent from light (e.g. add
 * `image` to a modal that had none). Structural damage (non-object `dark`, non-/mismatched
 * arrays, non-object array elements) fails the whole overlay: [merge] returns `null` and the
 * caller falls back to the light block. Value-level damage (bad hex, wrong scalar type) is
 * copied through on purpose: the strict parser downstream rejects the merged block and the
 * caller falls back the same way.
 */
internal object InAppDarkOverlay {

    private val VISUAL_LEAF_KEYS = setOf("background", "color", "image", "poster", "fallback")
    private val POSITIONAL_ARRAY_KEYS = setOf("buttons", "items")

    /**
     * Returns a merged deep copy of [block] with its `dark` key consumed and removed, or `null`
     * when the overlay is structurally broken. Never mutates [block]. Callers must check that
     * `dark` is present and not JSON null before calling.
     */
    fun merge(block: JSONObject): JSONObject? {
        val dark = block.opt("dark") as? JSONObject ?: return null
        val copySource: String? = block.toString()
        val merged = JSONObject(copySource ?: return null)
        merged.remove("dark")
        return if (overlay(merged, dark)) merged else null
    }

    private fun overlay(light: JSONObject, dark: JSONObject): Boolean {
        for (key in dark.keys()) {
            val darkValue = dark.opt(key)
            val lightValue = light.opt(key)
            when {
                darkValue == null || darkValue == JSONObject.NULL -> Unit
                key in POSITIONAL_ARRAY_KEYS -> {
                    if (darkValue !is JSONArray || lightValue !is JSONArray) return false
                    if (darkValue.length() != lightValue.length()) return false
                    for (i in 0 until darkValue.length()) {
                        val darkItem = darkValue.optJSONObject(i) ?: return false
                        val lightItem = lightValue.optJSONObject(i) ?: return false
                        if (!overlay(lightItem, darkItem)) return false
                    }
                }
                lightValue is JSONObject -> {
                    if (darkValue is JSONObject && !overlay(lightValue, darkValue)) return false
                }
                key in VISUAL_LEAF_KEYS -> {
                    val valueToPut = when (darkValue) {
                        is JSONArray -> JSONArray(darkValue.toString())
                        is JSONObject -> JSONObject(darkValue.toString())
                        else -> darkValue
                    }
                    light.put(key, valueToPut)
                }
                else -> Unit
            }
        }
        return true
    }
}
