package com.pushwoosh.pushmart.core

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

class SharedPrefsKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String, default: String): String =
        prefs.getString(key, null) ?: default

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    override fun putStringList(key: String, value: List<String>) {
        prefs.edit().putString(key, JSONArray(value).toString()).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
