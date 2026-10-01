package com.pushwoosh.pushmart.ui.richmedia

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture
import org.junit.Assert.assertEquals
import org.junit.Test

class RichMediaStyleTest {
    @Test
    fun noCheckedGesturesGivesEmptySetNotNone() {
        assertEquals(
            emptySet<ModalRichMediaSwipeGesture>(),
            RichMediaStyle.gestures(false, false, false, false))
    }

    @Test
    fun checkedGesturesMapToEnumValues() {
        assertEquals(
            setOf(ModalRichMediaSwipeGesture.UP, ModalRichMediaSwipeGesture.RIGHT),
            RichMediaStyle.gestures(up = true, left = false, right = true, down = false))
        assertEquals(
            setOf(ModalRichMediaSwipeGesture.LEFT, ModalRichMediaSwipeGesture.DOWN),
            RichMediaStyle.gestures(up = false, left = true, right = false, down = true))
    }

    @Test
    fun durationIndexFindsPresetsAndRejectsOthers() {
        assertEquals(0, RichMediaStyle.durationIndex(300))
        assertEquals(1, RichMediaStyle.durationIndex(600))
        assertEquals(2, RichMediaStyle.durationIndex(1000))
        assertEquals(-1, RichMediaStyle.durationIndex(250))
        assertEquals(-1, RichMediaStyle.durationIndex(null))
    }
}
