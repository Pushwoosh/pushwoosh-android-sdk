package com.pushwoosh.inline.internal.store

import com.pushwoosh.inline.internal.Cancelable
import com.pushwoosh.inline.internal.InlineClock
import com.pushwoosh.inline.internal.InlineScheduler
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.parser.InlineParser
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executor

/**
 * One banner per slot, persisted so received banners survive a cold start without network.
 * Expired banners are dropped on load, on a timer while the app runs, and whenever the manager
 * re-checks: foreground, a slot view attaching, a banner event. A banner the user closed is not
 * taken back at the same or a lower version until its `expireDate`.
 *
 * Banners, closed banners and reported shows belong to one [owner] (`user|appCode`): an answer
 * for another owner is not applied, and [syncOwner] clears them for a new one. Between a user
 * change and its sync the previous user's banners stay ([previousUserId]). Also keeps the slot
 * ids the app uses, forgotten after 30 days without being seen. A test banner covers the live
 * banner of its slot until relaunch; it, its close and its reported show stay in memory.
 *
 * Main thread only; the file is written on [writer], in call order.
 */
internal class InlineStore(
    file: File,
    private val clock: InlineClock,
    private val scheduler: InlineScheduler,
    private val writer: Executor,
) {
    private class Dismissal(val version: Int, val until: Long?)

    private class Stored(val banner: InlineBanner, val receivedAt: Long)

    private val cacheFile = InlineCacheFile(file)

    /** Called per slot whose shown banner changed, after its subscribers; not on [load]. */
    var onChange: ((slotId: String, banner: InlineBanner?) -> Unit)? = null

    /** The user and application the stored banners belong to; `null` = not known yet. */
    var owner: String? = null
        private set

    /** The file was read (or there was none); until then nothing is written over it. */
    var isLoaded = false
        private set

    /** The owner from [load], [setCurrentOwner] or [syncOwner], to tell banners of another application. */
    private var currentOwner: String? = null

    /** The owner the SDK works for was given; `false` after a [load] while it was not known. */
    val hasCurrentOwner: Boolean get() = currentOwner != null

    private val banners = HashMap<String, Stored>()
    private val dismissed = HashMap<String, Dismissal>()
    private val reported = HashMap<String, Int>()
    private val knownSlots = HashMap<String, Long>()
    private val testBanners = HashMap<String, InlineBanner>()
    private val dismissedTests = HashMap<String, Dismissal>()
    private val reportedTests = HashMap<String, Int>()
    private val subscribers = HashMap<String, MutableList<(InlineBanner?) -> Unit>>()
    private var expiryTask: Cancelable? = null

    private fun nowSec(): Long = clock.nowMillis() / 1000

    /** Reads the file; the stored state of another owner than [currentOwner] is discarded. */
    fun load(currentOwner: String?) {
        val watched = subscribers.keys.toList()
        val shownBefore = watched.associateWith { banner(it) }
        this.currentOwner = currentOwner
        banners.clear()
        dismissed.clear()
        reported.clear()
        knownSlots.clear()
        val json = cacheFile.read()
        isLoaded = true
        owner = json?.optStringOrNull("owner")
        if (json != null) decode(json)
        if (currentOwner != null && owner != currentOwner) {
            val hadData = banners.isNotEmpty() || dismissed.isNotEmpty() || reported.isNotEmpty()
            banners.clear()
            dismissed.clear()
            reported.clear()
            testBanners.clear()
            dismissedTests.clear()
            reportedTests.clear()
            if (hadData) PWLog.info(TAG, "Inline banners cache belongs to another user or application, discarded")
            owner = currentOwner
            if (json != null) save()
        }
        scheduleExpiry()
        for (slotId in watched) {
            val now = banner(slotId)
            if (!isSame(shownBefore[slotId], now)) subscribers[slotId]?.toList()?.forEach { it(now) }
        }
    }

    /**
     * Test banner over live, not expired, not dismissed; `null` when the store holds banners of
     * another application than the current one. Control banners are returned too.
     */
    fun banner(slotId: String): InlineBanner? {
        if (!belongsToCurrentApplication()) return null
        val nowSec = nowSec()
        return listOfNotNull(testBanners[slotId], banners[slotId]?.banner).firstOrNull { !it.isExpired(nowSec) }
    }

    /**
     * Puts the answer [slots] for the [requested] slot ids: the slot's banner, or empty when it has
     * none; a test banner goes over the live one and an empty answer leaves it. A banner with the
     * same id and a version not above the stored one is a repeat and is skipped. `false` = the answer
     * is for another owner and nothing was applied.
     */
    fun apply(requested: List<String>, slots: JSONObject, forOwner: String?): Boolean {
        if (owner == null && forOwner != null) syncOwner(forOwner)
        if (forOwner == null || forOwner != owner) {
            PWLog.info(TAG, "Inline slots answer for another user or application is dropped")
            return false
        }
        var liveChanged = false
        val changed = trackChanges(requested) {
            val receivedAt = nowSec()
            for (slotId in requested) {
                val received = InlineParser.parse(slots.opt(slotId), slotId)
                when {
                    received != null && received.isTest -> {
                        if (isShowable(received) && !isRepeat(received, testBanners[slotId])) {
                            testBanners[slotId] = received
                        }
                    }
                    received != null && isShowable(received) -> {
                        if (!isRepeat(received, banners[slotId]?.banner)) {
                            banners[slotId] = Stored(received, receivedAt)
                            liveChanged = true
                        }
                    }
                    banners.remove(slotId) != null -> liveChanged = true
                }
            }
        }
        if (liveChanged) save()
        if (liveChanged || changed.isNotEmpty()) scheduleExpiry()
        notify(changed)
        return true
    }

    /** The user closed the slot's banner: it collapses and does not come back before its `expireDate`. */
    fun dismiss(slotId: String): InlineBanner? {
        if (!belongsToCurrentApplication()) return null
        val nowSec = nowSec()
        testBanners[slotId]?.takeIf { !it.isExpired(nowSec) }?.let { test ->
            testBanners.remove(slotId)
            dismissedTests[test.bannerId] = Dismissal(test.version, test.expireDate)
            PWLog.info(TAG, "Inline test banner '${test.bannerId}' closed by the user")
            notify(listOf(slotId))
            return test
        }
        val banner = banners[slotId]?.banner?.takeIf { !it.isExpired(nowSec) } ?: return null
        banners.remove(slotId)
        dismissed.values.removeAll { isOver(it) }
        dismissed[banner.bannerId] = Dismissal(banner.version, banner.expireDate)
        PWLog.info(TAG, "Inline banner '${banner.bannerId}' closed by the user")
        save()
        scheduleExpiry()
        notify(listOf(slotId))
        return banner
    }

    fun isReported(b: InlineBanner): Boolean =
        (if (b.isTest) reportedTests else reported)[b.bannerId]?.let { it >= b.version } ?: false

    /** Records that the show (or control) of [b]'s version was reported; `false` = already was, or another application's. */
    fun markReported(b: InlineBanner): Boolean {
        if (!belongsToCurrentApplication() || isReported(b)) return false
        if (b.isTest) {
            reportedTests[b.bannerId] = b.version
            return true
        }
        reported[b.bannerId] = b.version
        save()
        return true
    }

    fun rememberSlots(ids: Collection<String>) {
        val nowSec = nowSec()
        var changed = false
        for (slotId in ids) {
            if (slotId.isEmpty()) continue
            val seen = knownSlots[slotId]
            if (seen != null && nowSec - seen < SLOT_SEEN_REFRESH_SEC) continue
            knownSlots[slotId] = nowSec
            changed = true
        }
        if (changed) save()
    }

    /** Slot ids seen in the last 30 days, sorted. */
    fun knownSlots(): List<String> {
        val nowSec = nowSec()
        if (knownSlots.values.removeAll { nowSec - it >= SLOT_MEMORY_SEC }) save()
        return knownSlots.keys.sorted()
    }

    /** Clears banners, closes and reported shows when [newOwner] differs; returns the slots that had a banner. */
    fun syncOwner(newOwner: String?): List<String> {
        if (newOwner == null) return emptyList()
        val cleared = (banners.keys + testBanners.keys).sorted()
        val shownBefore = cleared.associateWith { banner(it) }
        currentOwner = newOwner
        if (newOwner == owner) {
            notify(cleared.filter { !isSame(shownBefore[it], banner(it)) })
            return emptyList()
        }
        banners.clear()
        dismissed.clear()
        reported.clear()
        testBanners.clear()
        dismissedTests.clear()
        reportedTests.clear()
        owner = newOwner
        if (cleared.isNotEmpty()) PWLog.info(TAG, "Inline banners of another user or application, ${cleared.size} slots cleared")
        save()
        scheduleExpiry()
        notify(cleared.filter { shownBefore[it] != null })
        return cleared
    }

    /** The user the stored banners belong to while the owner changed to another user of the same application. */
    fun previousUserId(currentOwner: String?): String? {
        val cacheOwner = owner ?: return null
        if (currentOwner == null || currentOwner == cacheOwner) return null
        if (appCodeOf(currentOwner) != appCodeOf(cacheOwner)) return null
        return userIdOf(cacheOwner)
    }

    /**
     * Tells the store the owner the SDK works for now, before [syncOwner] runs; while its app code
     * differs from [owner] the stored banners are not shown, closed or reported.
     */
    fun setCurrentOwner(current: String?) {
        if (current == null || current == currentOwner) return
        val changed = trackChanges((banners.keys + testBanners.keys).sorted()) { currentOwner = current }
        notify(changed)
    }

    /** Calls [handler] at once with the slot's banner, then on every change of it. */
    fun subscribe(slotId: String, handler: (InlineBanner?) -> Unit): Cancelable {
        subscribers.getOrPut(slotId) { mutableListOf() } += handler
        handler(banner(slotId))
        return Cancelable {
            subscribers[slotId]?.let { list ->
                list.removeAll { it === handler }
                if (list.isEmpty()) subscribers.remove(slotId)
            }
        }
    }

    private fun belongsToCurrentApplication(): Boolean {
        val current = currentOwner ?: return true
        val cacheOwner = owner ?: return true
        return appCodeOf(current) == appCodeOf(cacheOwner)
    }

    private fun isShowable(banner: InlineBanner): Boolean {
        val dismissal = (if (banner.isTest) dismissedTests else dismissed)[banner.bannerId]
        if (dismissal != null && dismissal.version >= banner.version && !isOver(dismissal)) {
            PWLog.info(TAG, "Inline banner '${banner.bannerId}' was closed by the user, slot '${banner.slotId}' stays empty")
            return false
        }
        return !banner.isExpired(nowSec())
    }

    private fun isRepeat(banner: InlineBanner, current: InlineBanner?): Boolean =
        current != null && current.bannerId == banner.bannerId && current.version >= banner.version

    private fun isOver(dismissal: Dismissal): Boolean = dismissal.until != null && dismissal.until <= nowSec()

    private fun isSame(a: InlineBanner?, b: InlineBanner?): Boolean = if (a == null) b == null else a.sameShown(b)

    /** Runs [mutation] and returns the [slotIds] whose shown banner it changed. */
    private inline fun trackChanges(slotIds: Collection<String>, mutation: () -> Unit): List<String> {
        val before = slotIds.distinct().map { it to banner(it) }
        mutation()
        return before.filter { (slotId, banner) -> !isSame(banner, banner(slotId)) }.map { it.first }
    }

    private fun notify(slotIds: List<String>) {
        for (slotId in slotIds) {
            val banner = banner(slotId)
            subscribers[slotId]?.toList()?.forEach { it(banner) }
            onChange?.invoke(slotId, banner)
        }
    }

    private fun scheduleExpiry() {
        expiryTask?.cancel()
        expiryTask = null
        val next = (banners.values.map { it.banner } + testBanners.values).mapNotNull { it.expireDate }.minOrNull() ?: return
        val nowMillis = clock.nowMillis()
        val left = next - nowMillis / 1000
        val delayMs = when {
            left <= 0 -> 0L
            left >= MAX_EXPIRY_WAIT_SEC -> MAX_EXPIRY_WAIT_SEC * 1000
            else -> next * 1000 - nowMillis
        }
        expiryTask = scheduler.postDelayed(delayMs) { refreshExpiry() }
    }

    /**
     * Drops the banners expired by the wall clock and tells subscribers and [onChange] about the slots
     * whose shown banner changed, then times the next expiry from now. The expiry timer runs on uptime,
     * so it is late after the process slept; the manager calls this when that matters.
     */
    fun refreshExpiry() {
        val nowSec = nowSec()
        val expired = banners.filterValues { it.banner.isExpired(nowSec) }.keys
        val expiredTests = testBanners.filterValues { it.isExpired(nowSec) }.keys
        val slotIds = (expired + expiredTests).sorted()
        // banner() already hides expired ones, so compare with what was on screen before they expired.
        val shownBefore = slotIds.associateWith { testBanners[it] ?: banners[it]?.banner }
        expired.forEach { banners.remove(it) }
        expiredTests.forEach { testBanners.remove(it) }
        val changed = if (belongsToCurrentApplication()) slotIds.filter { !isSame(shownBefore[it], banner(it)) } else emptyList()
        if (expired.isNotEmpty()) save()
        scheduleExpiry()
        notify(changed)
    }

    /**
     * When a banner has expired by the wall clock, moves the expiry timer to run at once: subscribers and
     * [onChange] hear of it right after the current main-thread work, not from inside it.
     */
    fun refreshExpirySoon() {
        val nowSec = nowSec()
        if ((banners.values.map { it.banner } + testBanners.values).none { it.isExpired(nowSec) }) return
        expiryTask?.cancel()
        expiryTask = scheduler.postDelayed(0) { refreshExpiry() }
    }

    private fun decode(json: JSONObject) {
        val nowSec = nowSec()
        json.optJSONObject("dismissed")?.let { all ->
            for (bannerId in all.keys()) {
                val record = all.optJSONObject(bannerId) ?: continue
                if (!record.has("version")) continue
                val until = if (record.isNull("until")) null else record.optLong("until")
                val dismissal = Dismissal(record.optInt("version"), until)
                if (!isOver(dismissal)) dismissed[bannerId] = dismissal
            }
        }
        json.optJSONObject("reported")?.let { all ->
            for (bannerId in all.keys()) {
                if (all.opt(bannerId) is Number) reported[bannerId] = all.optInt(bannerId)
            }
        }
        json.optJSONObject("slots")?.let { all ->
            for (slotId in all.keys()) {
                if (all.opt(slotId) is Number) knownSlots[slotId] = all.optLong(slotId)
            }
        }
        json.optJSONObject("banners")?.let { all ->
            for (slotId in all.keys()) {
                val record = all.optJSONObject(slotId) ?: continue
                val banner = InlineParser.parse(record.opt("banner"), slotId) ?: continue
                if (banner.isTest || banner.isExpired(nowSec)) continue
                banners[slotId] = Stored(banner, record.optLong("receivedAt", nowSec))
            }
        }
    }

    private fun save() {
        if (!isLoaded) return
        val json = JSONObject()
        json.put("owner", owner)
        json.put(
            "banners",
            JSONObject().apply {
                for ((slotId, stored) in banners) {
                    put(slotId, JSONObject().put("banner", stored.banner.raw).put("receivedAt", stored.receivedAt))
                }
            },
        )
        json.put(
            "dismissed",
            JSONObject().apply {
                for ((bannerId, d) in dismissed) {
                    put(bannerId, JSONObject().put("version", d.version).apply { d.until?.let { put("until", it) } })
                }
            },
        )
        json.put("reported", JSONObject().apply { for ((bannerId, version) in reported) put(bannerId, version) })
        json.put("slots", JSONObject().apply { for ((slotId, seen) in knownSlots) put(slotId, seen) })
        writer.execute { cacheFile.write(json) }
    }

    private companion object {
        const val TAG = "InlineStore"
        const val SLOT_MEMORY_SEC = 30L * 24 * 3600
        const val SLOT_SEEN_REFRESH_SEC = 24L * 3600

        /** A far `expireDate` is checked again after this wait instead of one timer that long. */
        const val MAX_EXPIRY_WAIT_SEC = 24L * 3600

        fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

        /** The app code has no `|`, so the owner splits at its last one. */
        fun appCodeOf(owner: String): String = owner.substringAfterLast('|')

        fun userIdOf(owner: String): String = owner.substringBeforeLast('|')
    }
}
