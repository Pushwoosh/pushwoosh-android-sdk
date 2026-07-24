package com.pushwoosh.inapp.ui

import android.content.Context
import android.graphics.Color
import android.os.Looper
import com.pushwoosh.inapp.event.ActivityBroughtOnTopEvent
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.ModalContent
import com.pushwoosh.inapp.ui.presentation.InAppPresentationChannel
import com.pushwoosh.internal.event.EventBus
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
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InAppUiPluginTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var platformModule: MockedStatic<AndroidPlatformModule>

    @Before
    fun setup() {
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        platformModule = Mockito.mockStatic(AndroidPlatformModule::class.java)
        platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)
        platformModule.`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }.thenReturn(true)
    }

    @After
    fun tearDown() {
        platformModule.close()
    }

    private fun resetStatic(owner: Any, field: String) {
        owner.javaClass.getDeclaredField(field).apply {
            isAccessible = true
            set(owner, null)
        }
    }

    private fun msg(id: String?) =
        InAppMessage(id, InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), true)), null, null, null, "{}")

    /// The plugin's ActivityBroughtOnTopEvent subscription is the retry signal for a present
    /// deferred while topActivity was null: posting the event re-pumps the queue and the held
    /// message attaches. Without this wiring a deferred overlay waits for the next full
    /// background→foreground transition.
    @Test
    fun activityResumeEventRetriesDeferredPresent() {
        InAppUiPlugin().init()
        val manager = InAppModule.queueManager(context)
        val presented = mutableListOf<String?>()
        var hostAvailable = false
        manager.channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message.id)
                if (hostAvailable) manager.onPresentConfirmed() else manager.onPresentDeferred(message)
            }
        }
        manager.enqueueForeground(msg("a"))
        assertEquals(listOf("a"), presented)

        hostAvailable = true
        EventBus.sendEvent(ActivityBroughtOnTopEvent.getInstance())
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("a", "a"), presented)
    }
}
