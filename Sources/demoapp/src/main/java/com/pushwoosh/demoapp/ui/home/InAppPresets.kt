package com.pushwoosh.demoapp.ui.home

/**
 * Ready-made native in-app configs for local testing without a server round-trip.
 *
 * Each constant is a config map in the shape [com.pushwoosh.inapp.ui.parser.InAppConfigParser]
 * accepts — feed it straight to `PushwooshInAppUi.present(config)`. Nested blocks are maps, arrays
 * are lists. Together they cover every layout (banner / modal / sheet / fullscreen / carousel /
 * stories), both display paths (blocking queue vs. floating overlay), and the richer optional
 * fields (styled text colors, images, multi-button actions, auto-dismiss, per-story duration). All
 * configs are in the canonical typed-contract form (no legacy aliases).
 *
 * `@JvmField` keeps them readable from Java as `InAppPresets.BANNER`.
 *
 * Images point at picsum.photos, so they only render with network access; the layout still shows
 * without it.
 */
object InAppPresets {

    /** Top banner: image + styled text, tinted background, auto-dismiss, tap-through url. */
    @JvmField
    val BANNER: Map<String, Any?> =
        mapOf(
            "displayType" to "banner",
            "inAppId" to "demo-banner",
            "banner" to
                mapOf(
                    "showClose" to true,
                    "position" to "top",
                    "background" to "#1E88E5FF",
                    "image" to "https://picsum.photos/seed/pwbanner/160/160",
                    "title" to mapOf("text" to "Flash Sale", "color" to "#FFFFFFFF"),
                    "message" to mapOf("text" to "50% off — today only", "color" to "#E3F2FDFF"),
                    "action" to mapOf("type" to "url", "url" to "https://pushwoosh.com"),
                    "autoDismiss" to 6,
                ),
        )

    /** Blocking modal: header image, two styled buttons (url + close), dims and blocks the host. */
    @JvmField
    val MODAL: Map<String, Any?> =
        mapOf(
            "displayType" to "modal",
            "inAppId" to "demo-modal",
            "modal" to
                mapOf(
                    "showClose" to true,
                    "dimBackground" to true,
                    "background" to "#FFFFFFFF",
                    "image" to "https://picsum.photos/seed/pwmodal/600/320",
                    "title" to mapOf("text" to "Welcome aboard!", "color" to "#1A1A1AFF"),
                    "message" to
                        mapOf(
                            "text" to
                                "Thanks for installing the app. Enable notifications so you never miss an update.",
                            "color" to "#555555FF",
                        ),
                    "buttons" to
                        listOf(
                            mapOf(
                                "text" to mapOf("text" to "Enable", "color" to "#FFFFFFFF"),
                                "background" to "#1E88E5FF",
                                "border" to mapOf("color" to "#1E88E5FF", "radius" to 12),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com"),
                            ),
                            mapOf(
                                "text" to mapOf("text" to "Later", "color" to "#1E88E5FF"),
                                "background" to "#FFFFFFFF",
                                "border" to mapOf("color" to "#1E88E5FF", "radius" to 12),
                                "action" to mapOf("type" to "close"),
                            ),
                        ),
                ),
        )

    /** Floating modal (dimBackground: false): non-blocking overlay card. */
    @JvmField
    val MODAL_FLOATING: Map<String, Any?> =
        mapOf(
            "displayType" to "modal",
            "inAppId" to "demo-modal-floating",
            "modal" to
                mapOf(
                    "showClose" to true,
                    "dimBackground" to false,
                    "background" to "#263238FF",
                    "title" to mapOf("text" to "New message", "color" to "#FFFFFFFF"),
                    "message" to
                        mapOf(
                            "text" to "This card floats over your content instead of blocking it.",
                            "color" to "#B0BEC5FF",
                        ),
                    "buttons" to
                        listOf(
                            mapOf(
                                "text" to mapOf("text" to "Open", "color" to "#263238FF"),
                                "background" to "#FFD54FFF",
                                "border" to mapOf("color" to "#FFD54FFF", "radius" to 20),
                                "action" to mapOf("type" to "close"),
                            )),
                ),
        )

    /** Blocking sheet: bottom card with a grabber, cover, left-aligned text and two buttons. */
    @JvmField
    val SHEET: Map<String, Any?> =
        mapOf(
            "displayType" to "sheet",
            "inAppId" to "demo-sheet",
            "sheet" to
                mapOf(
                    "showClose" to true,
                    "dimBackground" to true,
                    "background" to "#FFFFFFFF",
                    "image" to "https://picsum.photos/seed/pwsheet/600/320",
                    "title" to mapOf("text" to "Your quote is ready", "color" to "#1A1A1AFF"),
                    "message" to
                        mapOf(
                            "text" to
                                "Guaranteed buyout for your A110: \$68,500. Valid for 7 days.",
                            "color" to "#555555FF",
                        ),
                    "buttons" to
                        listOf(
                            mapOf(
                                "text" to
                                    mapOf("text" to "Get guaranteed quote", "color" to "#FFFFFFFF"),
                                "background" to "#1E88E5FF",
                                "border" to mapOf("color" to "#1E88E5FF", "radius" to 12),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com"),
                            ),
                            mapOf(
                                "text" to mapOf("text" to "Not now", "color" to "#1E88E5FF"),
                                "background" to "#FFFFFFFF",
                                "border" to mapOf("color" to "#1E88E5FF", "radius" to 12),
                                "action" to mapOf("type" to "close"),
                            ),
                        ),
                ),
        )

    /**
     * Floating sheet (dimBackground: false): non-blocking bottom card, no cover, dark surface —
     * taps above it reach the app, and the grabber has to contrast against a dark background.
     * `showClose` is off on purpose: the swipe-down is the guaranteed dismiss path, no forced ✕.
     */
    @JvmField
    val SHEET_FLOATING: Map<String, Any?> =
        mapOf(
            "displayType" to "sheet",
            "inAppId" to "demo-sheet-floating",
            "sheet" to
                mapOf(
                    "showClose" to false,
                    "dimBackground" to false,
                    "background" to "#263238FF",
                    "title" to mapOf("text" to "Trade-in offer", "color" to "#FFFFFFFF"),
                    "message" to
                        mapOf(
                            "text" to
                                "Swipe down to dismiss — the app stays usable behind this sheet.",
                            "color" to "#B0BEC5FF",
                        ),
                    "buttons" to
                        listOf(
                            mapOf(
                                "text" to mapOf("text" to "Open", "color" to "#263238FF"),
                                "background" to "#FFD54FFF",
                                "border" to mapOf("color" to "#FFD54FFF", "radius" to 20),
                                "action" to mapOf("type" to "close"),
                            )),
                ),
        )

    /** Fullscreen: full-bleed cover image over a background color, title/message, two buttons. */
    @JvmField
    val FULLSCREEN: Map<String, Any?> =
        mapOf(
            "displayType" to "fullscreen",
            "inAppId" to "demo-fullscreen",
            "fullscreen" to
                mapOf(
                    "showClose" to true,
                    "cover" to
                        mapOf(
                            "image" to "https://picsum.photos/seed/pwcover/900/1400",
                            "background" to "#1A1A1EFF",
                        ),
                    "title" to mapOf("text" to "Unlock Premium", "color" to "#FFFFFFFF"),
                    "message" to
                        mapOf(
                            "text" to "Go ad-free and get exclusive content.",
                            "color" to "#ECEFF1FF"),
                    "buttons" to
                        listOf(
                            mapOf(
                                "text" to
                                    mapOf("text" to "Start free trial", "color" to "#FFFFFFFF"),
                                "background" to "#43A047FF",
                                "border" to mapOf("color" to "#43A047FF", "radius" to 12),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com"),
                            ),
                            mapOf(
                                "text" to mapOf("text" to "No thanks", "color" to "#FFFFFFFF"),
                                "background" to "#00000000",
                                "border" to mapOf("color" to "#FFFFFF99", "radius" to 12),
                                "action" to mapOf("type" to "close"),
                            ),
                        ),
                ),
        )

    /**
     * Carousel: swipeable cards, each with image + styled title/message and its own tap-through
     * url. The last slide is image-only on purpose — it shows the text scrim going out on a slide
     * with nothing to protect.
     */
    @JvmField
    val CAROUSEL: Map<String, Any?> =
        mapOf(
            "displayType" to "carousel",
            "inAppId" to "demo-carousel",
            "carousel" to
                mapOf(
                    "showClose" to true,
                    "items" to
                        listOf(
                            mapOf(
                                "image" to "https://picsum.photos/seed/pwc1/640/420",
                                "title" to mapOf("text" to "Explore", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf(
                                        "text" to "Discover new features", "color" to "#FFFFFFFF"),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com/1"),
                            ),
                            mapOf(
                                "image" to "https://picsum.photos/seed/pwc2/640/420",
                                "title" to mapOf("text" to "Connect", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf("text" to "Reach your audience", "color" to "#FFFFFFFF"),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com/2"),
                            ),
                            mapOf(
                                "image" to "https://picsum.photos/seed/pwc3/640/420",
                                "title" to mapOf("text" to "Grow", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf("text" to "Boost engagement", "color" to "#FFFFFFFF"),
                                "action" to
                                    mapOf("type" to "url", "url" to "https://pushwoosh.com/3"),
                            ),
                            mapOf("image" to "https://picsum.photos/seed/pwc4/640/420"),
                        ),
                ),
        )

    /** Stories: auto-advancing full-screen frames with per-frame duration and CTA buttons. */
    @JvmField
    val STORIES: Map<String, Any?> =
        mapOf(
            "displayType" to "stories",
            "inAppId" to "demo-stories",
            "stories" to
                mapOf(
                    "showClose" to true,
                    "loop" to false,
                    "items" to
                        listOf(
                            mapOf(
                                "image" to "https://picsum.photos/seed/pws1/800/1400",
                                "title" to mapOf("text" to "Day 1", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf("text" to "Your journey begins", "color" to "#FFFFFFFF"),
                                "duration" to 4,
                                "buttons" to
                                    listOf(
                                        mapOf(
                                            "text" to
                                                mapOf("text" to "Next", "color" to "#FFFFFFFF"),
                                            "background" to "#0F0F0FFF",
                                            "border" to
                                                mapOf("color" to "#0F0F0FFF", "radius" to 26),
                                            "action" to mapOf("type" to "close"),
                                        )),
                            ),
                            mapOf(
                                "image" to "https://picsum.photos/seed/pws2/800/1400",
                                "title" to mapOf("text" to "Day 2", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf(
                                        "text" to "Keep the streak going", "color" to "#FFFFFFFF"),
                                "duration" to 4,
                                "buttons" to emptyList<Any>(),
                            ),
                            mapOf(
                                "image" to "https://picsum.photos/seed/pws3/800/1400",
                                "title" to mapOf("text" to "Day 3", "color" to "#FFFFFFFF"),
                                "message" to
                                    mapOf("text" to "You are a pro now", "color" to "#FFFFFFFF"),
                                "duration" to 4,
                                "buttons" to
                                    listOf(
                                        mapOf(
                                            "text" to
                                                mapOf(
                                                    "text" to "Get started",
                                                    "color" to "#FFFFFFFF"),
                                            "background" to "#0F0F0FFF",
                                            "border" to
                                                mapOf("color" to "#0F0F0FFF", "radius" to 26),
                                            "action" to
                                                mapOf(
                                                    "type" to "url",
                                                    "url" to "https://pushwoosh.com"),
                                        )),
                            ),
                        ),
                ),
        )
}
