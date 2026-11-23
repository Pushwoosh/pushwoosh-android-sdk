package com.pushwoosh.inline.internal.net

import androidx.annotation.VisibleForTesting
import com.pushwoosh.inline.internal.CoreInlineIdentity
import com.pushwoosh.internal.network.PushRequest
import org.json.JSONArray
import org.json.JSONObject

private const val INLINE_API_PATH = "api/v2/device-api/"

internal class GetInlineBannersRequest(
    val slots: List<String>,
    private val overrideUrl: String?,
) : PushRequest<JSONObject>() {
    var sentUserId: String? = null
        private set
    var sentAppCode: String? = null
        private set

    override fun getMethod() = "getInlineBanners"

    override fun shouldWrapRequest() = false

    override fun getApiPath(): String? = if (overrideUrl == null) INLINE_API_PATH else null

    override fun getApplicationCode(): String? = CoreInlineIdentity.appCode()

    override fun buildParams(params: JSONObject) {
        sentUserId = params.optString("userId").takeIf { it.isNotEmpty() }
        sentAppCode = params.optString("application").takeIf { it.isNotEmpty() }
        params.put("slots", JSONArray(slots))
        params.put("formats", JSONArray(listOf("native", "html")))
        params.put("nativeVersion", 1)
    }

    override fun parseResponse(response: JSONObject): JSONObject? = response.optJSONObject("slots")

    /** The body core would send over the [base] fields it adds itself. */
    @VisibleForTesting
    internal fun paramsOver(base: JSONObject): JSONObject = base.also { buildParams(it) }
}

internal class TrackInlineEventRequest(
    val event: JSONObject,
    private val apiPathMode: Boolean,
) : PushRequest<Void>() {
    override fun getMethod() = "trackInlineEvent"

    override fun shouldWrapRequest() = false

    override fun getApiPath(): String? = if (apiPathMode) INLINE_API_PATH else null

    /** The application the event was recorded for, so a queued event goes to its own host after an app code change. */
    override fun getApplicationCode(): String? =
        event.optString("application").takeIf { it.isNotEmpty() } ?: CoreInlineIdentity.appCode()

    override fun buildParams(params: JSONObject) {
        event.keys().forEach { params.put(it, event.get(it)) }
    }

    /** The body core would send over the [base] fields it adds itself. */
    @VisibleForTesting
    internal fun paramsOver(base: JSONObject): JSONObject = base.also { buildParams(it) }
}
