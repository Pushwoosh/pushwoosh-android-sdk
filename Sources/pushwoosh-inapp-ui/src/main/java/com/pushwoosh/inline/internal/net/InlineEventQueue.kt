package com.pushwoosh.inline.internal.net

import android.os.Bundle
import com.pushwoosh.inline.internal.Cancelable
import com.pushwoosh.inline.internal.InlineClock
import com.pushwoosh.inline.internal.InlineScheduler
import com.pushwoosh.inline.internal.store.InlineCacheFile
import com.pushwoosh.internal.network.ConnectionException
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.network.NetworkModule
import com.pushwoosh.internal.network.RequestBlockedException
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executor
import kotlin.math.ln
import kotlin.random.Random

internal interface InlineEventSink {
    /** [onResult] runs on the main thread; `false` means the event was queued or dropped. */
    fun send(event: JSONObject, onResult: (delivered: Boolean) -> Unit)
}

internal interface InlineEventSender {
    fun send(event: JSONObject, root: String?, callback: (NetworkException?) -> Unit)
}

internal class CoreInlineEventSender(private val metaData: () -> Bundle?) : InlineEventSender {
    override fun send(event: JSONObject, root: String?, callback: (NetworkException?) -> Unit) {
        val request = TrackInlineEventRequest(event, InlineEndpoint.overrideUrl(metaData()) == null)
        NetworkModule.getRequestManager().sendRequest(request, root) { result -> callback(result.exception) }
    }
}

/**
 * Delivers `trackInlineEvent` calls and keeps the ones that could not be delivered on disk. A queued event
 * stays on the root of its first attempt, waits for the network without spending attempts, and is retried
 * with a growing backoff until it is delivered, refused for good, out of attempts or older than 3 days.
 * While connectivity cannot be read ([isOnline] = `null`) a lost request spends no attempt either: the
 * event is retried with the backoff until it is delivered or too old.
 * An event core refuses to send (stopped server communication, missing reverse proxy) spends no attempt
 * and is retried only by [flush], not by the queue's own timer; once device data is removed the queue is
 * cleared and nothing is kept. All methods run on the main thread; [writer] only persists snapshots.
 */
internal class InlineEventQueue(
    file: File,
    private val sender: InlineEventSender,
    private val clock: InlineClock,
    private val scheduler: InlineScheduler,
    /** `null` = connectivity cannot be read; sending is still tried. */
    private val isOnline: () -> Boolean?,
    private val rootNow: () -> String?,
    private val writer: Executor,
    private val isDeviceRemoved: () -> Boolean,
    private val random: Random = Random.Default,
) : InlineEventSink {
    private class Entry(
        val event: JSONObject,
        val root: String?,
        var attempts: Int,
        val createdAt: Long,
        var nextAt: Long,
    ) {
        var inFlight = false

        /** Requests lost while connectivity could not be read; they time the backoff instead of [attempts]. */
        var unknownLosses = 0

        /** Refused by core's gates; retried by the next [flush], not by the timer. */
        var blocked = false

        /** Deleted by [clear] while its request was in flight. */
        var dropped = false
    }

    /** Called on the main thread with an event that reached the server after having been queued. */
    var onQueuedDelivered: ((JSONObject) -> Unit)? = null

    private val queueFile = InlineCacheFile(file, "Inline event queue")
    private val entries = load().toMutableList()
    private var wakeUp: Cancelable? = null

    override fun send(event: JSONObject, onResult: (delivered: Boolean) -> Unit) {
        val now = clock.nowMillis()
        val entry = Entry(event, rootNow(), attempts = 0, createdAt = now, nextAt = now)
        if (isOnline() == false) {
            enqueue(entry)
            onResult(false)
            return
        }
        attempt(entry) { delivered -> onResult(delivered) }
    }

    /**
     * Sends every entry that is due, blocked ones too; call when the network is back, a session starts, the
     * process starts or core starts server communication again.
     * [networkBack] = the system just reported a network, which the connectivity state may not show yet.
     */
    fun flush(networkBack: Boolean = false) = sendDue(networkBack, withBlocked = true)

    /** Device data was removed: every queued event is deleted from memory and file and never sent. */
    fun clear() {
        entries.forEach { it.dropped = true }
        entries.clear()
        wakeUp?.cancel()
        wakeUp = null
        persist()
    }

    private fun sendDue(networkBack: Boolean, withBlocked: Boolean) {
        val now = clock.nowMillis()
        if (entries.removeAll { now - it.createdAt >= TTL_MS && !it.inFlight }) persist()
        val online = networkBack || isOnline() != false
        if (online) {
            entries.filter { it.nextAt <= now && !it.inFlight && (withBlocked || !it.blocked) }.forEach { entry ->
                if (!entry.dropped) attempt(entry) { delivered -> if (delivered) onQueuedDelivered?.invoke(entry.event) }
            }
        }
        reschedule(online)
    }

    private fun attempt(entry: Entry, onDone: (delivered: Boolean) -> Unit) {
        entry.inFlight = true
        entry.blocked = false
        sender.send(entry.event, entry.root) { error ->
            entry.inFlight = false
            if (error == null) {
                if (entries.remove(entry)) persist()
                onDone(true)
            } else {
                handleFailure(entry, error)
                onDone(false)
            }
        }
    }

    private fun handleFailure(entry: Entry, error: NetworkException) {
        if (entry.dropped) return
        val now = clock.nowMillis()
        if (error is RequestBlockedException) {
            holdBlocked(entry)
            return
        }
        if (isNotReady(error)) {
            entry.nextAt = now + NOT_READY_HOLD_MS
            enqueue(entry)
            return
        }
        val connection = error as? ConnectionException
        if (connection == null || !connection.isTransient()) {
            PWLog.warn(TAG, "Inline event dropped: not retriable")
            if (entries.remove(entry)) persist()
            return
        }
        val lost = connection.statusCode == 0 && connection.pushwooshStatusCode == 0
        when (if (lost) isOnline() else true) {
            false -> entry.nextAt = now
            null -> {
                entry.unknownLosses += 1
                entry.nextAt = now + backoffMillis(entry.unknownLosses)
            }
            true -> {
                entry.attempts += 1
                if (entry.attempts >= MAX_ATTEMPTS) {
                    PWLog.warn(TAG, "Inline event dropped: out of attempts")
                    if (entries.remove(entry)) persist()
                    return
                }
                entry.nextAt = now + backoffMillis(entry.attempts)
            }
        }
        enqueue(entry)
    }

    /**
     * Keeps a refused event for the next [flush], without a timer; on a removed device the event is dropped
     * and the queue cleared. A held event's stored state does not change, so only a new one is written.
     */
    private fun holdBlocked(entry: Entry) {
        if (isDeviceRemoved()) {
            if (entries.isNotEmpty()) {
                PWLog.info(TAG, "Device data was removed, queued inline events are deleted")
                clear()
            }
            return
        }
        entry.blocked = true
        if (entry !in entries) enqueue(entry)
    }

    private fun enqueue(entry: Entry) {
        if (entry !in entries) entries += entry
        while (entries.size > CAP) {
            val oldest = entries.indexOfFirst { !it.inFlight }
            if (oldest < 0) break
            entries.removeAt(oldest)
        }
        persist()
        reschedule()
    }

    /**
     * True when core's request manager is not initialised yet: it answers with a bare [NetworkException].
     * Such an event is held for a while without spending an attempt. A request refused by core's gates is a
     * [RequestBlockedException]; every other failure is a [ConnectionException].
     */
    private fun isNotReady(error: NetworkException) = error.javaClass == NetworkException::class.java

    private fun backoffMillis(attempts: Int): Long {
        val base = BASE_BACKOFF_MS * (1 + ln(attempts.toDouble()) / ln(2.0))
        val jittered = base * (1 + (random.nextDouble() * 2 - 1) * JITTER)
        return jittered.toLong().coerceIn(MIN_BACKOFF_MS, MAX_BACKOFF_MS)
    }

    private fun reschedule(online: Boolean = isOnline() != false) {
        wakeUp?.cancel()
        wakeUp = null
        if (!online) return
        val next = entries.filter { !it.inFlight && !it.blocked }.minOfOrNull { it.nextAt } ?: return
        wakeUp = scheduler.postDelayed(next - clock.nowMillis()) { sendDue(networkBack = false, withBlocked = false) }
    }

    private fun persist() {
        val json = JSONArray()
        entries.forEach { entry ->
            json.put(
                JSONObject()
                    .put("event", entry.event)
                    .put("root", entry.root ?: JSONObject.NULL)
                    .put("attempts", entry.attempts)
                    .put("createdAt", entry.createdAt)
                    .put("nextAt", entry.nextAt),
            )
        }
        val snapshot = json.toString()
        writer.execute { queueFile.write(snapshot) }
    }

    private fun load(): List<Entry> {
        val json = queueFile.read { JSONArray(it) } ?: return emptyList()
        return (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let { item ->
                val event = item.optJSONObject("event") ?: return@let null
                Entry(
                    event = event,
                    root = item.optString("root").takeIf { !item.isNull("root") && it.isNotEmpty() },
                    attempts = item.optInt("attempts", 0),
                    createdAt = item.optLong("createdAt", 0),
                    nextAt = item.optLong("nextAt", 0),
                )
            }
        }.takeLast(CAP)
    }

    private companion object {
        const val TAG = "InlineEventQueue"
        const val CAP = 100
        const val MAX_ATTEMPTS = 3
        const val TTL_MS = 3L * 24 * 60 * 60 * 1000
        const val BASE_BACKOFF_MS = 25_000.0
        const val NOT_READY_HOLD_MS = 25_000L
        const val JITTER = 0.25
        const val MIN_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 6L * 60 * 60 * 1000
    }
}
