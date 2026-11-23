package com.pushwoosh.pushmart.routing

import com.pushwoosh.notification.PushMessage
import org.json.JSONException
import org.json.JSONObject

object PushRouting {
    fun routeFor(message: PushMessage?): Route? {
        val raw = message?.customData ?: return null
        return DeepLinkRouter.fromCustomData(parse(raw))
    }

    /** Parses custom data JSON: nested objects become nested maps, JSON null becomes null. */
    fun parse(json: String): Map<String, Any?> {
        return try {
            toMap(JSONObject(json))
        } catch (e: JSONException) {
            emptyMap()
        }
    }

    private fun toMap(obj: JSONObject): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] =
                if (obj.isNull(key)) null
                else
                    when (val value = obj.get(key)) {
                        is JSONObject -> toMap(value)
                        else -> value
                    }
        }
        return map
    }
}
