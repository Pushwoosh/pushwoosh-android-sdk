package com.pushwoosh.pushmart.ui.inapp

import androidx.annotation.StringRes
import com.pushwoosh.pushmart.R

/**
 * Local native in-app configs for the showcase, in the exact shape
 * [com.pushwoosh.inapp.ui.parser.InAppConfigParser] accepts (see demoapp's `InAppPresets` for the
 * reference form). Images are module drawables addressed as
 * `android.resource://<package>/drawable/<name>`, so every preset renders offline; only the video
 * clip is fetched over https. The parser is fail-closed — `InAppPresetsTest` feeds every preset
 * through it in both color schemes. The copy is PushMart fashion copy: product links and photos
 * point at `Catalog` products, as iOS `InAppDemoConfigs` links do.
 */
class InAppPresets(private val packageName: String) {
    data class Preset(val id: String, @StringRes val label: Int, val config: Map<String, Any?>)

    private fun drawable(name: String): String = "android.resource://$packageName/drawable/$name"

    private fun text(text: String, color: String): Map<String, Any?> =
        mapOf("text" to text, "color" to color)

    private fun url(url: String): Map<String, Any?> = mapOf("type" to "url", "url" to url)

    private val close: Map<String, Any?> = mapOf("type" to "close")

    private fun button(
        text: String,
        textColor: String,
        background: String,
        border: String,
        radius: Int,
        action: Map<String, Any?>,
    ): Map<String, Any?> =
        mapOf(
            "text" to text(text, textColor),
            "background" to background,
            "border" to mapOf("color" to border, "radius" to radius),
            "action" to action,
        )

    /** Blocking modal with a square cover and a dark variant (the only preset carrying `dark`). */
    val modal =
        Preset(
            "pushmart-modal",
            R.string.inapp_preset_modal,
            mapOf(
                "displayType" to "modal",
                "inAppId" to "pushmart-modal",
                "modal" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to true,
                        "background" to SURFACE,
                        "image" to drawable("product_outwear_02"),
                        "title" to text("Members save 20% today", INK),
                        "message" to
                            text(
                                "Trenchcoats and the whole catalog. The discount lands at checkout.",
                                SECONDARY),
                        "buttons" to
                            listOf(
                                button(
                                    "Shop the sale",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    12,
                                    url("pushwoosh://sale")),
                                button("Maybe later", CORAL, SURFACE, CORAL, 12, close),
                            ),
                        "dark" to
                            mapOf(
                                "background" to "#16161AFF",
                                "image" to drawable("product_outwear_03"),
                                "title" to mapOf("color" to "#F2F2F7FF"),
                                "message" to mapOf("color" to "#B8B8C0FF"),
                                "buttons" to
                                    listOf(
                                        mapOf(
                                            "text" to mapOf("color" to INK),
                                            "background" to TANGERINE,
                                            "border" to mapOf("color" to TANGERINE),
                                        ),
                                        mapOf(
                                            "text" to mapOf("color" to TANGERINE),
                                            "background" to "#16161AFF",
                                            "border" to mapOf("color" to TANGERINE),
                                        ),
                                    ),
                            ),
                    ),
            ))

    /** Floating modal (`dimBackground: false`): the shop stays tappable behind the card. */
    val modalFloating =
        Preset(
            "pushmart-modal-floating",
            R.string.inapp_preset_modal_floating,
            mapOf(
                "displayType" to "modal",
                "inAppId" to "pushmart-modal-floating",
                "modal" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to false,
                        "background" to INK,
                        "image" to drawable("product_dress_04"),
                        "title" to text("Price drop on your wishlist", SURFACE),
                        "message" to
                            text(
                                "Plait Halter Sundress just dropped 15%. This card floats, the shop stays tappable.",
                                "#FFFFFFA6"),
                        "buttons" to
                            listOf(
                                button(
                                    "View the sundress",
                                    INK,
                                    TANGERINE,
                                    TANGERINE,
                                    20,
                                    url("pushwoosh://product/dress_04"))),
                    ),
            ))

    /** Blocking bottom sheet with a landscape cover and two buttons. */
    val sheet =
        Preset(
            "pushmart-sheet",
            R.string.inapp_preset_sheet,
            mapOf(
                "displayType" to "sheet",
                "inAppId" to "pushmart-sheet",
                "sheet" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to true,
                        "background" to SURFACE,
                        "image" to drawable("product_jeans_01"),
                        "title" to text("Your size is back", INK),
                        "message" to
                            text(
                                "Skinny Low Ankle Jeans are in stock again in your size. They will not last.",
                                SECONDARY),
                        "buttons" to
                            listOf(
                                button(
                                    "See the jeans",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    12,
                                    url("pushwoosh://product/jeans_01")),
                                button("Not now", CORAL, SURFACE, CORAL, 12, close),
                            ),
                    ),
            ))

    /** Floating sheet: no close button on purpose, swipe-down is the dismiss path. */
    val sheetFloating =
        Preset(
            "pushmart-sheet-floating",
            R.string.inapp_preset_sheet_floating,
            mapOf(
                "displayType" to "sheet",
                "inAppId" to "pushmart-sheet-floating",
                "sheet" to
                    mapOf(
                        "showClose" to false,
                        "dimBackground" to false,
                        "background" to INK,
                        "title" to text("New arrivals are in", SURFACE),
                        "message" to
                            text(
                                "Fresh picks landed in the shop this week. Swipe down to dismiss.",
                                "#FFFFFFA6"),
                        "buttons" to listOf(button("Got it", INK, TANGERINE, TANGERINE, 20, close)),
                    ),
            ))

    /** Carousel of drop photos linking catalog products; the last slide is image-only. */
    val carousel =
        Preset(
            "pushmart-carousel",
            R.string.inapp_preset_carousel,
            mapOf(
                "displayType" to "carousel",
                "inAppId" to "pushmart-carousel",
                "carousel" to
                    mapOf(
                        "showClose" to true,
                        "items" to
                            listOf(
                                mapOf(
                                    "image" to drawable("drop_trench"),
                                    "title" to text("Linen Trenchcoat", SURFACE),
                                    "message" to text("Just landed in Outerwear", SURFACE),
                                    "action" to url("pushwoosh://product/outwear_02"),
                                ),
                                mapOf(
                                    "image" to drawable("drop_sundress"),
                                    "title" to text("Plait Halter Sundress", SURFACE),
                                    "message" to text("Made for warm afternoons", SURFACE),
                                    "action" to url("pushwoosh://product/dress_04"),
                                ),
                                mapOf(
                                    "image" to drawable("drop_jeans"),
                                    "title" to text("Skinny Low Ankle Jeans", SURFACE),
                                    "message" to text("Your everyday denim", SURFACE),
                                    "action" to url("pushwoosh://product/jeans_01"),
                                ),
                                mapOf("image" to drawable("product_tops_02")),
                            ),
                    ),
            ))

    /** Stories on portrait drop photos with a per-frame duration and one button per frame. */
    val stories =
        Preset(
            "pushmart-stories",
            R.string.inapp_preset_stories,
            mapOf(
                "displayType" to "stories",
                "inAppId" to "pushmart-stories",
                "stories" to
                    mapOf(
                        "showClose" to true,
                        "loop" to false,
                        "items" to
                            listOf(
                                mapOf(
                                    "image" to drawable("drop_tee"),
                                    "title" to text("Drop 01", SURFACE),
                                    "message" to text("Three new arrivals this week", SURFACE),
                                    "duration" to 4,
                                    "buttons" to
                                        listOf(
                                            button(
                                                "See the Linen Trenchcoat",
                                                SURFACE,
                                                INK,
                                                INK,
                                                26,
                                                url("pushwoosh://product/outwear_02"))),
                                ),
                                mapOf(
                                    "image" to drawable("drop_sundress"),
                                    "title" to text("The summer sale", SURFACE),
                                    "message" to text("Sundresses and more, marked down", SURFACE),
                                    "duration" to 4,
                                    "buttons" to
                                        listOf(
                                            button(
                                                "Shop the sale",
                                                SURFACE,
                                                INK,
                                                INK,
                                                26,
                                                url("pushwoosh://sale"))),
                                ),
                                mapOf(
                                    "image" to drawable("drop_trench"),
                                    "title" to text("The denim edit", SURFACE),
                                    "message" to
                                        text("Skinny Low Ankle Jeans, back in stock", SURFACE),
                                    "duration" to 4,
                                    "buttons" to
                                        listOf(
                                            button(
                                                "See the jeans",
                                                SURFACE,
                                                INK,
                                                INK,
                                                26,
                                                url("pushwoosh://product/jeans_01"))),
                                ),
                            ),
                    ),
            ))

    /** Top banner with a product thumbnail, tap-through to the product, auto-dismiss after 6 s. */
    val banner =
        Preset(
            "pushmart-banner",
            R.string.inapp_preset_banner,
            mapOf(
                "displayType" to "banner",
                "inAppId" to "pushmart-banner",
                "banner" to
                    mapOf(
                        "showClose" to true,
                        "position" to "top",
                        "background" to CORAL,
                        "image" to drawable("product_outwear_02"),
                        "title" to text("Back in stock: Linen Trenchcoat", INK),
                        "message" to text("Tap to see it before it sells out", "#0B0B10D9"),
                        "action" to url("pushwoosh://product/outwear_02"),
                        "autoDismiss" to 6,
                    ),
            ))

    /** Fullscreen with a portrait drop photo over an ink background. */
    val fullscreen =
        Preset(
            "pushmart-fullscreen",
            R.string.inapp_preset_fullscreen,
            mapOf(
                "displayType" to "fullscreen",
                "inAppId" to "pushmart-fullscreen",
                "fullscreen" to
                    mapOf(
                        "showClose" to true,
                        "cover" to mapOf("image" to drawable("drop_trench"), "background" to INK),
                        "title" to text("PushMart Plus", SURFACE),
                        "message" to text("Early access to drops and member prices.", "#FFFFFFD9"),
                        "buttons" to
                            listOf(
                                button(
                                    "Start free trial",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    12,
                                    url("pushwoosh://sale")),
                                button("No thanks", SURFACE, "#00000000", "#FFFFFF99", 12, close),
                            ),
                    ),
            ))

    /** Video: https clip (no drawable can hold video), poster and fallback are local. */
    val video =
        Preset(
            "pushmart-video",
            R.string.inapp_preset_video,
            mapOf(
                "displayType" to "video",
                "inAppId" to "pushmart-video",
                "video" to
                    mapOf(
                        "showClose" to true,
                        "loop" to true,
                        "muted" to true,
                        "url" to
                            "https://videos.pexels.com/video-files/854745/854745-hd_1280_720_50fps.mp4",
                        "poster" to drawable("drop_trench"),
                        "fallback" to drawable("drop_trench"),
                        "title" to text("The new season, in motion", SURFACE),
                        "message" to text("Watch the edit move before anyone else.", "#FFFFFFD9"),
                        "buttons" to
                            listOf(
                                button(
                                    "Shop the sale",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    14,
                                    url("pushwoosh://sale"))),
                    ),
            ))

    val all: List<Preset> =
        listOf(
            modal,
            modalFloating,
            sheet,
            sheetFloating,
            carousel,
            stories,
            banner,
            fullscreen,
            video)

    private companion object {
        const val CORAL = "#FF5A5FFF"
        const val TANGERINE = "#FF8A3DFF"
        const val INK = "#0B0B10FF"
        const val SURFACE = "#FFFFFFFF"
        const val SECONDARY = "#000000A6"
    }
}
