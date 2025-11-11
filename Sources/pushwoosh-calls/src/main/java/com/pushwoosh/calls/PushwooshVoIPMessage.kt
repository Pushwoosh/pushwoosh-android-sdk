package com.pushwoosh.calls

import android.os.Bundle

class PushwooshVoIPMessage(payload: Bundle?) {
    val callerName: String? = payload?.getString("callerName", "Unknown Caller")
    val hasVideo: Boolean = getBooleanFromBundle(payload, "video", false)
    val supportsHolding: Boolean = getBooleanFromBundle(payload, "supportsHolding", false)
    val supportsDTMF: Boolean = getBooleanFromBundle(payload, "supportsDTMF", false)
    val rawPayload: Bundle? = payload

    // our boolean values are converted to String when parsing push extras
    private fun getBooleanFromBundle(bundle: Bundle?, key: String, defaultValue: Boolean): Boolean {
        if (bundle == null || !bundle.containsKey(key)) {
            return defaultValue
        }

        val result = try {
            // We intentionally use deprecated get() because we need to handle multiple possible types
            // (Boolean, String, Int) for the same key. The push notification payload may contain
            // inconsistent types depending on how it was constructed.
            @Suppress("DEPRECATION")
            when (val value = bundle.get(key)) {
                is Boolean -> value
                is String -> {
                    when (value.lowercase()) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> defaultValue
                    }
                }
                is Int -> value != 0
                is Long -> value != 0L
                is Double -> value != 0.0
                is Float -> value != 0.0f
                else -> defaultValue
            }
        } catch (e:Exception) {
            defaultValue
        }
        return result
    }
}

