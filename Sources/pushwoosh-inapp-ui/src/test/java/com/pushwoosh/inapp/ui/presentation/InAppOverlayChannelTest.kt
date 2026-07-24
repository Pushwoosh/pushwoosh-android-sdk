package com.pushwoosh.inapp.ui.presentation

import android.content.Context
import android.graphics.Color
import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.ModalContent
import com.pushwoosh.internal.platform.AndroidPlatformModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InAppOverlayChannelTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var platformModule: MockedStatic<AndroidPlatformModule>
    private val shown = mutableListOf<String?>()
    private val closed = mutableListOf<String?>()
    private var showResult = true
    private var delegate: InAppMessageDelegate? = null
    private lateinit var manager: InAppQueueManager

    @Before
    fun setup() {
        shown.clear()
        closed.clear()
        showResult = true
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")

        platformModule = Mockito.mockStatic(AndroidPlatformModule::class.java)
        platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)
        platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)

        delegate = object : InAppMessageDelegate {
            override fun didClose(messageId: String?) {
                closed.add(messageId)
            }
        }
        InAppModule.delegate = delegate

        manager = InAppModule.queueManager(context)
        manager.channel = InAppOverlayChannel(manager, show = { m, _ -> shown.add(m.id); showResult })
    }

    @After
    fun tearDown() {
        platformModule.close()
        InAppModule.delegate = null
        delegate = null
    }

    private fun resetStatic(owner: Any, field: String) {
        owner.javaClass.getDeclaredField(field).apply {
            isAccessible = true
            set(owner, null)
        }
    }

    private fun msg(id: String?) =
        InAppMessage(id, InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true)), null, null, null, "{}")

    /// A successful attach confirms the show, so the slot is held: a second message waits until
    /// the first is dismissed, and a confirmed dismissal reports didClose exactly once.
    @Test
    fun attachSuccessConfirmsAndHoldsSlot() {
        showResult = true
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("a"), shown)

        manager.onDismissed("a")
        assertEquals(listOf("a", "b"), shown)
        assertEquals(listOf<String?>("a"), closed)
    }

    /// A failed attach (no host Activity yet) is a transient, retryable state, not a drop: the
    /// message is not closed (no didClose) and is held for the next Activity's resume signal.
    /// Once an Activity resumes, the signal re-pumps the queue and the held message attaches —
    /// nothing is lost.
    @Test
    fun attachNoActivityDefersAndRetries() {
        showResult = false
        manager.enqueueForeground(msg("a"))
        // Attempted once, held for retry — not confirmed, not closed, not dropped.
        assertEquals(listOf("a"), shown)
        assertEquals(emptyList<String?>(), closed)

        // An Activity resumes: the resume signal retries the held message.
        showResult = true
        manager.onActivityBroughtOnTop()
        assertEquals(listOf("a", "a"), shown)
        assertEquals(emptyList<String?>(), closed)
    }

    /// A show() that throws (e.g. an integrator delegate callback or a view constructor) must not
    /// wedge the shared slot: the channel catches it, releases the slot silently (no didClose),
    /// and the next message shows.
    @Test
    fun attachThrowReleasesSlotSilently() {
        var first = true
        manager.channel = InAppOverlayChannel(manager, show = { m, _ ->
            if (first) {
                first = false
                throw RuntimeException("delegate blew up")
            }
            shown.add(m.id)
            true
        })
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("b"), shown)
        assertEquals(emptyList<String?>(), closed)
    }

    /// S3 regression: an Error (a TODO()/assert in show(), not an Exception) must release the slot
    /// too. BackgroundExecutor.main swallows the Error without a crash, so a catch narrowed to
    /// Exception would leave `showing` set forever and wedge every later in-app. The catch is
    /// widened to Throwable: the slot is released silently (no didClose) and the next message shows.
    @Test
    fun attachErrorReleasesSlotSilently() {
        var first = true
        manager.channel = InAppOverlayChannel(manager, show = { m, _ ->
            if (first) {
                first = false
                throw NotImplementedError("show() not implemented")
            }
            shown.add(m.id)
            true
        })
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf("b"), shown)
        assertEquals(emptyList<String?>(), closed)
    }
}
