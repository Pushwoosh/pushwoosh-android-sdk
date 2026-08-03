package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.StoriesContent
import com.pushwoosh.inapp.ui.model.StoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class StoriesInAppViewTest {

    private lateinit var activity: Activity
    private val dispatched = mutableListOf<InAppAction>()
    private val listener = object : InAppTemplateView.Listener {
        override fun onAction(action: InAppAction) { dispatched.add(action) }
        override fun onClose() {}
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun button(label: String, action: InAppAction) = InAppButton(
        text = InAppText(label, Color.WHITE),
        backgroundColor = Color.BLUE,
        borderColor = Color.BLUE,
        cornerRadiusDp = 8f,
        action = action
    )

    private fun storyItem(vararg buttons: InAppButton) = StoryItem(
        imageUrl = null,
        title = InAppText("Title", Color.WHITE),
        message = InAppText("Message", Color.WHITE),
        buttons = buttons.toList(),
        durationMs = 5_000L
    )

    // animateIn() lays out the first slide (incl. its buttons) synchronously.
    private fun showStories(vararg items: StoryItem): StoriesInAppView {
        val view = StoriesInAppView(activity, StoriesContent(items.toList(), loops = false, showCloseButton = true))
        view.listener = listener
        view.animateIn()
        return view
    }

    // A CTA is a clickable TextView whose contentDescription is not "Close" (title/message are
    // non-clickable; the close button carries "Close").
    private fun findCta(view: View): TextView? {
        if (view is TextView && view.isClickable && view.contentDescription != "Close") return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findCta(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun tapForward(view: StoriesInAppView) {
        val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1000f, 100f, 0)
        val up = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_UP, 1000f, 100f, 0)
        view.onTouchEvent(down)
        view.onTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    @Test
    fun rendersCtaWhenButtonPresent() {
        val view = showStories(storyItem(button("Shop", InAppAction.Url("app://sale"))))
        val cta = findCta(view)
        assertNotNull("CTA should be rendered", cta)
        assertEquals("Shop", cta!!.text.toString())
    }

    @Test
    fun rendersNoCtaWhenButtonsEmpty() {
        val view = showStories(storyItem())
        assertNull("no buttons means no CTA", findCta(view))
    }

    @Test
    fun tappingCtaDispatchesUrlAction() {
        val view = showStories(storyItem(button("Shop", InAppAction.Url("app://sale"))))
        findCta(view)!!.performClick()
        assertEquals(listOf<InAppAction>(InAppAction.Url("app://sale")), dispatched)
    }

    @Test
    fun clearsCtaWhenAdvancingToSlideWithoutButtons() {
        val view = showStories(
            storyItem(button("Shop", InAppAction.Url("app://sale"))),
            storyItem()
        )
        assertNotNull("first slide should have a CTA", findCta(view))
        tapForward(view)
        assertNull("advancing to a button-less slide must clear the CTA", findCta(view))
    }

    @Test
    fun tappingCtaAfterAdvancingDispatchesCurrentSlideAction() {
        val view = showStories(
            storyItem(button("Shop", InAppAction.Url("app://sale"))),
            storyItem(button("Buy", InAppAction.Url("app://buy")))
        )
        tapForward(view)
        val cta = findCta(view)
        assertEquals("Buy", cta!!.text.toString())
        cta.performClick()
        assertEquals(listOf<InAppAction>(InAppAction.Url("app://buy")), dispatched)
    }

    @Test
    fun tapIsIgnoredWhilePaused() {
        val view = showStories(
            storyItem(button("Shop", InAppAction.Url("app://sale"))),
            storyItem(button("Buy", InAppAction.Url("app://buy")))
        )
        view.pauseTimer()
        tapForward(view)
        assertEquals("Shop", findCta(view)!!.text.toString())
    }

    @Test
    fun tapNavigatesAgainAfterRelease() {
        val view = showStories(
            storyItem(button("Shop", InAppAction.Url("app://sale"))),
            storyItem(button("Buy", InAppAction.Url("app://buy")))
        )
        view.pauseTimer()
        tapForward(view) // ignored while paused; its ACTION_UP resumes the timer
        assertEquals("Shop", findCta(view)!!.text.toString())
        tapForward(view) // resumed → this tap advances
        assertEquals("Buy", findCta(view)!!.text.toString())
    }

    @Test
    fun closeButtonFollowsShowCloseButtonFlag() {
        val shown = StoriesInAppView(activity, StoriesContent(listOf(storyItem()), loops = false, showCloseButton = true))
        val hidden = StoriesInAppView(activity, StoriesContent(listOf(storyItem()), loops = false, showCloseButton = false))
        assertNotNull("stories with showCloseButton=true must show the ✕", findCloseButton(shown))
        assertNull("stories has swipe-down to dismiss — the ✕ stays opt-in", findCloseButton(hidden))
    }

    @Test
    fun closeButtonSitsInTopBarRow() {
        val view = showStories(storyItem())
        val close = findCloseButton(view)!!
        assertNotSame("✕ must sit in the topBar row under the progress bar, not on the root", view, close.parent)
    }

    @Test
    fun closeButtonClosesStories() {
        var closed = false
        val view = showStories(storyItem())
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closed = true }
        }
        findCloseButton(view)!!.performClick()
        assertTrue("tapping the ✕ must close the stories", closed)
    }

    // Child order once the scrim is attached: image(0), topBar(1), scrim(2), bottomBar(3).
    private fun scrim(view: StoriesInAppView): View = view.getChildAt(2)

    private fun bottomBar(view: StoriesInAppView): LinearLayout = view.getChildAt(3) as LinearLayout

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    // forceLayout: the scrim's height comes from the bottom bar's layout callback, and a plain
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
    fun scrimSitsBetweenTheImageAndTheBottomBar() {
        val view = showStories(storyItem())

        assertTrue(
            "stories needs the same readability band as fullscreen",
            scrim(view).background is GradientDrawable
        )
        assertFalse("tap zones and CTAs must keep receiving touches", scrim(view).isClickable)
        assertTrue("the band stays under the text it protects", view.getChildAt(3) is LinearLayout)
    }

    @Test
    fun scrimRisesFortyFourDpAboveTheStoriesText() {
        val view = showStories(storyItem(button("Shop", InAppAction.Url("app://sale"))))

        layOut(view)

        val bar = bottomBar(view)
        assertTrue("an unlaid-out column would make this assertion meaningless", bar.height > 0)
        assertEquals(
            "iOS: scrim.top = bottomStack.top − 44 (PWStoriesInAppView.swift:119)",
            bar.height - bar.paddingTop + dp(44f),
            scrim(view).layoutParams.height
        )
    }

    @Test
    fun scrimFollowsTheSlideWithFewerButtons() {
        val view = showStories(
            storyItem(button("Shop", InAppAction.Url("app://sale")), button("Later", InAppAction.Close)),
            storyItem()
        )
        layOut(view)
        val tall = scrim(view).layoutParams.height

        tapForward(view)
        layOut(view)
        val short = scrim(view).layoutParams.height

        assertTrue("a slide without buttons has a shorter column — the band must shrink ($tall → $short)", short < tall)
        val bar = bottomBar(view)
        assertEquals(bar.height - bar.paddingTop + dp(44f), short)
    }
}
