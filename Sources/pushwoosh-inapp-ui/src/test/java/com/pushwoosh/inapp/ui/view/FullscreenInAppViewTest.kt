package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.model.FullscreenContent
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class FullscreenInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun content(showCloseButton: Boolean, buttons: List<InAppButton> = emptyList()) = FullscreenContent(
        imageUrl = null,
        backgroundColor = Color.BLACK,
        title = InAppText("Title", Color.WHITE),
        message = null,
        buttons = buttons,
        showCloseButton = showCloseButton
    )

    private fun button() = InAppButton(
        text = InAppText("OK", Color.WHITE),
        backgroundColor = Color.BLUE,
        borderColor = Color.BLUE,
        cornerRadiusDp = 8f,
        action = InAppAction.Close
    )

    @Test
    fun closeButtonForcedWhenNoButtons() {
        val view = FullscreenInAppView(activity, content(showCloseButton = false))
        assertNotNull("fullscreen without buttons must force-show the ✕", findCloseButton(view))
    }

    @Test
    fun closeButtonHiddenWhenButtonsPresent() {
        val view = FullscreenInAppView(activity, content(showCloseButton = false, buttons = listOf(button())))
        assertNull("buttons already close the in-app, ✕ obeys showCloseButton=false", findCloseButton(view))
    }

    @Test
    fun closeButtonClosesInApp() {
        var closed = false
        val view = FullscreenInAppView(activity, content(showCloseButton = true))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closed = true }
        }
        findCloseButton(view)!!.performClick()
        assertTrue(closed)
    }

    @Test
    fun closeButtonShownWhenFlagSetDespiteButtons() {
        val view = FullscreenInAppView(activity, content(showCloseButton = true, buttons = listOf(button())))
        assertNotNull("explicit showCloseButton=true must win even when buttons exist", findCloseButton(view))
    }

    // The screen is the card here, so the ✕ has no margin of its own to hide behind: a landscape nav
    // bar or cutout on the END edge would sit on top of it and eat the tap. Asserted on the laid-out
    // position, not on `lp.marginEnd`: marginEnd is a relative margin, `setMarginEnd` only re-arms it
    // for resolution, and nothing re-resolves it on a plain layout pass — the field reads right while
    // the button stays where it resolved at attach time. SDK 34, not the module's Robolectric default
    // of 21, or the bottom/side insets reach the listener zeroed.
    @Test
    @Config(sdk = [34])
    fun insetsMoveTheCloseButtonClearOfTheSystemBars() {
        val view = FullscreenInAppView(activity, content(showCloseButton = true))
        activity.setContentView(view)
        val close = findCloseButton(view)!!

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val end = InAppViewUtils.dp(activity, 9f) + 33
        assertEquals("the ✕ must actually sit inset from the END edge", 1080 - end, close.right)
        assertEquals(
            "and below the status bar",
            InAppViewUtils.dp(activity, 5f) + 22, close.top
        )
    }

    // Child order once the scrim is attached: image(0), scrim(1), column(2), ✕(3).
    private fun scrim(view: FullscreenInAppView): View = view.getChildAt(1)

    private fun column(view: FullscreenInAppView): LinearLayout = view.getChildAt(2) as LinearLayout

    // forceLayout: the scrim's height comes from the column's layout callback, and a plain
    // measure/layout on an already-laid-out tree is a no-op.
    private fun layOut(view: View, width: Int = 1080, height: Int = 1920) {
        view.forceLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
    }

    @Test
    fun scrimSitsBetweenTheImageAndTheTextColumn() {
        val view = FullscreenInAppView(activity, content(showCloseButton = true, buttons = listOf(button())))

        assertTrue("the image stays at the bottom of the stack", view.getChildAt(0) is ImageView)
        assertTrue("a gradient band covers it", scrim(view).background is GradientDrawable)
        assertFalse("a band that eats taps would swallow button clicks", scrim(view).isClickable)
        assertTrue("the text column draws over the darkening", view.getChildAt(2) is LinearLayout)
        assertEquals("and the ✕ over everything", 4, view.childCount)
    }

    @Test
    fun scrimRisesFortyEightDpAboveTheFullscreenText() {
        val view = FullscreenInAppView(activity, content(showCloseButton = true, buttons = listOf(button())))

        layOut(view)

        val column = column(view)
        assertTrue("an unlaid-out column would make this assertion meaningless", column.height > 0)
        assertEquals(
            "iOS: scrim.top = stack.top − 48 (PWFullscreenInAppView.swift:86)",
            column.height - column.paddingTop + InAppViewUtils.dp(activity, 48f),
            scrim(view).layoutParams.height
        )
        assertEquals("the column stays pinned to the bottom edge", 1920, column.bottom)
    }

    // The nav bar pads the column's bottom, making it taller; the band has to grow with it or the
    // rise shrinks by the inset and the first line of text creeps back onto bare image. SDK 34, not
    // the module's Robolectric default of 21, or the insets reach the listener zeroed.
    @Test
    @Config(sdk = [34])
    fun scrimGrowsWithTheBottomInset() {
        val view = FullscreenInAppView(activity, content(showCloseButton = true, buttons = listOf(button())))
        activity.setContentView(view)
        layOut(view)
        val before = scrim(view).layoutParams.height

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 44))
                .build()
        )
        layOut(view)

        assertEquals(
            "the band must cover the column the nav bar just made taller",
            before + 44,
            scrim(view).layoutParams.height
        )
    }
}
