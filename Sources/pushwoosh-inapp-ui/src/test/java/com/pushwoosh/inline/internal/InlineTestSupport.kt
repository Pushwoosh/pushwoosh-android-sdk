package com.pushwoosh.inline.internal

import com.pushwoosh.inline.PushwooshInlineBanner
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.inline.PushwooshInlineListener
import com.pushwoosh.inline.internal.net.InlineEventSink
import com.pushwoosh.inline.internal.net.InlineFetchResult
import com.pushwoosh.inline.internal.net.InlineTransport
import org.json.JSONArray
import org.json.JSONObject

/** Top-level keys of the shared iOS fixture `pushmart-inline-banners.json`. */
internal val FIXTURE_KEYS = listOf(
    "home_deals",
    "home_deals@member",
    "home_deals@ordered",
    "home_deals@test",
    "shop_grid",
    "cart_checkout",
    "pdp_offer",
    "perk_card",
)

private val fixture: JSONObject by lazy {
    val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/inline/pushmart-inline-banners.json"))
    JSONObject(stream.bufferedReader().use { it.readText() })
}

/** The banners stored under [key] in the fixture: a single banner object or an array of them. */
internal fun fixtureBanners(key: String): List<JSONObject> = when (val value = fixture.get(key)) {
    is JSONObject -> listOf(JSONObject(value.toString()))
    is JSONArray -> List(value.length()) { JSONObject(value.getJSONObject(it).toString()) }
    else -> error("fixture key '$key' holds neither a banner nor a list of banners")
}

internal fun nativeBanner(id: String = "b1", version: Int = 1, extra: JSONObject.() -> Unit = {}): JSONObject =
    JSONObject()
        .put("bannerId", id)
        .put("version", version)
        .put("format", "native")
        .put("native", JSONObject().put("root", JSONObject().put("type", "text").put("text", "hi")))
        .apply(extra)

internal fun htmlBanner(id: String = "h1", version: Int = 1, html: String = "<p>x</p>"): JSONObject =
    JSONObject().put("bannerId", id).put("version", version).put("format", "html").put("html", html)

internal fun controlBanner(id: String = "c1", version: Int = 1): JSONObject =
    JSONObject().put("bannerId", id).put("version", version).put("control", true)

/** A clock the test moves by hand. */
internal class FakeClock(var nowMillis: Long = 0L) : InlineClock {
    override fun nowMillis(): Long = nowMillis
}

/** Runs posted tasks when [advance] moves [clock] past their time, in due order. */
internal class FakeScheduler(private val clock: FakeClock) : InlineScheduler {
    private class Task(val dueMillis: Long, val seq: Long, val run: () -> Unit) {
        var cancelled = false
    }

    private val tasks = mutableListOf<Task>()
    private var seq = 0L

    val pendingCount: Int get() = tasks.count { !it.cancelled }

    /** Time left until the earliest pending task, or `null` when none is pending. */
    val nextDelayMs: Long? get() = tasks.filter { !it.cancelled }.minOfOrNull { it.dueMillis - clock.nowMillis }

    override fun postDelayed(delayMs: Long, task: () -> Unit): Cancelable {
        val entry = Task(clock.nowMillis + delayMs.coerceAtLeast(0), seq++, task)
        tasks += entry
        return Cancelable { entry.cancelled = true }
    }

    fun advance(ms: Long) {
        val target = clock.nowMillis + ms
        while (true) {
            tasks.removeAll { it.cancelled }
            val next = tasks.filter { it.dueMillis <= target }.minWithOrNull(compareBy({ it.dueMillis }, { it.seq })) ?: break
            tasks.remove(next)
            clock.nowMillis = maxOf(clock.nowMillis, next.dueMillis)
            next.run()
        }
        clock.nowMillis = target
    }
}

/** Records `getInlineBanners` calls; the test answers each one by its index. */
internal class FakeTransport : InlineTransport {
    private val callbacks = mutableListOf<(InlineFetchResult) -> Unit>()

    /** The slots of every call, in call order. */
    val calls = mutableListOf<List<String>>()

    override fun fetch(slots: List<String>, callback: (InlineFetchResult) -> Unit) {
        calls += slots
        callbacks += callback
    }

    fun answer(i: Int, slots: JSONObject, userId: String? = null, appCode: String? = null) {
        callbacks[i](InlineFetchResult.Success(slots, userId, appCode))
    }

    fun fail(i: Int, code: Int = PushwooshInlineException.OTHER) {
        callbacks[i](InlineFetchResult.Failure(PushwooshInlineException(code, "failed")))
    }
}

/** Records sent events; the test delivers each one by its index. */
internal class FakeEventSink : InlineEventSink {
    private val results = mutableListOf<(Boolean) -> Unit>()

    val events = mutableListOf<JSONObject>()

    override fun send(event: JSONObject, onResult: (delivered: Boolean) -> Unit) {
        events += event
        results += onResult
    }

    fun deliver(i: Int, delivered: Boolean = true) {
        results[i](delivered)
    }
}

internal class FakeIdentity(
    var userId: String? = "u1",
    var hwid: String = "hwid",
    var appCode: String? = "APP-1",
) : InlineIdentity {
    override fun userId(): String? = userId

    override fun hwid(): String = hwid

    override fun appCode(): String? = appCode
}

/** Records `onSlotUpdated` as `slotId to bannerId`, plus every other callback in [shown], [clicked], [dismissed]. */
internal class RecordingListener : PushwooshInlineListener {
    val updates = mutableListOf<Pair<String, String?>>()
    val shown = mutableListOf<Pair<String, String>>()
    val clicked = mutableListOf<Triple<String, String, String?>>()
    val dismissed = mutableListOf<Pair<String, String>>()

    /** Runs on every `onSlotUpdated`, after it is recorded. */
    var onUpdate: (() -> Unit)? = null

    override fun onSlotUpdated(slotId: String, banner: PushwooshInlineBanner?) {
        updates += slotId to banner?.bannerId
        onUpdate?.invoke()
    }

    override fun onSlotShown(slotId: String, banner: PushwooshInlineBanner) {
        shown += slotId to banner.bannerId
    }

    override fun onSlotClicked(slotId: String, banner: PushwooshInlineBanner, elementId: String?) {
        clicked += Triple(slotId, banner.bannerId, elementId)
    }

    override fun onSlotDismissed(slotId: String, banner: PushwooshInlineBanner) {
        dismissed += slotId to banner.bannerId
    }
}
