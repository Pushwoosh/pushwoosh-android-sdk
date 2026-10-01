package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp

class PreferencesStore(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    val selected: Set<String>
        get() = prefs.getStringList(KEY).toSet()

    fun isSelected(category: String): Boolean = selected.contains(category)

    fun toggle(category: String): Boolean {
        val current = selected
        val on = category !in current
        val updated = if (on) current + category else current - category
        prefs.putStringList(KEY, updated.toList())
        gateway.setTags(listOf(TagOp.PutBoolean("fav_" + category.lowercase(), on)))
        return on
    }

    companion object {
        const val KEY = "pushmart.favCategories"
    }
}
