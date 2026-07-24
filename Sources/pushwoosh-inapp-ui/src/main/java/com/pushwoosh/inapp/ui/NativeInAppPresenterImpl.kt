package com.pushwoosh.inapp.ui

import com.pushwoosh.inapp.nativeui.NativeInAppPresenter
import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import org.json.JSONObject

/**
 * Core-facing adapter: raw native-config.json in, parse via [InAppConfigParser], route via
 * [PushwooshInAppUi.route]. Never throws — the parser returns null on any malformed input.
 */
internal class NativeInAppPresenterImpl : NativeInAppPresenter {

    override fun present(configJson: String, resource: Resource): Boolean {
        val message = InAppConfigParser.parse(ensureInAppId(configJson, resource.code)) ?: return false
        NativeInAppAnalytics.register(message.rawJson, resource)
        PushwooshInAppUi.route(message)
        return true
    }
}

/**
 * iOS parity for `id = inAppId ?? resource.code`: injects [code] as `inAppId` into [configJson]
 * unless the config already carries a valid explicit id. Written into the raw JSON (not the model)
 * so the id survives the InAppOverlayActivity Intent round-trip, which re-parses this string.
 *
 * Never throws: unparseable JSON is returned unchanged (the parser then fails closed to null).
 * Overwritable = absent / non-String / empty String; a non-empty String `inAppId` always wins.
 */
internal fun ensureInAppId(configJson: String, code: String?): String {
    if (code.isNullOrEmpty()) return configJson
    val root = try {
        JSONObject(configJson)
    } catch (e: Exception) {
        return configJson
    }
    val existing = root.opt("inAppId")
    if (existing is String && existing.isNotEmpty()) return configJson
    // JSONObject.toString() is declared @Nullable on Android (returns null if serialization
    // throws internally); keep the "returned unchanged" contract instead of leaking null.
    return root.put("inAppId", code).toString() ?: configJson
}
