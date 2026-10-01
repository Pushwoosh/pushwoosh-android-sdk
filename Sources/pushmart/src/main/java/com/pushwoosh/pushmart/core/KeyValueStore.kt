package com.pushwoosh.pushmart.core

interface KeyValueStore {
    fun getString(key: String, default: String = ""): String

    fun putString(key: String, value: String)

    fun getBoolean(key: String, default: Boolean = false): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getStringList(key: String): List<String>

    fun putStringList(key: String, value: List<String>)

    fun remove(key: String)
}
