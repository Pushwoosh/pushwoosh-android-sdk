package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowGradientDrawable

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InAppViewUtilsTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun makeTextClampsWhenMaxLinesPositive() {
        val tv = InAppViewUtils.makeText(activity, "long text", Color.WHITE, 14f, false, maxLines = 2)
        assertEquals(2, tv.maxLines)
        assertEquals(TextUtils.TruncateAt.END, tv.ellipsize)
    }

    @Test
    fun makeTextLeavesEllipsizeNullByDefault() {
        val tv = InAppViewUtils.makeText(activity, "long text", Color.WHITE, 14f, false)
        assertNull(tv.ellipsize)
    }

    private fun button(
        textColor: Int = Color.WHITE,
        background: Int = Color.BLUE,
        border: Int = Color.RED
    ) = InAppButton(
        text = InAppText("Tap", textColor),
        backgroundColor = background,
        borderColor = border,
        cornerRadiusDp = 8f,
        action = InAppAction.Close
    )

    @Test
    fun buttonUsesTextColorFromModel() {
        val b = InAppViewUtils.makeButton(activity, button(textColor = Color.GREEN)) {}
        assertEquals(Color.GREEN, b.currentTextColor)
    }

    @Test
    fun buttonFillIsBackgroundColor() {
        val b = InAppViewUtils.makeButton(activity, button(background = Color.BLUE)) {}
        val bg = Shadow.extract<ShadowGradientDrawable>(b.background)
        assertEquals(Color.BLUE, bg.lastSetColor)
    }

    @Test
    fun buttonHasBorderInBorderColor() {
        val b = InAppViewUtils.makeButton(activity, button(border = Color.RED)) {}
        val bg = Shadow.extract<ShadowGradientDrawable>(b.background)
        assertTrue("button must have a border stroke", bg.strokeWidth > 0)
        assertEquals(Color.RED, bg.strokeColor)
    }

    @Test
    fun buttonDispatchesActionOnClick() {
        var dispatched: InAppAction? = null
        val b = InAppViewUtils.makeButton(activity, button()) { dispatched = it }
        b.performClick()
        assertEquals(InAppAction.Close, dispatched)
    }

    @Test
    fun cardCloseButtonWrapsChipInInset() {
        val b = InAppViewUtils.makeCardCloseButton(activity) {}
        assertTrue("visible chip must be inset from the 48dp touch target", b.background is InsetDrawable)
        assertEquals("Close", b.contentDescription)
    }

    @Test
    fun cardCloseButtonDispatchesClick() {
        var clicked = false
        val b = InAppViewUtils.makeCardCloseButton(activity) { clicked = true }
        b.performClick()
        assertTrue(clicked)
    }
}
