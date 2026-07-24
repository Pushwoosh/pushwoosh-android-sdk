package com.pushwoosh.inapp.ui

import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppModuleTest {

    @Before
    fun setup() {
        InAppModule.delegate = null
    }

    @After
    fun tearDown() {
        InAppModule.delegate = null
    }

    /// A strongly-referenced delegate roundtrips through get/set and can be cleared to null.
    @Test
    fun delegateRoundtripsAndClears() {
        val delegate = object : InAppMessageDelegate {}
        InAppModule.delegate = delegate
        assertSame(delegate, InAppModule.delegate)

        InAppModule.delegate = null
        assertNull(InAppModule.delegate)
    }

    /// The delegate is held weakly: once the host's strong reference drops, it becomes
    /// collectible and the getter returns null.
    @Test
    fun delegateIsHeldWeakly() {
        var strong: InAppMessageDelegate? = object : InAppMessageDelegate {}
        InAppModule.delegate = strong
        assertNotNull(InAppModule.delegate)

        strong = null
        var collected = false
        for (i in 0 until 50) {
            System.gc()
            Runtime.getRuntime().runFinalization()
            if (InAppModule.delegate == null) {
                collected = true
                break
            }
            Thread.sleep(10)
        }
        assertTrue("a weakly-held delegate must be collectible once the strong ref drops", collected)
    }
}
