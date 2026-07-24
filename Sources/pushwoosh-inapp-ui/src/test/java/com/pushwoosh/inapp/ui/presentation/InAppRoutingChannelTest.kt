package com.pushwoosh.inapp.ui.presentation

import android.graphics.Color
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.FullscreenContent
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.ModalContent
import com.pushwoosh.inapp.ui.model.StoriesContent
import com.pushwoosh.inapp.ui.model.StoryItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppRoutingChannelTest {

    private val activityIds = mutableListOf<String?>()
    private val overlayIds = mutableListOf<String?>()
    private lateinit var routing: InAppRoutingChannel

    @Before
    fun setup() {
        activityIds.clear()
        overlayIds.clear()
        val activityChannel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                activityIds.add(message.id)
            }
        }
        val overlayChannel = object : InAppPresentationChannel {
            override fun present(message: InAppMessage) {
                overlayIds.add(message.id)
            }
        }
        routing = InAppRoutingChannel(activityChannel, overlayChannel)
    }

    private fun message(id: String, layout: InAppLayout) = InAppMessage(id, layout, null, null, null, "{}")

    private fun modal(dim: Boolean) =
        InAppLayout.Modal(ModalContent(Color.WHITE, null, null, null, true, emptyList(), dim))

    private fun banner() =
        InAppLayout.Banner(BannerContent(BannerPosition.BOTTOM, null, null, null, Color.BLACK, InAppAction.Close, 0L, true))

    private fun fullscreen() =
        InAppLayout.Fullscreen(FullscreenContent(null, Color.BLACK, null, null, emptyList(), true))

    private fun carousel() =
        InAppLayout.Carousel(CarouselContent(listOf(CarouselItem(null, null, null, null)), true))

    private fun stories() =
        InAppLayout.Stories(StoriesContent(listOf(StoryItem(null, null, null, emptyList(), 5000L)), false, true))

    @Test
    fun bannerRoutesToOverlay() {
        routing.present(message("b", banner()))
        assertEquals(listOf("b"), overlayIds)
        assertEquals(emptyList<String?>(), activityIds)
    }

    @Test
    fun floatingModalRoutesToOverlay() {
        routing.present(message("fm", modal(dim = false)))
        assertEquals(listOf("fm"), overlayIds)
        assertEquals(emptyList<String?>(), activityIds)
    }

    @Test
    fun blockingModalRoutesToActivity() {
        routing.present(message("m", modal(dim = true)))
        assertEquals(listOf("m"), activityIds)
        assertEquals(emptyList<String?>(), overlayIds)
    }

    @Test
    fun fullscreenRoutesToActivity() {
        routing.present(message("fs", fullscreen()))
        assertEquals(listOf("fs"), activityIds)
    }

    @Test
    fun carouselRoutesToActivity() {
        routing.present(message("c", carousel()))
        assertEquals(listOf("c"), activityIds)
    }

    @Test
    fun storiesRoutesToActivity() {
        routing.present(message("s", stories()))
        assertEquals(listOf("s"), activityIds)
    }
}
