package com.pushwoosh.demoapp.ui.home

/**
 * Ready-made native in-app configs for local testing without a server round-trip.
 *
 * Each constant is the raw `u` custom-data JSON that
 * [com.pushwoosh.inapp.ui.parser.InAppConfigParser] accepts — feed it straight to
 * `PushwooshInAppUi.present(json)`. Together they cover every layout (banner / modal / fullscreen /
 * carousel / stories), both display paths (blocking queue vs. floating overlay), and the richer
 * optional fields (styled text colors, images, multi-button actions, auto-dismiss, per-story
 * duration). All configs are in the canonical typed-contract form (no legacy aliases).
 *
 * Images point at picsum.photos, so they only render with network access; the layout still shows
 * without it.
 */
object InAppPresets {

    /** Top banner: image + styled text, tinted background, auto-dismiss, tap-through url. */
    const val BANNER =
        """
        {
          "displayType": "banner",
          "inAppId": "demo-banner",
          "banner": {
            "showClose": true,
            "position": "top",
            "background": "#1E88E5FF",
            "image": "https://picsum.photos/seed/pwbanner/160/160",
            "title": {"text": "Flash Sale", "color": "#FFFFFFFF"},
            "message": {"text": "50% off — today only", "color": "#E3F2FDFF"},
            "action": {"type": "url", "url": "https://pushwoosh.com"},
            "autoDismiss": 6
          }
        }
    """

    /** Blocking modal: header image, two styled buttons (url + close), dims and blocks the host. */
    const val MODAL =
        """
        {
          "displayType": "modal",
          "inAppId": "demo-modal",
          "modal": {
            "showClose": true,
            "dimBackground": true,
            "background": "#FFFFFFFF",
            "image": "https://picsum.photos/seed/pwmodal/600/320",
            "title": {"text": "Welcome aboard!", "color": "#1A1A1AFF"},
            "message": {"text": "Thanks for installing the app. Enable notifications so you never miss an update.", "color": "#555555FF"},
            "buttons": [
              {"text": {"text": "Enable", "color": "#FFFFFFFF"}, "background": "#1E88E5FF", "border": {"color": "#1E88E5FF", "radius": 12}, "action": {"type": "url", "url": "https://pushwoosh.com"}},
              {"text": {"text": "Later", "color": "#1E88E5FF"}, "background": "#FFFFFFFF", "border": {"color": "#1E88E5FF", "radius": 12}, "action": {"type": "close"}}
            ]
          }
        }
    """

    /** Floating modal (dimBackground: false): non-blocking overlay card. */
    const val MODAL_FLOATING =
        """
        {
          "displayType": "modal",
          "inAppId": "demo-modal-floating",
          "modal": {
            "showClose": true,
            "dimBackground": false,
            "background": "#263238FF",
            "title": {"text": "New message", "color": "#FFFFFFFF"},
            "message": {"text": "This card floats over your content instead of blocking it.", "color": "#B0BEC5FF"},
            "buttons": [
              {"text": {"text": "Open", "color": "#263238FF"}, "background": "#FFD54FFF", "border": {"color": "#FFD54FFF", "radius": 20}, "action": {"type": "close"}}
            ]
          }
        }
    """

    /** Fullscreen: full-bleed cover image over a background color, title/message, two buttons. */
    const val FULLSCREEN =
        """
        {
          "displayType": "fullscreen",
          "inAppId": "demo-fullscreen",
          "fullscreen": {
            "showClose": true,
            "cover": {"image": "https://picsum.photos/seed/pwcover/900/1400", "background": "#1A1A1EFF"},
            "title": {"text": "Unlock Premium", "color": "#FFFFFFFF"},
            "message": {"text": "Go ad-free and get exclusive content.", "color": "#ECEFF1FF"},
            "buttons": [
              {"text": {"text": "Start free trial", "color": "#FFFFFFFF"}, "background": "#43A047FF", "border": {"color": "#43A047FF", "radius": 12}, "action": {"type": "url", "url": "https://pushwoosh.com"}},
              {"text": {"text": "No thanks", "color": "#FFFFFFFF"}, "background": "#00000000", "border": {"color": "#FFFFFF99", "radius": 12}, "action": {"type": "close"}}
            ]
          }
        }
    """

    /**
     * Carousel: swipeable cards, each with image + styled title/message and its own tap-through
     * url.
     */
    const val CAROUSEL =
        """
        {
          "displayType": "carousel",
          "inAppId": "demo-carousel",
          "carousel": {
            "showClose": true,
            "items": [
              {"image": "https://picsum.photos/seed/pwc1/640/420", "title": {"text": "Explore", "color": "#FFFFFFFF"}, "message": {"text": "Discover new features", "color": "#FFFFFFFF"}, "action": {"type": "url", "url": "https://pushwoosh.com/1"}},
              {"image": "https://picsum.photos/seed/pwc2/640/420", "title": {"text": "Connect", "color": "#FFFFFFFF"}, "message": {"text": "Reach your audience", "color": "#FFFFFFFF"}, "action": {"type": "url", "url": "https://pushwoosh.com/2"}},
              {"image": "https://picsum.photos/seed/pwc3/640/420", "title": {"text": "Grow", "color": "#FFFFFFFF"}, "message": {"text": "Boost engagement", "color": "#FFFFFFFF"}, "action": {"type": "url", "url": "https://pushwoosh.com/3"}}
            ]
          }
        }
    """

    /** Stories: auto-advancing full-screen frames with per-frame duration and CTA buttons. */
    const val STORIES =
        """
        {
          "displayType": "stories",
          "inAppId": "demo-stories",
          "stories": {
            "showClose": true,
            "loop": false,
            "items": [
              {"image": "https://picsum.photos/seed/pws1/800/1400", "title": {"text": "Day 1", "color": "#FFFFFFFF"}, "message": {"text": "Your journey begins", "color": "#FFFFFFFF"}, "duration": 4, "buttons": [{"text": {"text": "Next", "color": "#FFFFFFFF"}, "background": "#0F0F0FFF", "border": {"color": "#0F0F0FFF", "radius": 26}, "action": {"type": "close"}}]},
              {"image": "https://picsum.photos/seed/pws2/800/1400", "title": {"text": "Day 2", "color": "#FFFFFFFF"}, "message": {"text": "Keep the streak going", "color": "#FFFFFFFF"}, "duration": 4, "buttons": []},
              {"image": "https://picsum.photos/seed/pws3/800/1400", "title": {"text": "Day 3", "color": "#FFFFFFFF"}, "message": {"text": "You are a pro now", "color": "#FFFFFFFF"}, "duration": 4, "buttons": [{"text": {"text": "Get started", "color": "#FFFFFFFF"}, "background": "#0F0F0FFF", "border": {"color": "#0F0F0FFF", "radius": 26}, "action": {"type": "url", "url": "https://pushwoosh.com"}}]}
            ]
          }
        }
    """
}
