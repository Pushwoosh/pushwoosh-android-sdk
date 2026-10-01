package com.pushwoosh.pushmart.routing

import com.pushwoosh.notification.PushMessage
import org.json.JSONException
import org.json.JSONObject

object PushRouting {
    fun routeFor(message: PushMessage?): Route? {
        val raw = message?.customData ?: return null
        return DeepLinkRouter.fromCustomData(parse(raw))
    }

    fun parse(json: String): Map<String, Any?> {
        return try {
            val obj = JSONObject(json)
            val map = HashMap<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = if (obj.isNull(key)) null else obj.get(key)
            }
            map
        } catch (e: JSONException) {
            emptyMap()
        }
    }
}
