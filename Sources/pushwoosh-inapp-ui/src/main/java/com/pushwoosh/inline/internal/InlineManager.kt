package com.pushwoosh.inline.internal

import com.pushwoosh.inline.PushwooshInlineBanner
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.inline.PushwooshInlineListener
import com.pushwoosh.inline.PushwooshInlineRequestCallback
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.net.InlineEventSink
import com.pushwoosh.inline.internal.net.InlineFetchResult
import com.pushwoosh.inline.internal.net.InlineTransport
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.internal.utils.PWLog
import org.json.JSONObject
import java.util.UUID

/**
 * Requests slots from the server, keeps the answers in the [store] and reports slot changes to the
 * [listener]. Known slots are requested on their own at a session start (first foreground or a return
 * after 30 minutes in the background) and after a confirmed user or app code change; a slot first put
 * on screen is requested together with the other new slots of the next 0.5 s. Manual requests are
 * limited to 5 per session start, then one per 3 minutes. Until the server confirms a user change no
 * slot is requested and no answer is applied.
 *
 * Also reports what happens to the banners (`show` / `control` once per banner version and owner,
 * every `click` and `dismiss`) as `trackInlineEvent` calls; events of a banner shown before an
 * unconfirmed user change go out for the previous user. Main thread only.
 */
internal class InlineManager(
    private val store: InlineStore,
    private val transport: InlineTransport,
    private val events: InlineEventSink,
    private val identity: InlineIdentity,
    private val clock: InlineClock,
    /** Runs the manager's delayed work; slot views time their impressions on it too. */
    val scheduler: InlineScheduler,
    private val limiter: ManualRequestLimiter = ManualRequestLimiter(clock),
) {
    var listener: PushwooshInlineListener? = null

    private var sessionStarted = false
    private var backgroundedAt: Long? = null
    private val requested = RequestedSlots()

    /** Slots not requested while a user change was unconfirmed; requested once it is settled. */
    private val held = LinkedHashSet<String>()
    private val pendingNewSlots = mutableListOf<String>()

    init {
        store.onChange = { slotId, banner ->
            listener?.onSlotUpdated(slotId, banner?.takeIf { !it.isControl }?.toPublic())
        }
    }

    // region Automatic requests

    fun onForeground() {
        ensureLoaded()
        store.refreshExpiry()
        val now = clock.nowMillis()
        val isNewSession = !sessionStarted || backgroundedAt?.let { now - it >= SESSION_GAP_MS } == true
        backgroundedAt = null
        if (!isNewSession) {
            requestKnownSlots()
            return
        }
        if (sessionStarted) {
            requested.clear()
            limiter.reset()
        }
        sessionStarted = true
        if (!isUserChangeUnconfirmed()) syncOwner()
        requestKnownSlots()
    }

    fun onBackground() {
        backgroundedAt = clock.nowMillis()
    }

    /** Slots whose request failed are requested again. */
    fun onNetworkAvailable() {
        if (!sessionStarted || backgroundedAt != null) return
        requestKnownSlots()
    }

    /**
     * The server confirmed a user change: the previous user's banners are cleared and known slots
     * requested again. A confirmation that leaves the owner as it was (the SDK's first registration of
     * the hwid, `setUserId` with the current id) only requests what is still held or not requested yet.
     * Core signals every registration result, not only the last of several pending ones.
     */
    fun onUserConfirmed() {
        ensureLoaded()
        val owner = identity.owner()
        if (owner != null && owner == store.owner) {
            onUserRefused()
            return
        }
        syncOwner()
        requested.clear()
        held.clear()
        requestKnownSlots()
    }

    /**
     * The server refused a user change and the SDK went back to the previous user, whose banners stayed:
     * slots held back meanwhile and known slots not requested yet are requested together. Core signals
     * every registration result, not only the last of several pending ones.
     */
    fun onUserRefused() {
        syncOwner()
        if (isUserChangeUnconfirmed()) return
        val heldSlots = held.toSet()
        held.clear()
        requestKnownSlots(adding = heldSlots)
    }

    /** The SDK moved to another app code: the previous application's banners are cleared and known slots requested again; same owner = as [onUserConfirmed]. */
    fun onAppCodeChanged() {
        onUserConfirmed()
    }

    /** A slot view is on screen: the slot becomes known and, when it has nothing yet, is requested with the other new slots of the next 0.5 s. */
    fun slotDidAppear(slotId: String) {
        if (slotId.isEmpty()) return
        ensureLoaded()
        store.rememberSlots(listOf(slotId))
        if (requested.contains(slotId, identity.owner()) || store.banner(slotId) != null || slotId in pendingNewSlots) return
        pendingNewSlots += slotId
        if (pendingNewSlots.size == 1) scheduler.postDelayed(NEW_SLOT_DELAY_MS) { requestNewSlots() }
    }

    /**
     * Calls [handler] at once with the banner [slotId] holds (a control banner too, `null` = empty), then on
     * every change of it. Runs inside a view attach, so the expiry it finds is announced from the scheduler.
     */
    fun subscribe(slotId: String, handler: (InlineBanner?) -> Unit): Cancelable {
        ensureLoaded()
        store.refreshExpirySoon()
        return store.subscribe(slotId, handler)
    }

    /**
     * The `inline` block of a postEvent answer (`{"slots": {...}}`) updates only the slots it lists;
     * [userId] and [appCode] are the user and application the event was sent for.
     */
    fun onEventAnswer(inline: JSONObject, userId: String?, appCode: String?) {
        val slots = inline.optJSONObject("slots")?.takeIf { it.length() > 0 } ?: return
        ensureLoaded()
        val owner = identity.owner()
        if (owner == null || ownerOf(userId, appCode) != owner) {
            PWLog.warn(TAG, "Event answer with inline slots is for the previous user or application and is dropped")
            return
        }
        val slotIds = slots.keys().asSequence().toList().sorted()
        PWLog.info(TAG, "Event answer updates inline slots: ${slotIds.joinToString(", ")}")
        store.apply(slotIds, slots, owner)
    }

    fun isUserChangeUnconfirmed(): Boolean {
        ensureLoaded()
        return store.previousUserId(identity.owner()) != null
    }

    /** Known slots not requested yet, plus [adding]; nothing while a user change is unconfirmed, as its outcome requests them. */
    private fun requestKnownSlots(adding: Set<String> = emptySet()) {
        if (isUserChangeUnconfirmed()) return
        val owner = identity.owner()
        val slots = adding + store.knownSlots().filter { !requested.contains(it, owner) }
        requestAutomatically(slots.sorted())
    }

    private fun requestNewSlots() {
        val owner = identity.owner()
        val slots = pendingNewSlots.filter { !requested.contains(it, owner) }
        pendingNewSlots.clear()
        requestAutomatically(slots)
    }

    private fun requestAutomatically(slotIds: List<String>) {
        slotIds.chunked(MAX_SLOTS_PER_REQUEST).forEach { send(it, null) }
    }

    // endregion

    // region Manual requests

    fun requestSlots(ids: List<String?>?, callback: PushwooshInlineRequestCallback?) {
        if (ids == null || ids.any { it == null }) {
            callback?.onResult(error(PushwooshInlineException.INVALID_ARGUMENT, "Inline slot ids must not be null"))
            return
        }
        val slots = ids.filterNotNull().filter { it.isNotEmpty() }.distinct()
        if (slots.isEmpty()) {
            callback?.onResult(error(PushwooshInlineException.INVALID_ARGUMENT, "No inline slots to request"))
            return
        }
        if (slots.size > MAX_SLOTS_PER_REQUEST) {
            PWLog.warn(TAG, "Inline slots are requested at most $MAX_SLOTS_PER_REQUEST per call, ${slots.size} slots are not requested")
            callback?.onResult(error(PushwooshInlineException.INVALID_ARGUMENT, "Too many inline slots in one request"))
            return
        }
        ensureLoaded()
        store.rememberSlots(slots)
        if (isUserChangeUnconfirmed()) {
            PWLog.warn(TAG, "Inline slots are requested once the server confirms the user change, slots keep their banners")
            callback?.onResult(error(PushwooshInlineException.OWNER_CHANGED, "The user change is not confirmed yet"))
            return
        }
        if (!limiter.take()) {
            PWLog.warn(TAG, "Inline slots request limit reached (5 per session start, then one per 3 minutes), slots keep their banners")
            callback?.onResult(error(PushwooshInlineException.RATE_LIMITED, "Inline slots request limit reached"))
            return
        }
        send(slots, callback)
    }

    // endregion

    // region Banner events

    /** A control banner reads as empty and reports its `control` event once; a normal banner read here reports no show. */
    fun banner(slotId: String): PushwooshInlineBanner? {
        ensureLoaded()
        val banner = store.banner(slotId) ?: return null
        if (banner.isControl) {
            reportShown(banner)
            return null
        }
        return banner.toPublic()
    }

    /** A slot view kept the banner on screen long enough to count as shown. */
    fun bannerDidBecomeVisible(slotId: String, bannerId: String) {
        currentBanner(slotId, bannerId)?.let { reportShown(it) }
    }

    /** The app drew the banner itself. */
    fun trackShown(slotId: String, bannerId: String) {
        bannerDidBecomeVisible(slotId, bannerId)
    }

    /**
     * [elementId] `null` = the banner itself. `true` = the click was reported; `false` = the slot does
     * not hold that banner now (expired, replaced) and nothing was reported, so its action must not run.
     */
    fun click(slotId: String, bannerId: String, elementId: String?): Boolean {
        val banner = currentBanner(slotId, bannerId) ?: return false
        PWLog.info(TAG, "Inline banner '$bannerId' clicked in slot '$slotId', element '${elementId ?: "banner"}'")
        track("click", banner, elementId)
        listener?.onSlotClicked(slotId, banner.toPublic(), elementId)
        return true
    }

    /** Re-checks expiry (an expired banner collapses its slot), then tells whether [slotId] holds [bannerId]. */
    fun isBannerLive(slotId: String, bannerId: String): Boolean = liveBanner(slotId, bannerId) != null

    /** The slot collapses at once; once the server has the dismiss, the slot is requested for its next banner. */
    fun dismiss(slotId: String, bannerId: String) {
        val banner = currentBanner(slotId, bannerId) ?: return
        store.dismiss(slotId)
        listener?.onSlotDismissed(slotId, banner.toPublic())
        track("dismiss", banner) { delivered ->
            if (delivered) requestAutomatically(listOf(slotId))
        }
    }

    /** A banner's postEvent goes out only for a confirmed user: it would be sent for the new one. */
    fun canPostEvent(): Boolean = !isUserChangeUnconfirmed()

    /** An event left the retry queue: a delivered `dismiss` requests its slot's next banner if the slot is still empty. */
    fun onQueuedEventDelivered(event: JSONObject) {
        if (event.optString("type") != "dismiss") return
        val slotId = event.optString("slotId").takeIf { it.isNotEmpty() } ?: return
        ensureLoaded()
        if (store.banner(slotId) != null) return
        requestAutomatically(listOf(slotId))
    }

    /** Once per banner version and owner: `control` for the control group, else `show` and [PushwooshInlineListener.onSlotShown]. */
    private fun reportShown(banner: InlineBanner) {
        if (!store.markReported(banner)) return
        if (banner.isControl) {
            PWLog.info(TAG, "Inline banner '${banner.bannerId}' would be shown in slot '${banner.slotId}' (control group)")
            track("control", banner)
            return
        }
        PWLog.info(TAG, "Inline banner '${banner.bannerId}' shown in slot '${banner.slotId}'")
        track("show", banner)
        listener?.onSlotShown(banner.slotId, banner.toPublic())
    }

    private fun currentBanner(slotId: String, bannerId: String): InlineBanner? =
        liveBanner(slotId, bannerId) ?: run {
            PWLog.warn(TAG, "Inline banner '$bannerId' is not in slot '$slotId', the event is not sent")
            null
        }

    private fun liveBanner(slotId: String, bannerId: String): InlineBanner? {
        ensureLoaded()
        store.refreshExpiry()
        return store.banner(slotId)?.takeIf { it.bannerId == bannerId }
    }

    private fun track(type: String, banner: InlineBanner, elementId: String? = null, onDelivered: ((Boolean) -> Unit)? = null) {
        val event = JSONObject()
            .put("eventId", UUID.randomUUID().toString())
            .put("type", type)
            .put("slotId", banner.slotId)
            .put("bannerId", banner.bannerId)
            .put("version", banner.version)
            .put("timestamp", clock.nowMillis() / 1000)
            .put("test", banner.isTest)
            .put("elementId", elementId)
            .put("userId", eventUserId())
            .put("application", identity.appCode()?.takeIf { it.isNotEmpty() })
        events.send(event) { delivered ->
            if (!delivered) PWLog.warn(TAG, "Inline '$type' event of banner '${banner.bannerId}' is not delivered yet")
            onDelivered?.invoke(delivered)
        }
    }

    /** The banners on screen belong to the previous user until the server confirms a user change. */
    private fun eventUserId(): String =
        store.previousUserId(identity.owner()) ?: identity.userId()?.takeIf { it.isNotEmpty() } ?: identity.hwid()

    // endregion

    // region Network

    private fun send(slots: List<String>, callback: PushwooshInlineRequestCallback?) {
        if (isUserChangeUnconfirmed()) {
            held += slots
            callback?.onResult(error(PushwooshInlineException.OWNER_CHANGED, "The user change is not confirmed yet"))
            return
        }
        val owner = identity.owner()
        if (owner == null) {
            PWLog.warn(TAG, "Inline slots are requested once the device and application are known")
            callback?.onResult(error(PushwooshInlineException.OWNER_CHANGED, "The device or application is not known yet"))
            return
        }
        requested.add(slots, owner)
        transport.fetch(slots) { result -> finish(slots, owner, result, callback) }
    }

    private fun finish(slots: List<String>, owner: String?, result: InlineFetchResult, callback: PushwooshInlineRequestCallback?) {
        when (result) {
            is InlineFetchResult.Failure -> {
                PWLog.warn(TAG, "Inline slots request failed, slots keep their banners: ${result.error.message}")
                requested.remove(slots, owner)
                callback?.onResult(result.error)
            }
            is InlineFetchResult.Success -> {
                val current = identity.owner()
                val answerOwner = ownerOf(result.userId, result.appCode) ?: owner
                if (owner != current || answerOwner != current || !store.apply(slots, result.slots, current)) {
                    PWLog.warn(TAG, "Inline slots answer is for the previous user or application and is dropped")
                    requested.remove(slots, owner)
                    callback?.onResult(error(PushwooshInlineException.OWNER_CHANGED, "User or application changed during the inline slots request"))
                    return
                }
                callback?.onResult(null)
            }
        }
    }

    // endregion

    /**
     * Loads the store on first use. A store loaded before the owner was known takes the first known
     * owner as its own, like a load would: no unconfirmed user change starts from an unknown owner.
     */
    private fun ensureLoaded() {
        if (!store.isLoaded) {
            store.load(identity.owner())
            return
        }
        if (!store.hasCurrentOwner && identity.owner() != null) syncOwner()
    }

    private fun syncOwner() {
        val owner = identity.owner()
        store.setCurrentOwner(owner)
        store.syncOwner(owner)
    }

    /** The owner of an answer sent for [userId] and [appCode]. */
    private fun ownerOf(userId: String?, appCode: String?): String? = inlineOwner(userId, identity.hwid(), appCode)

    private fun error(code: Int, message: String) = PushwooshInlineException(code, message)

    /** Slots requested in this session for one owner; another owner starts over. */
    private class RequestedSlots {
        private var owner: String? = null
        private val slots = HashSet<String>()

        fun contains(slotId: String, owner: String?): Boolean = owner == this.owner && slotId in slots

        fun add(slotIds: List<String>, owner: String?) {
            if (owner != this.owner) {
                this.owner = owner
                slots.clear()
            }
            slots += slotIds
        }

        fun remove(slotIds: List<String>, owner: String?) {
            if (owner == this.owner) slots -= slotIds.toSet()
        }

        fun clear() = slots.clear()
    }

    private companion object {
        const val TAG = "InlineManager"
        const val MAX_SLOTS_PER_REQUEST = 10
        const val SESSION_GAP_MS = 30L * 60 * 1000
        const val NEW_SLOT_DELAY_MS = 500L
    }
}
