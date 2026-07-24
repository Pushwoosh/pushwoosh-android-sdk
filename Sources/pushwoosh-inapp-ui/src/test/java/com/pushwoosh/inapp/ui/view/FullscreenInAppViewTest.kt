package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import com.pushwoosh.inapp.ui.model.FullscreenContent
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
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
}
