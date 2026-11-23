package com.pushwoosh.pushmart.shop

import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.core.KeyValueStore
import com.pushwoosh.pushmart.sdk.PushwooshGateway
import com.pushwoosh.pushmart.sdk.TagOp

/**
 * Profile > Shopping preferences, as iOS `TagsView`: favourite categories (`fav_<category>`), the
 * shopper's size (`size`), news topics (`news_topics`) and the email opt-in (`email_news` email
 * tag). A pick that the SDK rejects rolls back.
 */
class PreferencesStore(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    val selected: Set<String>
        get() = prefs.getStringList(KEY).toSet()

    val size: String
        get() = prefs.getString(KEY_SIZE)

    val newsTopics: Set<String>
        get() = prefs.getStringList(KEY_NEWS_TOPICS).toSet()

    val emailNews: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_NEWS)

    fun isSelected(category: String): Boolean = selected.contains(category)

    fun toggle(category: String): Boolean {
        val current = selected
        val on = category !in current
        val updated = if (on) current + category else current - category
        prefs.putStringList(KEY, updated.toList())
        gateway.setTags(listOf(TagOp.PutString(favTag(category), on.toString())))
        return on
    }

    fun selectSize(value: String, onFailure: () -> Unit) {
        val previous = size
        prefs.putString(KEY_SIZE, value)
        gateway.setTags(listOf(TagOp.PutString(TAG_SIZE, value))) { result ->
            if (result.isSuccess) return@setTags
            if (size == value) prefs.putString(KEY_SIZE, previous)
            onFailure()
        }
    }

    /** Stores [ids] once the SDK took them, then posts the iOS `Subscription Segments` event. */
    fun saveNewsTopics(ids: List<String>, onResult: (saved: Boolean) -> Unit) {
        gateway.setTags(listOf(TagOp.PutList(TAG_NEWS_TOPICS, ids))) { result ->
            if (result.isSuccess) prefs.putStringList(KEY_NEWS_TOPICS, ids)
            onResult(result.isSuccess)
        }
        gateway.postEvent(EVENT_SUBSCRIPTION_SEGMENTS, emptyMap())
    }

    /** Does nothing without an [email], as the switch needs the one from Your details. */
    fun setEmailNews(on: Boolean, email: String, onFailure: () -> Unit) {
        if (email.isEmpty()) return
        prefs.putBoolean(KEY_EMAIL_NEWS, on)
        gateway.setEmailTags(email, listOf(TagOp.PutBoolean(TAG_EMAIL_NEWS, on))) { result ->
            if (result.isSuccess) return@setEmailTags
            if (emailNews == on) prefs.putBoolean(KEY_EMAIL_NEWS, !on)
            onFailure()
        }
    }

    /**
     * Merges the device's tags into the local picks: a `fav_<category>` tag that reads as on or off
     * applies, any other value or a missing tag leaves the category as it is; `size` applies only
     * when it is one of [SIZES]. [onRestored] runs when tags came back.
     */
    fun restore(onRestored: () -> Unit) {
        gateway.getTags { result ->
            val tags = result.data
            if (!result.isSuccess || tags == null) return@getTags
            val updated = selected.toMutableSet()
            for (category in Catalog.preferenceCategories) {
                when (isOn(tags[favTag(category)])) {
                    true -> updated.add(category)
                    false -> updated.remove(category)
                    null -> {}
                }
            }
            prefs.putStringList(KEY, updated.toList())
            val savedSize = tags[TAG_SIZE] as? String
            if (savedSize != null && savedSize in SIZES) prefs.putString(KEY_SIZE, savedSize)
            onRestored()
        }
    }

    private fun favTag(category: String): String = "fav_" + category.lowercase()

    private fun isOn(value: Any?): Boolean? =
        when (value) {
            is Boolean -> value
            is String ->
                when (value) {
                    "true",
                    "1" -> true
                    "false",
                    "0" -> false
                    else -> null
                }
            is Number ->
                when (value.toDouble()) {
                    1.0 -> true
                    0.0 -> false
                    else -> null
                }
            else -> null
        }

    companion object {
        const val KEY = "pushmart.favCategories"
        const val KEY_SIZE = "pushmart.prefs.size"
        const val KEY_NEWS_TOPICS = "pushmart.prefs.newsTopics"
        const val KEY_EMAIL_NEWS = "pushmart.prefs.emailNews"

        /** iOS `TagsView.sizes`. */
        val SIZES = listOf("XS", "S", "M", "L", "XL")

        /** iOS `TagsView.newsTopics` ids, in display order. */
        val NEWS_TOPICS = listOf("new_in", "sales", "drops")

        private const val TAG_SIZE = "size"
        private const val TAG_NEWS_TOPICS = "news_topics"
        private const val TAG_EMAIL_NEWS = "email_news"
        private const val EVENT_SUBSCRIPTION_SEGMENTS = "Subscription Segments"
    }
}
