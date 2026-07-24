package com.pushwoosh.inapp.ui.model

import androidx.annotation.ColorInt

/**
 * Typed native in-app message model. Mirrors the iOS `PWInAppLayout` enum: a layout
 * discriminator carrying a dedicated content type, so adding a template is one new
 * [InAppLayout] subclass plus one new view — no untyped map access leaks into the UI.
 *
 * Video and picture-in-picture templates from iOS are intentionally out of scope for
 * this iteration.
 */
sealed class InAppLayout {
    data class Modal(val content: ModalContent) : InAppLayout()
    data class Carousel(val content: CarouselContent) : InAppLayout()
    data class Stories(val content: StoriesContent) : InAppLayout()
    data class Banner(val content: BannerContent) : InAppLayout()
    data class Fullscreen(val content: FullscreenContent) : InAppLayout()
}

/**
 * A parsed native in-app: the layout, optional opt-in frequency caps, and the raw
 * config JSON. The raw JSON is retained so the presenting Activity can be handed the
 * message by value through an Intent extra (re-parsed on the other side), which
 * survives process death and configuration changes.
 */
data class InAppMessage(
    val id: String?,
    val layout: InAppLayout,
    val maxDisplays: Int?,
    val cooldownSec: Long?,
    val expireEpochSec: Long?,
    val rawJson: String
)

/** A piece of styled text — label plus its (required) color. */
data class InAppText(
    val text: String,
    @ColorInt val color: Int
)

/** What happens when an in-app element is tapped. */
sealed class InAppAction {
    data class Url(val url: String) : InAppAction()
    object Close : InAppAction()
}

data class InAppButton(
    val text: InAppText,
    @ColorInt val backgroundColor: Int,
    @ColorInt val borderColor: Int,
    val cornerRadiusDp: Float,
    val action: InAppAction
)

data class ModalContent(
    @ColorInt val backgroundColor: Int,
    val title: InAppText?,
    val message: InAppText?,
    val imageUrl: String?,
    val showCloseButton: Boolean,
    val buttons: List<InAppButton>,
    /** `true` dims + blocks the host until dismissed; `false` floats a non-blocking card. */
    val dimsBackground: Boolean
)

data class CarouselContent(
    val items: List<CarouselItem>,
    val showCloseButton: Boolean
)

data class CarouselItem(
    val imageUrl: String?,
    val title: InAppText?,
    val message: InAppText?,
    val action: InAppAction?
)

data class StoriesContent(
    val items: List<StoryItem>,
    val loops: Boolean,
    val showCloseButton: Boolean
)

data class StoryItem(
    val imageUrl: String?,
    val title: InAppText?,
    val message: InAppText?,
    val buttons: List<InAppButton>,
    val durationMs: Long
) {
    companion object {
        const val MAX_DURATION_MS = 30_000L
    }
}

enum class BannerPosition { TOP, BOTTOM }

data class BannerContent(
    val position: BannerPosition,
    val imageUrl: String?,
    val title: InAppText?,
    val message: InAppText?,
    @ColorInt val backgroundColor: Int,
    val action: InAppAction,
    /** Millis before auto-dismiss; `0` keeps the banner until dismissed. */
    val autoDismissMs: Long,
    val showCloseButton: Boolean
)

data class FullscreenContent(
    val imageUrl: String?,
    @ColorInt val backgroundColor: Int,
    val title: InAppText?,
    val message: InAppText?,
    val buttons: List<InAppButton>,
    val showCloseButton: Boolean
)

/**
 * Every image URL the message references, in slide order, including off-screen carousel/stories
 * slides, with null/blank dropped. Used to warm Glide's cache before the view is built (mirrors
 * iOS prefetching `message.imageURLs()` at enqueue time).
 */
fun InAppMessage.imageURLs(): List<String> = when (val layout = layout) {
    is InAppLayout.Modal -> listOfNotNull(layout.content.imageUrl)
    is InAppLayout.Carousel -> layout.content.items.mapNotNull { it.imageUrl }
    is InAppLayout.Stories -> layout.content.items.mapNotNull { it.imageUrl }
    is InAppLayout.Banner -> listOfNotNull(layout.content.imageUrl)
    is InAppLayout.Fullscreen -> listOfNotNull(layout.content.imageUrl)
}.filter { it.isNotBlank() }
