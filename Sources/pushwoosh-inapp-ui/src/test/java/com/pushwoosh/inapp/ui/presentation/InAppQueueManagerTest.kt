package com.pushwoosh.inapp.ui.presentation

import android.graphics.Color
import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.ModalContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppQueueManagerTest {

    private val presented = mutableListOf<InAppMessage>()
    private var delegate: InAppMessageDelegate? = null
    private var isForeground = true
    private lateinit var manager: InAppQueueManager

    @Before
    fun setup() {
        presented.clear()
        delegate = null
        isForeground = true
        // Real store with opt-in disabled → caps never interfere; isolates queue logic.
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        // Inline main-marshaller so unpausing advances synchronously in the test.
        manager = InAppQueueManager(store, { isForeground }, { it.run() }) { delegate }
        manager.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message)
            }
        }
    }

    private fun msg(id: String?) =
        InAppMessage(id, InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true)), null, null, null, "{}")

    private fun bannerMsg(id: String?) =
        InAppMessage(
            id,
            InAppLayout.Banner(BannerContent(BannerPosition.BOTTOM, null, null, null, Color.BLACK, InAppAction.Close, 0L, true)),
            null,
            null,
            null,
            "{}"
        )

    /// Verifies the first message shows immediately and the second waits for dismissal.
    @Test
    fun showsFirstImmediatelyQueuesSecond() {
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(1, presented.size)
        assertEquals("a", presented[0].id)

        manager.onDismissed("a")
        assertEquals(2, presented.size)
        assertEquals("b", presented[1].id)
    }

    /// Verifies a duplicate id is collapsed while the first is queued/showing.
    @Test
    fun dedupesById() {
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("a"))
        assertEquals(1, presented.size)
    }

    /// Verifies a paused queue holds messages and drains on resume.
    @Test
    fun pauseHoldsQueueUntilResume() {
        manager.isPaused = true
        manager.enqueueForeground(msg("a"))
        assertEquals(0, presented.size)

        manager.isPaused = false
        assertEquals(1, presented.size)
    }

    /// Verifies advance() never launches while backgrounded (a background startActivity is
    /// dropped silently on API 29+) and the queued message drains on the next foreground.
    @Test
    fun backgroundDefersQueueUntilForeground() {
        isForeground = false
        manager.enqueueForeground(msg("a"))
        assertEquals(0, presented.size)

        isForeground = true
        manager.onAppForegrounded()
        assertEquals(1, presented.size)
        assertEquals("a", presented[0].id)
    }

    /// Verifies the confirmation handshake: a launch the OS dropped silently (never reached
    /// onCreate, so never confirmed) is re-presented on the next foreground.
    @Test
    fun unconfirmedLaunchRepresentedOnForeground() {
        manager.enqueueForeground(msg("a"))
        manager.onPresentConfirmed()
        manager.enqueueForeground(msg("b"))
        // The foreground flag lags a real background transition by the detector's
        // debounce, so this dismissal presents "b" even though the app is backgrounded.
        manager.onDismissed("a")
        assertEquals(listOf("a", "b"), presented.map { it.id })

        manager.onAppForegrounded()
        assertEquals(listOf("a", "b", "b"), presented.map { it.id })
    }

    /// Verifies a confirmed (visible) launch is left alone on foreground transitions.
    @Test
    fun confirmedLaunchNotRepresentedOnForeground() {
        manager.enqueueForeground(msg("a"))
        manager.onPresentConfirmed()

        manager.onAppForegrounded()
        assertEquals(1, presented.size)
    }

    /// Verifies a stale dismissal (mismatched id, e.g. from a duplicate overlay) does not
    /// advance the queue past the live message.
    @Test
    fun mismatchedDismissalIgnored() {
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        manager.onDismissed("a")
        assertEquals(listOf("a", "b"), presented.map { it.id })

        manager.onDismissed("a")
        assertEquals(2, presented.size)

        manager.onDismissed("b")
        manager.enqueueForeground(msg("c"))
        assertEquals(listOf("a", "b", "c"), presented.map { it.id })
    }

    /// Verifies a null-id dismissal (parse/launch failure paths) still releases the slot.
    @Test
    fun nullIdDismissalReleasesSlot() {
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        manager.onDismissed(null)
        assertEquals(listOf("a", "b"), presented.map { it.id })
    }

    /// Verifies the delegate can suppress a message before it is presented.
    @Test
    fun delegateCanSuppress() {
        delegate = object : InAppMessageDelegate {
            override fun shouldDisplay(messageId: String?) = false
        }
        manager.enqueueForeground(msg("a"))
        assertEquals(0, presented.size)
    }

    /// One slot across templates: a banner holds the slot, so a modal queued behind it waits until
    /// the banner is dismissed (and vice versa).
    @Test
    fun oneAtATimeAcrossTemplates() {
        manager.enqueueForeground(bannerMsg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("a"), presented.map { it.id })

        manager.onDismissed("a")
        assertEquals(listOf("a", "b"), presented.map { it.id })
    }

    /// Dedup spans templates: a banner already showing with id "x" collapses a modal enqueued with
    /// the same id.
    @Test
    fun dedupesAcrossTemplates() {
        manager.enqueueForeground(bannerMsg("x"))
        manager.enqueueForeground(msg("x"))
        assertEquals(1, presented.size)
    }

    /// Pause holds an overlay (banner) message in the queue and drains it on resume — the old
    /// overlay path dropped it.
    @Test
    fun pauseHoldsOverlayMessageUntilResume() {
        manager.isPaused = true
        manager.enqueueForeground(bannerMsg("a"))
        assertEquals(0, presented.size)

        manager.isPaused = false
        assertEquals(listOf("a"), presented.map { it.id })
    }

    /// A dropped launch (an overlay present that threw, or a failed Activity launch) reports
    /// onDismissed without a prior onPresentConfirmed: the slot is released with no didClose, so
    /// the next message shows. (The transient no-Activity path is retryable — onPresentDeferred —
    /// not this drop.)
    @Test
    fun overlayChannelFailReleasesSlotWithoutDidClose() {
        val closed = mutableListOf<String?>()
        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closed.add(messageId)
            }
        }
        manager.enqueueForeground(bannerMsg("a"))
        manager.onDismissed("a")
        assertEquals(emptyList<String?>(), closed)

        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("a", "b"), presented.map { it.id })
    }

    /// A transient "no host Activity yet" (overlay reports onPresentDeferred) is retryable, not a
    /// drop: the burst is neither drained against the missing Activity nor lost (the resumed
    /// Activity attaches a tick later and the queue must still hold it), and the pump is neither
    /// recursed nor re-posted — the retry waits for the resume signal. The whole burst attaches
    /// in FIFO order once an Activity resumes.
    @Test
    fun overlayNoActivityHoldsBurstAndRetriesInOrder() {
        val dispatched = ArrayDeque<Runnable>()
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        var hostAvailable = false
        val confirmed = mutableListOf<String?>()
        lateinit var mgr: InAppQueueManager
        mgr = InAppQueueManager(store, { true }, { dispatched.add(it) }) { null }
        mgr.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message)
                if (hostAvailable) {
                    mgr.onPresentConfirmed()
                    confirmed.add(message.id)
                } else {
                    mgr.onPresentDeferred(message)
                }
            }
        }

        mgr.enqueueForeground(bannerMsg("a"))
        mgr.enqueueForeground(bannerMsg("b"))
        mgr.enqueueForeground(bannerMsg("c"))

        // No recursion, no drain, no self-post: only the head was ever attempted (each enqueue
        // re-tries it), "b"/"c" wait in the queue, and nothing was posted back to the looper.
        assertTrue(presented.all { it.id == "a" })
        assertFalse(presented.any { it.id == "b" || it.id == "c" })
        assertTrue(dispatched.isEmpty())

        // An Activity resumes; the resume signal starts the drain, then dismissing each confirmed
        // show advances synchronously to the next. The whole burst attaches in FIFO order.
        hostAvailable = true
        mgr.onActivityBroughtOnTop()
        val dismissed = mutableListOf<String?>()
        var guard = 0
        while (confirmed.size > dismissed.size && guard++ < 100) {
            val id = confirmed[dismissed.size]
            mgr.onDismissed(id)
            dismissed.add(id)
        }
        assertEquals(listOf("a", "b", "c"), confirmed)
    }

    /// MUST FIX 3 regression: a deferred present must not re-post the pump. "Foregrounded but
    /// topActivity == null" holds the whole time an Activity sits paused (permission dialog on
    /// top, PiP — seconds to minutes), so an immediate re-post spins advance→present→defer at
    /// looper speed, polling the integrator's shouldDisplay and the frequency store. The retry
    /// is the next Activity's resume signal (onActivityBroughtOnTop), not a self-post.
    @Test
    fun deferredPresentDoesNotRepostPump() {
        val dispatched = mutableListOf<Runnable>()
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        lateinit var mgr: InAppQueueManager
        mgr = InAppQueueManager(store, { true }, { dispatched.add(it) }) { null }
        mgr.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message)
                mgr.onPresentDeferred(message)
            }
        }

        mgr.enqueueForeground(bannerMsg("a"))

        assertEquals(listOf("a"), presented.map { it.id })
        assertEquals(0, dispatched.size)
    }

    /// A frequency cap is charged only when the launch is confirmed, not when the slot is claimed:
    /// a launch that does not confirm (here a dropped present that reports onDismissed) releases
    /// the slot without recording a display, so a maxDisplays=1 message is not burned and stays
    /// eligible. It records only on the retry that actually reaches the screen and confirms.
    @Test
    fun frequencyCapNotConsumedUntilShown() {
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication()).apply { enabled = true }
        var attachFails = true
        lateinit var mgr: InAppQueueManager
        mgr = InAppQueueManager(store, { isForeground }, { it.run() }) { delegate }
        mgr.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message)
                if (attachFails) mgr.onDismissed(message.id) else mgr.onPresentConfirmed()
            }
        }
        val capped = InAppMessage(
            "a",
            InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true)),
            1,
            null,
            null,
            "{}"
        )

        // Attach fails (no host Activity yet): the slot is released without a confirm.
        mgr.enqueueForeground(capped)
        assertEquals(1, presented.size)
        // Cap not consumed — the message is still eligible when re-triggered.
        assertTrue(store.canShow(capped))

        // Re-triggered once a host Activity exists: it shows, confirms, and now the cap is charged.
        attachFails = false
        mgr.enqueueForeground(capped)
        assertEquals(2, presented.size)
        assertFalse(store.canShow(capped))
    }

    /// The delegate veto is asked at show time (inside advance), not at enqueue: a message vetoed
    /// when advance runs is dropped and the next one shows.
    @Test
    fun vetoAskedAtShowTimeDropsAndAdvances() {
        delegate = object : InAppMessageDelegate {
            override fun shouldDisplay(messageId: String?) = messageId != "a"
        }
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("b"), presented.map { it.id })
    }

    /// A veto lifted between enqueue and show lets the message through (asked at show, not enqueue).
    @Test
    fun vetoLiftedBeforeShowAllowsPresent() {
        var allow = false
        delegate = object : InAppMessageDelegate {
            override fun shouldDisplay(messageId: String?) = allow
        }
        isForeground = false
        manager.enqueueForeground(msg("a"))
        assertEquals(0, presented.size)

        allow = true
        isForeground = true
        manager.onAppForegrounded()
        assertEquals(listOf("a"), presented.map { it.id })
    }

    /// The delegate is asked exactly once per message (at show time only).
    @Test
    fun vetoAskedExactlyOncePerMessage() {
        var asks = 0
        delegate = object : InAppMessageDelegate {
            override fun shouldDisplay(messageId: String?): Boolean {
                asks++
                return true
            }
        }
        manager.enqueueForeground(msg("a"))
        assertEquals(1, asks)
        assertEquals(listOf("a"), presented.map { it.id })
    }

    /// A confirmed (shown) message reports didClose exactly once on dismissal.
    @Test
    fun confirmedDismissReportsDidClose() {
        val closed = mutableListOf<String?>()
        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closed.add(messageId)
            }
        }
        manager.enqueueForeground(msg("a"))
        manager.onPresentConfirmed()
        manager.onDismissed("a")
        assertEquals(listOf<String?>("a"), closed)
    }

    /// An unconfirmed launch (never reached the screen — dropped by the OS or a launch failure)
    /// reports no didClose on dismissal; the slot is released silently.
    @Test
    fun unconfirmedDismissSuppressesDidClose() {
        val closed = mutableListOf<String?>()
        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closed.add(messageId)
            }
        }
        manager.enqueueForeground(msg("a"))
        manager.onDismissed("a")
        assertEquals(emptyList<String?>(), closed)
    }

    /// After a confirmed message closes and the queue drains, a later stale dismissal (e.g. a
    /// duplicate overlay finishing) must not read a sticky showingConfirmed and re-fire didClose.
    @Test
    fun staleDismissalAfterDrainDoesNotRefireDidClose() {
        val closed = mutableListOf<String?>()
        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closed.add(messageId)
            }
        }
        manager.enqueueForeground(msg("a"))
        manager.onPresentConfirmed()
        manager.onDismissed("a")
        assertEquals(listOf<String?>("a"), closed)

        manager.onDismissed(null)
        assertEquals(listOf<String?>("a"), closed)
    }

    /// The isPaused write is synchronous on the caller's thread: it is visible immediately, even
    /// when the injected main-marshaller has not run anything yet.
    @Test
    fun pauseWriteIsSynchronousNotDeferredToMain() {
        val neverRuns = mutableListOf<Runnable>()
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        val m = InAppQueueManager(store, { true }, { neverRuns.add(it) }) { null }

        m.isPaused = true

        assertTrue(m.isPaused)
        assertEquals(0, neverRuns.size)
    }

    /// Unpausing dispatches exactly one advance() through the injected main-marshaller (not inline),
    /// and running it drains the held message.
    @Test
    fun unpauseDispatchesAdvanceToMainExecutor() {
        val dispatched = mutableListOf<Runnable>()
        val store = InAppFrequencyStore(RuntimeEnvironment.getApplication())
        val presentedHere = mutableListOf<InAppMessage>()
        val m = InAppQueueManager(store, { true }, { dispatched.add(it) }) { null }
        m.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presentedHere.add(message)
            }
        }

        m.isPaused = true
        m.enqueueForeground(msg("a"))
        assertEquals(0, presentedHere.size)

        m.isPaused = false
        assertEquals(1, dispatched.size)

        dispatched.forEach { it.run() }
        assertEquals(listOf("a"), presentedHere.map { it.id })
    }
}
