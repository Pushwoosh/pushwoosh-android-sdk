package com.pushwoosh.calls

import android.os.Bundle

/**
 * Data model representing a VoIP push notification message.
 *
 * Parses VoIP call data from push notification payload and provides typed access to call properties.
 * Boolean fields support automatic conversion from String, Int, and Boolean types.
 *
 * @see PushwooshCallSettings
 */
class PushwooshVoIPMessage(payload: Bundle?) {
    /** Name of the caller to display in UI. Defaults to "Unknown Caller" if not present. */
    val callerName: String? = payload?.getString("callerName", "Unknown Caller")

    /** Whether this is a video call (`true`) or audio-only (`false`). */
    val hasVideo: Boolean = getBooleanFromBundle(payload, "video", false)

    /** Whether the call supports being put on hold. */
    val supportsHolding: Boolean = getBooleanFromBundle(payload, "supportsHolding", false)

    /** Whether the call supports DTMF (Dual-Tone Multi-Frequency) tones for dialpad input. */
    val supportsDTMF: Boolean = getBooleanFromBundle(payload, "supportsDTMF", false)

    /** Unique identifier for this call. Null if not provided in the push payload. */
    val callId: String? = getStringFromBundle(payload, "callId")

    /** Whether this is a call cancellation message. When `true`, triggers the call cancellation event. */
    val cancelCall: Boolean = getBooleanFromBundle(payload, "cancelCall", false)

    /** Original push notification payload Bundle with all raw data including custom fields. */
    val rawPayload: Bundle? = payload

    private fun getStringFromBundle(bundle: Bundle?, key: String): String? {
        if (bundle == null || !bundle.containsKey(key)) {
            return null
        }

        return try {
            @Suppress("DEPRECATION")
            when (val value = bundle.get(key)) {
                null -> null
                is String -> value
                is Int -> value.toString()
                is Long -> value.toString()
                is Double -> {
                    // Convert 123.0 → "123" (remove decimal if it's a whole number)
                    if (value % 1.0 == 0.0) {
                        value.toLong().toString()
                    } else {
                        value.toString()
                    }
                }
                is Float -> {
                    // Convert 123.0f → "123" (remove decimal if it's a whole number)
                    if (value % 1.0f == 0.0f) {
                        value.toLong().toString()
                    } else {
                        value.toString()
                    }
                }
                else -> value.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    // our boolean values are converted to String when parsing push extras
    private fun getBooleanFromBundle(bundle: Bundle?, key: String, defaultValue: Boolean): Boolean {
        if (bundle == null || !bundle.containsKey(key)) {
            return defaultValue
        }

        val result = try {
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

