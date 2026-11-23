package com.pushwoosh.pushmart.ui.offers

import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InboxCounterTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun noUnreadShowsTheDefaultSubtitle() {
        assertEquals("Offers and updates from PushMart", InboxCounter.subtitle(resources, 0))
    }

    @Test
    fun oneUnreadIsSingular() {
        assertEquals("1 new offer", InboxCounter.subtitle(resources, 1))
    }

    @Test
    fun severalUnreadArePlural() {
        assertEquals("3 new offers", InboxCounter.subtitle(resources, 3))
    }

    @Test
    fun largeCountKeepsTheFullNumber() {
        assertEquals("12 new offers", InboxCounter.subtitle(resources, 12))
    }

    @Test
    fun negativeUnreadShowsTheDefaultSubtitle() {
        assertEquals("Offers and updates from PushMart", InboxCounter.subtitle(resources, -1))
    }
}
