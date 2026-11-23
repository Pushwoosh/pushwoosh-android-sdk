package com.pushwoosh.inline

/**
 * Receives inline slot events. The SDK holds the listener strongly; every callback runs on the
 * main thread. Java implementations can extend [PushwooshInlineListenerAdapter] to override only
 * the callbacks they need.
 */
interface PushwooshInlineListener {
    /** The banner of [slotId] changed; `banner` is `null` when the slot became empty. */
    fun onSlotUpdated(slotId: String, banner: PushwooshInlineBanner?) {}

    /** [banner] was shown in [slotId] (counted as an impression). */
    fun onSlotShown(slotId: String, banner: PushwooshInlineBanner) {}

    /** [banner] in [slotId] was tapped; [elementId] is the tapped element, `null` for the banner itself. */
    fun onSlotClicked(slotId: String, banner: PushwooshInlineBanner, elementId: String?) {}

    /** [banner] in [slotId] was closed by the user. */
    fun onSlotDismissed(slotId: String, banner: PushwooshInlineBanner) {}
}
