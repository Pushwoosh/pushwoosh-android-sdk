package com.pushwoosh.inapp.ui

import android.content.Context
import android.net.Uri
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.presentation.InAppPresentationChannel
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.BackgroundExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        PushwooshInAppUi.present(mapOf(
            "displayType" to "modal",
            "inAppId" to "m1",
            "modal" to mapOf(
                "showClose" to true,
                "dimBackground" to true,
                "background" to "#FFFFFFFF",
                "buttons" to emptyList<Any>()
            )
        ))

        assertEquals(listOf("m1"), presented.map { it.id })
    }

    /// A banner now enters the FIFO queue like every other template (no overlay bypass) and is
    /// handed to the channel.
    @Test
    fun presentBannerEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "banner",
            "inAppId" to "b1",
            "banner" to mapOf(
                "showClose" to true,
                "position" to "top",
                "background" to "#4B5057FF",
                "action" to mapOf("type" to "close")
            )
        ))

        assertEquals(listOf("b1"), presented.map { it.id })
    }

    /// A floating modal (dimBackground=false) — previously an overlay bypass — now enters the queue.
    @Test
    fun presentFloatingModalEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "modal",
            "inAppId" to "fm1",
            "modal" to mapOf(
                "showClose" to true,
                "dimBackground" to false,
                "background" to "#FFFFFFFF",
                "buttons" to emptyList<Any>()
            )
        ))

        assertEquals(listOf("fm1"), presented.map { it.id })
    }

    /// Fullscreen routes through the queue.
    @Test
    fun presentFullscreenEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "fullscreen",
            "inAppId" to "fs1",
            "fullscreen" to mapOf(
                "showClose" to true,
                "cover" to mapOf("background" to "#1A1A1EFF"),
                "buttons" to emptyList<Any>()
            )
        ))

        assertEquals(listOf("fs1"), presented.map { it.id })
    }

    /// Carousel routes through the queue.
    @Test
    fun presentCarouselEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "carousel",
            "inAppId" to "c1",
            "carousel" to mapOf(
                "showClose" to true,
                "items" to listOf(mapOf("image" to "https://x/1.png"))
            )
        ))

        assertEquals(listOf("c1"), presented.map { it.id })
    }

    /// route() bails out silently when the application-context global is null (init not
    /// finished, or a dying process): nothing is queued, nothing crashes.
    @Test
    fun presentNoOpWhenAppContextIsNull() {
        platformModule.`when`<Context> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)

        PushwooshInAppUi.present(mapOf(
            "displayType" to "banner",
            "inAppId" to "b1",
            "banner" to mapOf(
                "showClose" to true,
                "position" to "top",
                "background" to "#4B5057FF",
                "action" to mapOf("type" to "close")
            )
        ))

        assertEquals(emptyList<InAppMessage>(), presented)
    }

    /// Stories routes through the queue.
    @Test
    fun presentStoriesEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "stories",
            "inAppId" to "s1",
            "stories" to mapOf(
                "showClose" to true,
                "loop" to false,
                "items" to listOf(mapOf("duration" to 5, "buttons" to emptyList<Any>()))
            )
        ))

        assertEquals(listOf("s1"), presented.map { it.id })
    }

    /// A floating sheet (dimBackground=false) takes the overlay channel, but like the floating
    /// modal it still goes through the FIFO queue rather than around it.
    @Test
    fun presentSheetEntersQueue() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "sheet",
            "inAppId" to "sh1",
            "sheet" to mapOf(
                "showClose" to true,
                "dimBackground" to false,
                "background" to "#FFFFFFFF",
                "buttons" to emptyList<Any>()
            )
        ))

        assertEquals(listOf("sh1"), presented.map { it.id })
    }

    /// Nested maps and lists survive the map -> JSON conversion in order and with their content:
    /// list of slides, per-slide nested styled text.
    @Test
    fun presentUnwrapsNestedMapsAndLists() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "carousel",
            "inAppId" to "nested",
            "carousel" to mapOf(
                "showClose" to true,
                "items" to listOf(
                    mapOf(
                        "image" to "https://x/1.png",
                        "title" to mapOf("text" to "First", "color" to "#FFFFFFFF")
                    ),
                    mapOf("image" to "https://x/2.png")
                )
            )
        ))

        val content = (presented.single().layout as InAppLayout.Carousel).content
        assertEquals(listOf("https://x/1.png", "https://x/2.png"), content.items.map { it.imageUrl })
        assertEquals("First", content.items[0].title?.text)
    }

    /// An empty map is a config without displayType: no-op, no exception.
    @Test
    fun presentEmptyMapIsNoOp() {
        PushwooshInAppUi.present(emptyMap())

        assertEquals(emptyList<InAppMessage>(), presented)
    }

    /// A non-String key makes the map non-convertible (JSONObject casts keys to String): the
    /// public API swallows it and no-ops instead of throwing into the host.
    @Test
    @Suppress("UNCHECKED_CAST")
    fun presentNonStringKeyIsNoOp() {
        val broken = mapOf<Any, Any?>(1 to "x") as Map<String, Any?>

        PushwooshInAppUi.present(broken)

        assertEquals(emptyList<InAppMessage>(), presented)
    }

    /// Contract strictness is unchanged by the map entry point: a string where the contract
    /// wants a boolean is still rejected wholesale.
    @Test
    fun presentRejectsCoercedBoolean() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "modal",
            "inAppId" to "coerce",
            "modal" to mapOf(
                "showClose" to "true",
                "dimBackground" to true,
                "background" to "#FFFFFFFF",
                "buttons" to emptyList<Any>()
            )
        ))

        assertEquals(emptyList<InAppMessage>(), presented)
    }

    /// A Double reaches the parser as a number, not a string: 2.5s -> 2500ms.
    @Test
    fun presentAcceptsDoubleAutoDismiss() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "banner",
            "inAppId" to "num-double",
            "banner" to mapOf(
                "showClose" to true,
                "position" to "top",
                "background" to "#4B5057FF",
                "action" to mapOf("type" to "close"),
                "autoDismiss" to 2.5
            )
        ))

        val content = (presented.single().layout as InAppLayout.Banner).content
        assertEquals(2500L, content.autoDismissMs)
    }

    /// A value the map allows but JSON cannot carry is silently nulled by JSONObject.wrap, not
    /// raised — unlike a top-level non-String key, which throws out of the constructor. At a
    /// required position the config is left incomplete, so nothing is shown and nothing escapes
    /// into the host app.
    @Test
    fun presentNoOpWhenRequiredValueIsNotJsonConvertible() {
        val cases = listOf(
            "non-String key inside the display block" to mapOf(
                "displayType" to "banner",
                "inAppId" to "u1",
                "banner" to mapOf<Any?, Any?>(1 to "x")
            ),
            "Uri where the contract wants a url string" to mapOf(
                "displayType" to "banner",
                "inAppId" to "u2",
                "banner" to mapOf(
                    "showClose" to true,
                    "position" to "top",
                    "background" to "#4B5057FF",
                    "action" to mapOf("type" to "url", "url" to Uri.parse("https://pushwoosh.com"))
                )
            ),
            "null display block" to mapOf(
                "displayType" to "banner",
                "inAppId" to "u3",
                "banner" to null
            )
        )

        for ((name, config) in cases) {
            presented.clear()

            PushwooshInAppUi.present(config)

            assertEquals("case $name", emptyList<InAppMessage>(), presented)
        }
    }

    /// The same unconvertible value at an *optional* field is not fail-closed: wrap() nulls it and
    /// the parser reads a JSON null as "absent", so the banner shows minus that field. Only the map
    /// entry point can produce this input — JSON text cannot express a dropped value, so the
    /// parser's fail-closed reading of a present-but-malformed field never engages here.
    @Test
    fun presentDropsUnconvertibleOptionalValue() {
        // Distinct ids plus an explicit onDismissed: the fake channel never dismisses, so without
        // both the second case would sit in the queue behind the first instead of being handed over.
        val cases = listOf<Triple<String, String, Any?>>(
            Triple("Uri instead of an image url string", "opt-uri", Uri.parse("https://x/1.png")),
            Triple("explicit null allowed by the Map<String, Any?> signature", "opt-null", null)
        )

        for ((name, id, image) in cases) {
            presented.clear()

            PushwooshInAppUi.present(mapOf(
                "displayType" to "banner",
                "inAppId" to id,
                "banner" to mapOf(
                    "showClose" to true,
                    "position" to "top",
                    "background" to "#4B5057FF",
                    "action" to mapOf("type" to "close"),
                    "image" to image
                )
            ))

            assertEquals("case $name is shown", listOf(id), presented.map { it.id })
            val content = (presented.single().layout as InAppLayout.Banner).content
            assertNull("case $name drops the image", content.imageUrl)

            InAppModule.queueManager(context).onDismissed(id)
        }
    }

    /// An Int reaches the parser as a number too: 5s -> 5000ms.
    @Test
    fun presentAcceptsIntStoryDuration() {
        PushwooshInAppUi.present(mapOf(
            "displayType" to "stories",
            "inAppId" to "num-int",
            "stories" to mapOf(
                "showClose" to true,
                "loop" to false,
                "items" to listOf(mapOf("duration" to 5, "buttons" to emptyList<Any>()))
            )
        ))

        val item = (presented.single().layout as InAppLayout.Stories).content.items.single()
        assertEquals(5000L, item.durationMs)
    }

    /// A NaN/Infinity number is the one value JSON refuses at serialization time: AOSP
    /// JSONObject.toString() swallows the JSONException and returns null instead of throwing, so
    /// the map path must no-op rather than let a platform-type null check fire into the host app.
    @Test
    fun presentNoOpWhenNumberIsNotSerializable() {
        val cases = listOf("NaN" to Double.NaN, "Infinity" to Double.POSITIVE_INFINITY)

        for ((name, bad) in cases) {
            presented.clear()

            PushwooshInAppUi.present(mapOf(
                "displayType" to "banner",
                "inAppId" to "nan",
                "banner" to mapOf(
                    "showClose" to true,
                    "position" to "top",
                    "background" to "#4B5057FF",
                    "action" to mapOf("type" to "close"),
                    "autoDismiss" to bad
                )
            ))

            assertEquals("case $name", emptyList<InAppMessage>(), presented)
        }
    }
}
