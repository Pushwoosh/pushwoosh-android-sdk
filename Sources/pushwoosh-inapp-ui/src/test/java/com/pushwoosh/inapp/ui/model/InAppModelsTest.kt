package com.pushwoosh.inapp.ui.model

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppModelsTest {

    private fun msg(layout: InAppLayout) = InAppMessage(
        id = null,
        layout = layout,
        maxDisplays = null,
        cooldownSec = null,
        expireEpochSec = null,
        rawJson = "{}"
    )

    @Test
    fun collectsAllCarouselSlidesIncludingOffScreen() {
        val layout = InAppLayout.Carousel(
            CarouselContent(
                items = listOf(
                    CarouselItem("https://a/1.png", null, null, null),
                    CarouselItem("https://a/2.png", null, null, null),
                    CarouselItem("https://a/3.png", null, null, null)
                ),
                showCloseButton = false
            )
        )
        assertEquals(listOf("https://a/1.png", "https://a/2.png", "https://a/3.png"), msg(layout).imageURLs())
    }

    @Test
    fun collectsAllStoriesSlidesDroppingNullAndBlank() {
        val layout = InAppLayout.Stories(
            StoriesContent(
                items = listOf(
                    StoryItem("https://a/1.png", null, null, emptyList(), 5_000L),
                    StoryItem(null, null, null, emptyList(), 5_000L),
                    StoryItem("", null, null, emptyList(), 5_000L)
                ),
                loops = false,
                showCloseButton = false
            )
        )
        assertEquals(listOf("https://a/1.png"), msg(layout).imageURLs())
    }

    @Test
    fun collectsSingleUrlLayouts() {
        val modal = InAppLayout.Modal(
            ModalContent(Color.WHITE, null, null, "https://a/m.png", false, emptyList(), dimsBackground = true)
        )
        val banner = InAppLayout.Banner(
            BannerContent(BannerPosition.TOP, "https://a/b.png", null, null, Color.BLACK, InAppAction.Close, 0L, false)
        )
        val fullscreen = InAppLayout.Fullscreen(
            FullscreenContent("https://a/f.png", Color.BLACK, null, null, emptyList(), false)
        )
        assertEquals(listOf("https://a/m.png"), msg(modal).imageURLs())
        assertEquals(listOf("https://a/b.png"), msg(banner).imageURLs())
        assertEquals(listOf("https://a/f.png"), msg(fullscreen).imageURLs())
    }

    @Test
    fun returnsEmptyWhenNoImage() {
        val modal = InAppLayout.Modal(
            ModalContent(Color.WHITE, null, null, null, false, emptyList(), dimsBackground = true)
        )
        assertEquals(emptyList<String>(), msg(modal).imageURLs())
    }
}
