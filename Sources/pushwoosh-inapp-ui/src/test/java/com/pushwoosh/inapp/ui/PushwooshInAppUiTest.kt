package com.pushwoosh.inapp.ui

import android.content.Context
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.presentation.InAppPresentationChannel
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.BackgroundExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PushwooshInAppUiTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var platformModule: MockedStatic<AndroidPlatformModule>
    private lateinit var backgroundExecutor: MockedStatic<BackgroundExecutor>
    private val presented = mutableListOf<InAppMessage>()

    @Before
    fun setup() {
        presented.clear()
        // Module singletons are process-static; reset so tests don't leak.
        resetStatic(InAppModule, "queueManagerInstance")
        resetStatic(InAppModule, "frequencyStoreInstance")
        PushwooshInAppUi.delegate = null

        platformModule = Mockito.mockStatic(AndroidPlatformModule::class.java)
        platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)
        platformModule
            .`when`<Boolean> { AndroidPlatformModule.isApplicationInForeground() }
            .thenReturn(true)

        // Run every main-thread post inline so route() resolves deterministically.
        backgroundExecutor = Mockito.mockStatic(BackgroundExecutor::class.java)
        backgroundExecutor.`when`<Unit> { BackgroundExecutor.main(any()) }.thenAnswer {
            (it.getArgument(0) as Runnable).run()
            null
        }

        // Real queue manager, fake channel: a blocking enqueue is observable as a present().
        InAppModule.queueManager(context).channel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                presented.add(message)
            }
        }
    }

    @After
    fun tearDown() {
        backgroundExecutor.close()
        platformModule.close()
    }

    private fun resetStatic(owner: Any, field: String) {
        owner.javaClass.getDeclaredField(field).apply {
            isAccessible = true
            set(owner, null)
        }
    }

    /// A blocking modal (dimBackground defaults true) routed via present() enters the FIFO
    /// queue and is handed to the channel.
    @Test
    fun presentBlockingModalEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"modal","inAppId":"m1","modal":{"showClose":true,"dimBackground":true,"background":"#FFFFFFFF","buttons":[]}}""")

        assertEquals(listOf("m1"), presented.map { it.id })
    }

    /// A banner now enters the FIFO queue like every other template (no overlay bypass) and is
    /// handed to the channel.
    @Test
    fun presentBannerEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"banner","inAppId":"b1","banner":{"showClose":true,"position":"top","background":"#4B5057FF","action":{"type":"close"}}}""")

        assertEquals(listOf("b1"), presented.map { it.id })
    }

    /// A floating modal (dimBackground=false) — previously an overlay bypass — now enters the queue.
    @Test
    fun presentFloatingModalEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"modal","inAppId":"fm1","modal":{"showClose":true,"dimBackground":false,"background":"#FFFFFFFF","buttons":[]}}""")

        assertEquals(listOf("fm1"), presented.map { it.id })
    }

    /// Fullscreen routes through the queue.
    @Test
    fun presentFullscreenEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"fullscreen","inAppId":"fs1","fullscreen":{"showClose":true,"cover":{"background":"#1A1A1EFF"},"buttons":[]}}""")

        assertEquals(listOf("fs1"), presented.map { it.id })
    }

    /// Carousel routes through the queue.
    @Test
    fun presentCarouselEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"carousel","inAppId":"c1","carousel":{"showClose":true,"items":[{"image":"https://x/1.png"}]}}""")

        assertEquals(listOf("c1"), presented.map { it.id })
    }

    /// route() bails out silently when the application-context global is null (init not
    /// finished, or a dying process): nothing is queued, nothing crashes.
    @Test
    fun presentNoOpWhenAppContextIsNull() {
        platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)

        PushwooshInAppUi.present("""{"displayType":"banner","inAppId":"b1","banner":{"showClose":true,"position":"top","background":"#4B5057FF","action":{"type":"close"}}}""")

        assertEquals(emptyList<InAppMessage>(), presented)
    }

    /// Stories routes through the queue.
    @Test
    fun presentStoriesEntersQueue() {
        PushwooshInAppUi.present("""{"displayType":"stories","inAppId":"s1","stories":{"showClose":true,"loop":false,"items":[{"duration":5,"buttons":[]}]}}""")

        assertEquals(listOf("s1"), presented.map { it.id })
    }
}
