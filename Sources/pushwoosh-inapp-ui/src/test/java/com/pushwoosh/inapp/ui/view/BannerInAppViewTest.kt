package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import com.pushwoosh.inapp.ui.model.BannerContent
import com.pushwoosh.inapp.ui.model.BannerPosition
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class BannerInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun topBannerClosesOnUpwardFling() {
        assertTrue(shouldCloseOnSwipe(BannerPosition.TOP, 0f, -1200f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.TOP, 0f, 1200f))
    }

    @Test
    fun bottomBannerClosesOnDownwardFling() {
        assertTrue(shouldCloseOnSwipe(BannerPosition.BOTTOM, 0f, 1200f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.BOTTOM, 0f, -1200f))
    }

    @Test
    fun horizontalDominantFlingDoesNotClose() {
        assertFalse(shouldCloseOnSwipe(BannerPosition.TOP, -2000f, -300f))
        assertFalse(shouldCloseOnSwipe(BannerPosition.BOTTOM, 2000f, 300f))
    }

    // The banner card is always clickable (it always carries an action now); this guards that the
    // card stays a touch target so the GestureDetector receives MOVE/UP and swipe-to-close works.
    @Test
    fun bannerCardIsClickable() {
        val content = BannerContent(
            position = BannerPosition.TOP,
            imageUrl = null,
            title = InAppText("Info", Color.WHITE),
            message = null,
            backgroundColor = Color.BLACK,
            action = InAppAction.Close,
            autoDismissMs = 0L,
            showCloseButton = false
        )
        val view = BannerInAppView(activity, content)
        val card = view.getChildAt(0) as LinearLayout
        assertTrue("banner card must be clickable so swipe gestures are received", card.isClickable)
    }

    private fun content(showCloseButton: Boolean) = BannerContent(
        position = BannerPosition.TOP,
        imageUrl = null,
        title = InAppText("Info", Color.WHITE),
        message = null,
        backgroundColor = Color.BLACK,
        action = InAppAction.Close,
        autoDismissMs = 0L,
        showCloseButton = showCloseButton
    )

    @Test
    fun closeButtonFollowsShowCloseButtonFlag() {
        assertNotNull("banner with showCloseButton=true must show the inline ✕",
            findCloseButton(BannerInAppView(activity, content(showCloseButton = true))))
        assertNull("banner has swipe-to-dismiss — the ✕ stays opt-in",
            findCloseButton(BannerInAppView(activity, content(showCloseButton = false))))
    }
}
