package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.ModalContent
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
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
class ModalInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun firstImageView(view: View): ImageView? {
        if (view is ImageView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                firstImageView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    // imageUrl = "" builds the content ImageView (proving minHeight is set) while load("") hits
    // the early return, so Glide is never invoked in the unit test.
    @Test
    fun modalImageReservesHeight() {
        val content = ModalContent(
            backgroundColor = Color.WHITE,
            title = null,
            message = null,
            imageUrl = "",
            showCloseButton = false,
            buttons = emptyList(),
            dimsBackground = false
        )
        val view = ModalInAppView(activity, content)
        val image = firstImageView(view)
        assertNotNull("modal should build an ImageView when imageUrl is present", image)
        assertTrue("image must reserve height so a broken image can't collapse the card", image!!.minimumHeight > 0)
    }

    private fun modalContent(showCloseButton: Boolean, buttons: List<InAppButton> = emptyList()) = ModalContent(
        backgroundColor = Color.WHITE,
        title = InAppText("Title", Color.BLACK),
        message = null,
        imageUrl = null,
        showCloseButton = showCloseButton,
        buttons = buttons,
        dimsBackground = false
    )

    private fun button() = InAppButton(
        text = InAppText("OK", Color.WHITE),
        backgroundColor = Color.BLUE,
        borderColor = Color.BLUE,
        cornerRadiusDp = 8f,
        action = InAppAction.Close
    )

    // Regression for the tap-through bug: an unclickable card let a body tap fall to the next
    // sibling under the touch point — the dim backdrop's tap-to-dismiss (modal closed by tapping
    // itself) or, on a floating modal, the app UI underneath.
    @Test
    fun modalCardSwallowsBodyTaps() {
        val dimmed = ModalInAppView(activity, modalContent(showCloseButton = false).copy(dimsBackground = true))
        assertTrue("card over dim backdrop must swallow taps", dimmed.getChildAt(1).isClickable)

        val floating = ModalInAppView(activity, modalContent(showCloseButton = false))
        assertTrue("floating card must not leak taps into the app", floating.getChildAt(0).isClickable)
    }

    @Test
    fun closeButtonForcedWhenNoButtons() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = false))
        assertNotNull("modal without buttons must force-show the ✕ (no other dismiss path)", findCloseButton(view))
    }

    @Test
    fun closeButtonHiddenWhenButtonsPresent() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = false, buttons = listOf(button())))
        assertNull("buttons already close the modal, ✕ obeys showCloseButton=false", findCloseButton(view))
    }

    @Test
    fun closeButtonLivesInsideCard() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = true))
        val close = findCloseButton(view)!!
        assertNotSame("✕ must be anchored to the card, not to the screen root", view, close.parent)
    }

    @Test
    fun closeButtonClosesModal() {
        var closed = false
        val view = ModalInAppView(activity, modalContent(showCloseButton = true))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closed = true }
        }
        findCloseButton(view)!!.performClick()
        assertTrue(closed)
    }

    @Test
    fun closeButtonShownWhenFlagSetDespiteButtons() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = true, buttons = listOf(button())))
        assertNotNull("explicit showCloseButton=true must win even when buttons exist", findCloseButton(view))
    }

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    private fun card(view: ModalInAppView) = view.getChildAt(view.childCount - 1) as FrameLayout

    // The card carries the modal's screen margins, so the insets land on it: the ✕ and the buttons
    // live inside and stay clear of a landscape cutout with no inset logic of their own (unlike the
    // edge-to-edge sheet/fullscreen, whose ✕ takes the END inset itself). Same symmetric-max rule
    // as the carousel. SDK 34, not the module's Robolectric default of 21, or the bottom/side
    // insets reach the listener zeroed.
    @Test
    @Config(sdk = [34])
    fun insetsPadTheCardAndLeaveTheCloseButtonAlone() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = true))
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )

        val lp = card(view).layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(28f) + 33, lp.leftMargin)
        assertEquals(dp(28f) + 33, lp.rightMargin)
        assertEquals(44, lp.topMargin)
        assertEquals(44, lp.bottomMargin)

        val closeLp = findCloseButton(view)!!.layoutParams as FrameLayout.LayoutParams
        assertEquals("the ✕ rides the card and has no inset logic of its own", dp(13f), closeLp.marginEnd)
        assertEquals(dp(13f), closeLp.topMargin)
    }

    // Margins alone are not the contract — where the card ends up is: a centered FrameLayout child
    // is offset by the *whole* leftMargin − rightMargin, so an asymmetric inset would slide the card
    // toward the uninset edge (its ✕ was partially inside a landscape cutout before the card took
    // the insets at all).
    @Test
    @Config(sdk = [34])
    fun cardStaysCenteredAndClearOfAOneSidedCutout() {
        val view = ModalInAppView(activity, modalContent(showCloseButton = true))
        activity.setContentView(view)
        val width = 1000
        val height = 600
        val cutout = 120

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, 0, cutout, 0))
                .build()
        )
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)

        // ±1px: FrameLayout halves the leftover space with integer division.
        val card = card(view)
        assertTrue(
            "the card must stay centered horizontally, got ${card.left}..${card.right} of $width",
            abs((width - card.right) - card.left) <= 1
        )
        assertTrue("its trailing edge must clear the cutout", card.right <= width - cutout)
        assertTrue("and it must keep its own margin on the uninset edge", card.left >= dp(28f))
    }
}
