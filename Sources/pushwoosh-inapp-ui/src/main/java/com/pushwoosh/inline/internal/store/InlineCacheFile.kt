package com.pushwoosh.inline.internal.store

import com.pushwoosh.internal.utils.PWLog
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * A JSON file of the module, replaced atomically. The banners cache layout:
 * `{"owner", "banners": {slot: {banner, receivedAt}}, "dismissed": {id: {version, until?}},
 * "reported": {id: version}, "slots": {slot: lastSeen}}`, times in Unix epoch seconds.
 */
internal class InlineCacheFile(private val file: File, private val description: String = "Inline banners cache") {

    /** `null` when the file is missing or damaged. */
    fun read(): JSONObject? = read { JSONObject(it) }

    /** The contents through [parse]; `null` when the file is missing, unreadable or [parse] rejects it. */
    fun <T> read(parse: (String) -> T): T? {
        if (!file.exists()) return null
        return try {
            parse(file.readText())
        } catch (e: IOException) {
            PWLog.warn(TAG, "$description is not readable, starting empty: ${e.message}")
            null
        } catch (e: JSONException) {
            PWLog.warn(TAG, "$description is damaged, starting empty")
            null
        }
    }

    fun write(json: JSONObject) = write(json.toString())

    /** Replaces the file atomically: a reader sees either the old or the new contents. */
    fun write(contents: String) {
        val temp = File(file.path + ".tmp")
        try {
            file.parentFile?.mkdirs()
            temp.writeText(contents)
            if (!temp.renameTo(file)) throw IOException("rename to ${file.name} failed")
        } catch (e: IOException) {
            temp.delete()
            PWLog.warn(TAG, "Failed to persist ${description.replaceFirstChar { it.lowercase() }}: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "InlineCacheFile"
    }
}
