package com.pushwoosh.inapp.ui.parser

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeConfigFixtureTest {

    private val validFixtures = listOf(
        "banner.json", "carousel.json", "fullscreen.json", "modal.json", "stories.json",
        "banner-no-autodismiss.json", "modal-empty-buttons.json", "fullscreen-empty-buttons.json",
        "stories-single-empty-buttons.json", "carousel-minimal.json"
    )

    @Test
    fun everyValidFixtureParses() {
        for (name in validFixtures) {
            assertNotNull("valid fixture '$name' must parse", InAppConfigParser.parse(readFixture("valid/$name")))
        }
    }

    @Test
    fun everyInvalidCaseIsRejected() {
        val manifest = JSONArray(readFixture("invalid-cases.json"))
        for (i in 0 until manifest.length()) {
            val case = manifest.getJSONObject(i)
            val mutated = mutate(readFixture("valid/${case.getString("base")}"), case)
            assertNull("invalid case must be rejected: ${case.getString("reason")}", InAppConfigParser.parse(mutated))
        }
    }

    private fun readFixture(relative: String): String =
        javaClass.getResourceAsStream("/native-config/$relative")!!.bufferedReader().use { it.readText() }

    private fun mutate(baseJson: String, case: JSONObject): String {
        val root = JSONObject(baseJson)
        val path = case.getString("path").split(".")
        var container: Any = root
        for (i in 0 until path.size - 1) {
            container = child(container, path[i])
        }
        val leaf = path.last()
        when (case.getString("op")) {
            "delete" -> deleteChild(container, leaf)
            "set" -> setChild(container, leaf, case.opt("value"))
            else -> throw IllegalArgumentException("unknown op ${case.getString("op")}")
        }
        return root.toString()
    }

    private fun child(container: Any, segment: String): Any =
        if (container is JSONArray) container.get(segment.toInt()) else (container as JSONObject).get(segment)

    private fun deleteChild(container: Any, segment: String) {
        if (container is JSONArray) container.remove(segment.toInt()) else (container as JSONObject).remove(segment)
    }

    private fun setChild(container: Any, segment: String, value: Any?) {
        val v = value ?: JSONObject.NULL
        if (container is JSONArray) container.put(segment.toInt(), v) else (container as JSONObject).put(segment, v)
    }
}
