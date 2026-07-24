package com.pushwoosh.inapp.ui.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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
class InAppActivityChannelTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var platformModule: MockedStatic<AndroidPlatformModule>
    private val launched = mutableListOf<String?>()
    private val closed = mutableListOf<String?>()
    private var launchFailure: Throwable? = null
    private var delegate: InAppMessageDelegate? = null
    private lateinit var manager: InAppQueueManager

    /// startActivity stand-in: fails once with the queued [launchFailure], then records the
    /// launched message (its rawJson extra) like a healthy launch would.
    private val launchContext = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
        override fun startActivity(intent: Intent) {
            launchFailure?.let {
                launchFailure = null
                throw it
            }
            launched.add(intent.getStringExtra("pw_inapp_raw_json"))
        }
    }

    @Before
    fun setup() {
        launched.clear()
        closed.clear()
        launchFailure = null
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
        manager.channel = InAppActivityChannel(launchContext)
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

    private fun msg(id: String) =
        InAppMessage(id, InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true)), null, null, null, id)

    /// A failed startActivity never reaches the Activity's onDestroy, so the channel itself must
    /// release the queue slot via onDismissed or every later in-app stalls. The release is
    /// silent — no didClose for a show that never reached the screen — and the next message
    /// launches. Mirrors InAppOverlayChannelTest.attachThrowReleasesSlotSilently.
    @Test
    fun launchThrowReleasesSlotSilently() {
        launchFailure = RuntimeException("startActivity blew up")
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf<String?>("b"), launched)
        assertEquals(emptyList<String?>(), closed)
    }

    /// Same guarantee for an Error: the catch is Throwable, so narrowing it to Exception (an
    /// Error would then strand the slot forever) fails this test loud — the Error escapes the
    /// enqueue call. Mirrors InAppOverlayChannelTest.attachErrorReleasesSlotSilently.
    @Test
    fun launchErrorReleasesSlotSilently() {
        launchFailure = NotImplementedError("startActivity not implemented")
        manager.enqueueForeground(msg("a"))
        manager.enqueueForeground(msg("b"))
        assertEquals(listOf<String?>("b"), launched)
        assertEquals(emptyList<String?>(), closed)
    }
}
