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
    fun collectsSheetImageUrlDroppingNullAndBlank() {
        fun sheet(imageUrl: String?) = InAppLayout.Sheet(
            SheetContent(Color.WHITE, null, null, imageUrl, false, emptyList(), dimsBackground = true)
        )

        assertEquals(listOf("https://a/s.png"), msg(sheet("https://a/s.png")).imageURLs())
        assertEquals(emptyList<String>(), msg(sheet(null)).imageURLs())
        assertEquals("a blank url is an absence, not a load", emptyList<String>(), msg(sheet("")).imageURLs())
    }

    // The fallback is not a prefetch nicety: it is needed exactly when the video fails, and
    // downloading it at that moment is too late.
    @Test
    fun collectsVideoPosterAndFallbackDroppingBlank() {
        fun video(poster: String?, fallback: String?) = InAppLayout.Video(
            VideoContent(
                "https://a/v.mp4", poster, fallback, null, null, emptyList(),
                loops = true, muted = true, showCloseButton = true
            )
        )

        assertEquals(
            listOf("https://a/p.jpg", "https://a/f.jpg"),
            msg(video("https://a/p.jpg", "https://a/f.jpg")).imageURLs()
        )
        assertEquals(listOf("https://a/f.jpg"), msg(video(null, "https://a/f.jpg")).imageURLs())
        assertEquals("the video url itself is not an image", emptyList<String>(), msg(video(null, "")).imageURLs())
    }

    @Test
    fun returnsEmptyWhenNoImage() {
        val modal = InAppLayout.Modal(
            ModalContent(Color.WHITE, null, null, null, false, emptyList(), dimsBackground = true)
        )
        assertEquals(emptyList<String>(), msg(modal).imageURLs())
    }
}
