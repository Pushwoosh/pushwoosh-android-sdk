package com.pushwoosh.pushmart.ui.inapp

/**
 * Local native in-app configs for the showcase, in the exact shape
 * [com.pushwoosh.inapp.ui.parser.InAppConfigParser] accepts (see demoapp's `InAppPresets` for the
 * reference form). Images are module drawables addressed as
 * `android.resource://<package>/drawable/<name>`, so every preset renders offline; only the video
 * clip is fetched over https. The parser is fail-closed — `InAppPresetsTest` feeds every preset
 * through it in both color schemes.
 */
class InAppPresets(private val packageName: String) {
    data class Preset(val id: String, val label: String, val config: Map<String, Any?>)

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
            "Modal",
            mapOf(
                "displayType" to "modal",
                "inAppId" to "pushmart-modal",
                "modal" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to true,
                        "background" to SURFACE,
                        "image" to drawable("product_lumen_square"),
                        "title" to text("Members save 20% today", INK),
                        "message" to
                            text(
                                "Lumen Frames and the whole catalog. The discount lands at checkout.",
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
                                "image" to drawable("product_beacon"),
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
            "Modal floating",
            mapOf(
                "displayType" to "modal",
                "inAppId" to "pushmart-modal-floating",
                "modal" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to false,
                        "background" to INK,
                        "title" to text("Price drop on your wishlist", SURFACE),
                        "message" to
                            text(
                                "Chrono S3 just dropped 15%. This card floats, the shop stays tappable.",
                                "#FFFFFFA6"),
                        "buttons" to
                            listOf(
                                button(
                                    "View Chrono S3",
                                    INK,
                                    TANGERINE,
                                    TANGERINE,
                                    20,
                                    url("pushwoosh://product/chrono"))),
                    ),
            ))

    /** Blocking bottom sheet with a landscape cover and two buttons. */
    val sheet =
        Preset(
            "pushmart-sheet",
            "Sheet",
            mapOf(
                "displayType" to "sheet",
                "inAppId" to "pushmart-sheet",
                "sheet" to
                    mapOf(
                        "showClose" to true,
                        "dimBackground" to true,
                        "background" to SURFACE,
                        "image" to drawable("product_aeroknit"),
                        "title" to text("Your size is back", INK),
                        "message" to
                            text(
                                "AeroKnit Runner in 42 is in stock again. It will not last.",
                                SECONDARY),
                        "buttons" to
                            listOf(
                                button(
                                    "See the runner",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    12,
                                    url("pushwoosh://product/aeroknit")),
                                button("Not now", CORAL, SURFACE, CORAL, 12, close),
                            ),
                    ),
            ))

    /** Floating sheet: no close button on purpose, swipe-down is the dismiss path. */
    val sheetFloating =
        Preset(
            "pushmart-sheet-floating",
            "Sheet floating",
            mapOf(
                "displayType" to "sheet",
                "inAppId" to "pushmart-sheet-floating",
                "sheet" to
                    mapOf(
                        "showClose" to false,
                        "dimBackground" to false,
                        "background" to INK,
                        "title" to text("Free shipping unlocked", SURFACE),
                        "message" to
                            text(
                                "Orders over \$50 ship free this week. Swipe down to dismiss.",
                                "#FFFFFFA6"),
                        "buttons" to listOf(button("Got it", INK, TANGERINE, TANGERINE, 20, close)),
                    ),
            ))

    /** Carousel mixing square, landscape and portrait covers; the last slide is image-only. */
    val carousel =
        Preset(
            "pushmart-carousel",
            "Carousel",
            mapOf(
                "displayType" to "carousel",
                "inAppId" to "pushmart-carousel",
                "carousel" to
                    mapOf(
                        "showClose" to true,
                        "items" to
                            listOf(
                                mapOf(
                                    "image" to drawable("product_lumen_square"),
                                    "title" to text("New: Lumen Frames", SURFACE),
                                    "message" to text("Titanium, three tints", SURFACE),
                                    "action" to url("pushwoosh://product/lumen"),
                                ),
                                mapOf(
                                    "image" to drawable("product_trail"),
                                    "title" to text("Trail Pack 24L", SURFACE),
                                    "message" to text("Built for weekend routes", SURFACE),
                                    "action" to url("pushwoosh://product/trail"),
                                ),
                                mapOf(
                                    "image" to drawable("product_orbit_portrait"),
                                    "title" to text("Orbit Cable", SURFACE),
                                    "message" to text("Braided, 2 m, any port", SURFACE),
                                    "action" to url("pushwoosh://product/orbit"),
                                ),
                                mapOf("image" to drawable("product_voyager")),
                            ),
                    ),
            ))

    /** Stories on portrait covers, per-frame duration, one frame without buttons. */
    val stories =
        Preset(
            "pushmart-stories",
            "Stories",
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
                                    "image" to drawable("product_orbit_portrait"),
                                    "title" to text("Drop 01", SURFACE),
                                    "message" to text("Three new arrivals this week", SURFACE),
                                    "duration" to 4,
                                    "buttons" to
                                        listOf(button("Next", SURFACE, INK, INK, 26, close)),
                                ),
                                mapOf(
                                    "image" to drawable("product_lumen_square"),
                                    "title" to text("Lumen Frames", SURFACE),
                                    "message" to text("Lightweight titanium", SURFACE),
                                    "duration" to 4,
                                    "buttons" to emptyList<Any>(),
                                ),
                                mapOf(
                                    "image" to drawable("product_orbit_portrait"),
                                    "title" to text("Orbit Cable", SURFACE),
                                    "message" to text("Braided, 2 m, any port", SURFACE),
                                    "duration" to 4,
                                    "buttons" to
                                        listOf(
                                            button(
                                                "Shop the drop",
                                                SURFACE,
                                                INK,
                                                INK,
                                                26,
                                                url("pushwoosh://sale"))),
                                ),
                            ),
                    ),
            ))

    /** Top banner with a landscape thumbnail, tap-through to the sale, auto-dismiss after 6 s. */
    val banner =
        Preset(
            "pushmart-banner",
            "Banner",
            mapOf(
                "displayType" to "banner",
                "inAppId" to "pushmart-banner",
                "banner" to
                    mapOf(
                        "showClose" to true,
                        "position" to "top",
                        "background" to CORAL,
                        "image" to drawable("product_glide"),
                        "title" to text("Flash sale is live", INK),
                        "message" to text("Up to 40% off for the next 3 hours", "#0B0B10D9"),
                        "action" to url("pushwoosh://sale"),
                        "autoDismiss" to 6,
                    ),
            ))

    /** Fullscreen with a portrait cover over an ink background. */
    val fullscreen =
        Preset(
            "pushmart-fullscreen",
            "Fullscreen",
            mapOf(
                "displayType" to "fullscreen",
                "inAppId" to "pushmart-fullscreen",
                "fullscreen" to
                    mapOf(
                        "showClose" to true,
                        "cover" to
                            mapOf(
                                "image" to drawable("product_orbit_portrait"), "background" to INK),
                        "title" to text("PushMart Plus", SURFACE),
                        "message" to
                            text(
                                "Early access to drops, free returns, member prices.", "#FFFFFFD9"),
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
            "Video",
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
                        "poster" to drawable("product_voyager"),
                        "fallback" to drawable("product_voyager"),
                        "title" to text("Voyager Rattan, in motion", SURFACE),
                        "message" to text("See the weave before you buy.", "#FFFFFFD9"),
                        "buttons" to
                            listOf(
                                button(
                                    "Shop Voyager",
                                    INK,
                                    CORAL,
                                    CORAL,
                                    14,
                                    url("pushwoosh://product/voyager"))),
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
