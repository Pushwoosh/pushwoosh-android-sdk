package com.pushwoosh.inline

/**
 * [PushwooshInlineListener] with empty callbacks, for Java code that overrides only some of them.
 */
abstract class PushwooshInlineListenerAdapter : PushwooshInlineListener {
    override fun onSlotUpdated(slotId: String, banner: PushwooshInlineBanner?) {}

    override fun onSlotShown(slotId: String, banner: PushwooshInlineBanner) {}

    override fun onSlotClicked(slotId: String, banner: PushwooshInlineBanner, elementId: String?) {}

    override fun onSlotDismissed(slotId: String, banner: PushwooshInlineBanner) {}
}
