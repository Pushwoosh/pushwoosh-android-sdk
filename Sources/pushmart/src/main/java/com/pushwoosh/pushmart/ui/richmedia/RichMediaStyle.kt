package com.pushwoosh.pushmart.ui.richmedia

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture

/** Pure mapping between the style controls and SDK values; the fragment does the SDK calls. */
object RichMediaStyle {
    val DURATIONS_MS: List<Int> = listOf(300, 600, 1000)

    /** An empty set is what `setDefaultRichMediaConfig` stores as the NONE bitmask. */
    fun gestures(
        up: Boolean,
        left: Boolean,
        right: Boolean,
        down: Boolean
    ): Set<ModalRichMediaSwipeGesture> {
        val set = mutableSetOf<ModalRichMediaSwipeGesture>()
        if (up) set.add(ModalRichMediaSwipeGesture.UP)
        if (left) set.add(ModalRichMediaSwipeGesture.LEFT)
        if (right) set.add(ModalRichMediaSwipeGesture.RIGHT)
        if (down) set.add(ModalRichMediaSwipeGesture.DOWN)
        return set
    }

    /** Index into [DURATIONS_MS], or -1 when the stored value is not one of the presets. */
    fun durationIndex(ms: Int?): Int = DURATIONS_MS.indexOf(ms)
}
