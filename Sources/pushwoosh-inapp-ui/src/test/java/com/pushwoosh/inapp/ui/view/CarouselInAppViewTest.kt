package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class CarouselInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun tappingBackdropRequestsClose() {
        var closed = false
        val view = CarouselInAppView(
            activity,
            CarouselContent(listOf(CarouselItem(null, InAppText("A", Color.WHITE), null, null)), showCloseButton = false)
        )
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closed = true }
        }
        view.performClick()
        assertTrue("tapping the dimmed backdrop should close the carousel", closed)
    }

    @Test
    fun closeButtonAlwaysShownOnPagerFrame() {
        val view = CarouselInAppView(
            activity,
            CarouselContent(listOf(CarouselItem(null, InAppText("A", Color.WHITE), null, null)), showCloseButton = false)
        )
        val close = findCloseButton(view)
        assertNotNull("carousel has no guaranteed dismiss path — the ✕ is always shown", close)
        assertNotSame("✕ must sit on the pager frame, not on the screen root", view, close!!.parent)
    }

    @Test
    fun closeButtonClosesCarousel() {
        var closed = false
        val view = CarouselInAppView(
            activity,
            CarouselContent(listOf(CarouselItem(null, InAppText("A", Color.WHITE), null, null)), showCloseButton = false)
        )
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closed = true }
        }
        findCloseButton(view)!!.performClick()
        assertTrue("tapping the ✕ must close the carousel", closed)
    }

    @Test
    fun recycledSlideWithoutActionDropsStaleClickListener() {
        val actions = mutableListOf<InAppAction>()
        val view = CarouselInAppView(
            activity,
            CarouselContent(
                listOf(
                    CarouselItem(null, InAppText("A", Color.WHITE), null, InAppAction.Url("https://a.example")),
                    CarouselItem(null, InAppText("B", Color.WHITE), null, null)
                ),
                showCloseButton = false
            )
        )
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) { actions += action }
            override fun onClose() {}
        }

        @Suppress("UNCHECKED_CAST")
        val adapter = findPager(view)!!.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        val holder = adapter.createViewHolder(FrameLayout(activity), 0)

        adapter.bindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertEquals(listOf<InAppAction>(InAppAction.Url("https://a.example")), actions)

        adapter.bindViewHolder(holder, 1)
        holder.itemView.performClick()
        assertEquals("action-less slide on a recycled holder must not fire the neighbour's action", 1, actions.size)
        assertFalse("action-less slide must not stay clickable", holder.itemView.isClickable)
    }

    private fun findPager(root: View): ViewPager2? {
        if (root is ViewPager2) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findPager(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
