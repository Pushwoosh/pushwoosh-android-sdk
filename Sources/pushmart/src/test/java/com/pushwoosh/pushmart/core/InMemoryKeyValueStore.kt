package com.pushwoosh.pushmart.core

class InMemoryKeyValueStore : KeyValueStore {
    private val values = HashMap<String, Any>()

    override fun getString(key: String, default: String): String = values[key] as? String ?: default

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        values[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringList(key: String): List<String> =
        values[key] as? List<String> ?: emptyList()

    override fun putStringList(key: String, value: List<String>) {
        values[key] = value.toList()
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
