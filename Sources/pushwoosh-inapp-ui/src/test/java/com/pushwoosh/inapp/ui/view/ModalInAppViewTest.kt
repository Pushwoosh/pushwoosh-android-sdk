package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.ModalContent
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
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
}
