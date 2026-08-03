package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.pushwoosh.inapp.ui.animation.ReduceMotionUtil
import com.pushwoosh.inapp.ui.model.CarouselContent
import com.pushwoosh.inapp.ui.model.CarouselItem
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowGradientDrawable
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class CarouselInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    // The dim moved out of the root's background into a child of its own: its alpha animates
    // independently of the card's scale, and the backdrop tap moved with it.
    @Test
    fun tappingBackdropRequestsClose() {
        val view = carousel(item())
        val closes = countCloses(view)

        dim(view).performClick()

        assertEquals("tapping the dim must close the carousel", 1, closes[0])
        assertFalse("the root must not be a second, overlapping tap target", view.isClickable)
    }

    @Test
    fun closeButtonRidesTheCard() {
        val view = carousel(item())
        val close = findCloseButton(view)

        assertNotNull("carousel has no guaranteed dismiss path — the ✕ is always shown", close)
        assertSame("iOS anchors the ✕ to the card, not to the screen", card(view), close!!.parent)
    }

    @Test
    fun closeButtonClosesCarousel() {
        val view = carousel(item())
        val closes = countCloses(view)

        findCloseButton(view)!!.performClick()

        assertEquals("tapping the ✕ must close the carousel", 1, closes[0])
    }

    // GradientDrawable.getCornerRadius() is API 24 and this module's Robolectric default is 21.
    @Test
    @Config(sdk = [34])
    fun cardIsRoundedBlackAndClipsContent() {
        val card = card(carousel(item()))
        val background = card.background as GradientDrawable

        assertEquals(dp(24f).toFloat(), background.cornerRadius, 0.5f)
        assertEquals(Color.BLACK, Shadow.extract<ShadowGradientDrawable>(background).lastSetColor)
        assertTrue("the card trims the full-bleed image instead of sitting behind it", card.clipToOutline)
    }

    @Test
    fun cardKeepsIosSideMarginsAndSizesItself() {
        val view = carousel(item())
        val lp = card(view).layoutParams as FrameLayout.LayoutParams

        assertEquals(dp(22f), lp.leftMargin)
        assertEquals(dp(22f), lp.rightMargin)
        assertEquals(Gravity.CENTER, lp.gravity)
        // MATCH_PARENT would reach onMeasure as EXACTLY and kill the clamp: the card learns the
        // available height only from the AT_MOST a FrameLayout gives a WRAP_CONTENT child.
        assertEquals(FrameLayout.LayoutParams.WRAP_CONTENT, lp.height)
        assertTrue(
            "the card swallows taps that missed the slide, or it would close itself through the dim",
            card(view).isClickable
        )
    }

    @Test
    fun cardSizeFollowsIosAspectWhenHeightIsFree() {
        assertEquals(1000 to 1320, carouselCardSize(1000, 5000))
        assertEquals("0 means the height is unbounded (UNSPECIFIED)", 1000 to 1320, carouselCardSize(1000, 0))
    }

    // Our deliberate divergence: iOS's fixed heightAnchor lets the card overflow the screen, we
    // narrow the width instead — slides keep their aspect and the ✕ and dots stay on screen.
    @Test
    fun cardSizeNarrowsInsteadOfSquashingWhenHeightIsTight() {
        assertEquals(500 to 660, carouselCardSize(1000, 660))
    }

    // The aspect is recomputed on every measure, not once from displayMetrics: InAppOverlayActivity
    // declares configChanges="orientation|...", so it is not recreated and the view is not rebuilt.
    @Test
    fun measuredCardHoldsTheAspectRatioInsideItsMargins() {
        val view = carousel(item(), item())

        view.measure(
            MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY)
        )

        val card = card(view)
        assertEquals(1080 - 2 * dp(22f), card.measuredWidth)
        assertEquals(1.32f, card.measuredHeight.toFloat() / card.measuredWidth, 0.01f)
    }

    // The clamp wired through onMeasure, not just through carouselCardSize: the available height
    // reaches the card as the AT_MOST its FrameLayout parent hands a WRAP_CONTENT child, already
    // short of the margins. This is the landscape/rotation case the e2e check screenshots.
    @Test
    fun measuredCardNarrowsWhenTheScreenIsTooShortForTheAspect() {
        val view = carousel(item(), item())

        view.measure(
            MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY)
        )

        val card = card(view)
        val availableHeight = 1080 - 2 * dp(22f)
        assertEquals("the card must fit the short side, not overflow it", availableHeight, card.measuredHeight)
        assertEquals((availableHeight / 1.32f).roundToInt(), card.measuredWidth)
        assertTrue(
            "the width narrows below what was available — the aspect is kept, not squashed",
            card.measuredWidth < 1920 - 2 * dp(22f)
        )
    }

    // Only the spec *mode* may switch the clamp off: ViewGroup.getChildMeasureSpec still passes the
    // remaining size along with UNSPECIFIED, so reading the size alone would clamp the card to a
    // height no parent ever asked for.
    @Test
    fun cardTreatsAnUnspecifiedHeightAsUnbounded() {
        val card = card(carousel(item()))

        card.measure(
            MeasureSpec.makeMeasureSpec(1000, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(500, MeasureSpec.UNSPECIFIED)
        )

        assertEquals(1320, card.measuredHeight)
        assertEquals(1000, card.measuredWidth)
    }

    @Test
    fun dotsSitInsideTheCardAboveItsBottom() {
        val view = carousel(item(), item())
        val dots = card(view).getChildAt(1) as LinearLayout
        val lp = dots.layoutParams as FrameLayout.LayoutParams

        assertSame("iOS keeps the UIPageControl inside the card", card(view), dots.parent)
        assertEquals(2, dots.childCount)
        assertEquals(dp(12f), lp.bottomMargin)
        assertEquals(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, lp.gravity)
    }

    @Test
    fun dotsHiddenForSinglePage() {
        assertEquals(
            "parity with hidesForSinglePage",
            View.GONE,
            card(carousel(item())).getChildAt(1).visibility
        )
    }

    // SDK 34 rather than the default 21: on 21 WindowInsetsCompat reaches the listener with the
    // bottom inset zeroed, and the bottom assert would pass for the wrong reason.
    @Test
    @Config(sdk = [34])
    fun insetsPadTheCardAndLeaveTheCloseButtonAlone() {
        val view = carousel(item())
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )

        // The bigger inset of each axis, on both sides of that axis: a centered FrameLayout child is
        // offset by the whole leftMargin − rightMargin, so asymmetric margins would move the card
        // instead of insetting it.
        val lp = card(view).layoutParams as FrameLayout.LayoutParams
        assertEquals(dp(22f) + 33, lp.leftMargin)
        assertEquals(dp(22f) + 33, lp.rightMargin)
        assertEquals(dp(22f) + 44, lp.topMargin)
        assertEquals(dp(22f) + 44, lp.bottomMargin)

        val closeLp = findCloseButton(view)!!.layoutParams as FrameLayout.LayoutParams
        assertEquals("the ✕ rides the card and has no inset logic of its own", dp(5f), closeLp.marginEnd)
        assertEquals(dp(5f), closeLp.topMargin)
    }

    // Margins alone are not the contract — where the card ends up is. This is what the margin-only
    // assert above cannot see: with asymmetric insets the card must stay centered and clear of the
    // bars, not slide toward the edge that has no inset (and off screen once a one-sided inset
    // passes 2 × 22dp).
    @Test
    @Config(sdk = [34])
    fun cardStaysCenteredAndOnScreenUnderAOneSidedInset() {
        val view = carousel(item())
        val width = 1000
        val height = 600
        val navBar = 120

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, navBar, 0))
                .build()
        )
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)

        // ±1px: FrameLayout halves the leftover space with integer division.
        val card = card(view)
        assertTrue(
            "the card must stay centered horizontally, got ${card.left}..${card.right} of $width",
            abs((width - card.right) - card.left) <= 1
        )
        assertTrue(
            "and vertically, got ${card.top}..${card.bottom} of $height",
            abs((height - card.bottom) - card.top) <= 1
        )
        assertTrue("its trailing edge must clear the nav bar", card.right <= width - navBar)
        assertTrue("and it must not leave the screen", card.left >= 0)
    }

    // iOS fills the cell with the image (scaleAspectFill, clipped by the card). It used to be
    // FIT_CENTER inside a 24dp padding — letterboxed.
    @Test
    fun slideImageFillsTheCell() {
        val h = holder(carousel(item()))
        val lp = slideImage(h).layoutParams as FrameLayout.LayoutParams

        assertEquals(ImageView.ScaleType.CENTER_CROP, slideImage(h).scaleType)
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, lp.width)
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, lp.height)
        assertEquals("the image runs to the cell edges — no padding left", 0, slide(h).paddingLeft)
        assertEquals(0, slide(h).paddingBottom)
    }

    @Test
    fun slideTextOverlaysTheImageAtTheBottom() {
        val h = holder(carousel(item()))
        val lp = slideText(h).layoutParams as FrameLayout.LayoutParams

        assertEquals(dp(18f), lp.marginStart)
        assertEquals(dp(18f), lp.marginEnd)
        assertEquals(dp(36f), lp.bottomMargin)
        assertEquals(Gravity.BOTTOM, lp.gravity)
        assertTrue(
            "the text paints over the image and the scrim, so it comes last",
            slide(h).indexOfChild(slideText(h)) > slide(h).indexOfChild(slideScrim(h))
        )
    }

    @Test
    fun slideTitleIsTwentySpBold() {
        val title = slideTitle(holder(carousel(item())))
        val expected = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 20f, activity.resources.displayMetrics
        )

        // The weight is not asserted: Robolectric's Typeface shadow doesn't carry the style set by
        // setTypeface(null, BOLD), so isBold reads false for a bold TextView. Bold shows up in the
        // e2e screenshot instead.
        assertEquals(expected, title.textSize, 0.5f)
        assertEquals("the 2-line clamps are not lost", 2, title.maxLines)

        val message = slideMessage(holder(carousel(item())))
        val expectedMessage = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 14f, activity.resources.displayMetrics
        )
        assertEquals("iOS: 14 regular", expectedMessage, message.textSize, 0.5f)
        assertEquals(2, message.maxLines)
    }

    // The cell is built once with placeholder text in white, so every slide's own string and colour
    // now arrive at bind time. A dropped assignment would leave the whole carousel blank or white —
    // and the config's text colours are mandatory on both platforms.
    @Test
    fun slideTextTakesItsStringAndColourFromTheModel() {
        val view = carousel(
            item(title = InAppText("Headline", Color.RED), message = InAppText("Body", Color.GREEN))
        )
        val h = holder(view)

        bind(view, h, 0)

        assertEquals("Headline", slideTitle(h).text.toString())
        assertEquals(
            "the slide's colour, not the white the cell was assembled with",
            Color.RED, slideTitle(h).currentTextColor
        )
        assertEquals("Body", slideMessage(h).text.toString())
        assertEquals(Color.GREEN, slideMessage(h).currentTextColor)
    }

    @Test
    fun scrimIsShownOnlyWhenThereIsText() {
        val view = carousel(item(), item(title = null, message = null))
        val h = holder(view)

        bind(view, h, 0)
        val lp = slideScrim(h).layoutParams as FrameLayout.LayoutParams
        assertEquals(View.VISIBLE, slideScrim(h).visibility)
        assertEquals(dp(InAppViewUtils.BOTTOM_SCRIM_HEIGHT_DP), lp.height)
        assertEquals(Gravity.BOTTOM, lp.gravity)

        bind(view, h, 1)
        assertEquals("iOS doesn't darken a slide with no text", View.GONE, slideScrim(h).visibility)
    }

    // C10: item.title?.let let "" through and left an empty TextView taking up a line. A blank
    // string is an absence, exactly like isHidden on the iOS label (and the scrim goes out too).
    @Test
    fun blankTextIsTreatedAsAbsent() {
        val view = carousel(item(title = InAppText("   ", Color.WHITE), message = null))
        val h = holder(view)

        bind(view, h, 0)

        assertEquals("a whitespace title must not take a line", View.GONE, slideTitle(h).visibility)
        assertEquals("and must not light up the scrim", View.GONE, slideScrim(h).visibility)
    }

    // The other direction, and the one a fresh holder cannot check: labels and scrim start out
    // visible, so only a holder that a text-less slide has already hidden proves that bind turns
    // them back on. Without it a single blank slide would strip the text off every slide after it.
    @Test
    fun recycledSlideRestoresWhatABlankSlideHid() {
        val view = carousel(item(title = null, message = null), item())
        val h = holder(view)

        bind(view, h, 0)
        bind(view, h, 1)

        assertEquals(View.VISIBLE, slideTitle(h).visibility)
        assertEquals(View.VISIBLE, slideMessage(h).visibility)
        assertEquals("the scrim comes back with the text it protects", View.VISIBLE, slideScrim(h).visibility)
    }

    // On iOS this gap is UIStackView.spacing, which applies between items: a message with no title
    // must not be pushed down (same logic as the banner's B6).
    @Test
    fun messageGapAppliesOnlyBelowTitle() {
        val view = carousel(item(), item(title = null))
        val h = holder(view)

        bind(view, h, 0)
        assertEquals(dp(3f), (slideMessage(h).layoutParams as LinearLayout.LayoutParams).topMargin)

        bind(view, h, 1)
        assertEquals(
            "nothing sits above the message — no gap either", 0,
            (slideMessage(h).layoutParams as LinearLayout.LayoutParams).topMargin
        )
    }

    // The cell's shape is fixed, so it is built once in onCreateViewHolder: bind only sets values,
    // instead of the former removeAllViews + rebuild on every swipe.
    @Test
    fun rebindReusesTheSlideViews() {
        val view = carousel(item(), item())
        val h = holder(view)

        bind(view, h, 0)
        val image = slideImage(h)
        bind(view, h, 1)

        assertSame(image, slideImage(h))
        assertEquals(3, slide(h).childCount)
    }

    @Test
    fun recycledSlideWithoutActionDropsStaleClickListener() {
        val actions = mutableListOf<InAppAction>()
        val view = carousel(
            item(action = InAppAction.Url("https://a.example")),
            item(action = null)
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

    // iOS animateIn(card:backdrop:): the card scales up from 0.88 while the backdrop fades on its
    // own. The scheduler is paused, or Robolectric runs the animation to completion inside animateIn().
    @Test
    fun entranceFadesTheBackdropAndScalesTheCard() {
        val view = carousel(item())
        activity.setContentView(view)
        val scheduler = Robolectric.getForegroundThreadScheduler()

        scheduler.pause()
        view.animateIn()

        assertEquals("the dim fades on its own", 0f, dim(view).alpha, 0.001f)
        assertEquals("the card enters from iOS's 0.88", 0.88f, card(view).scaleX, 0.001f)
        assertEquals(0.88f, card(view).scaleY, 0.001f)
        assertEquals("the root as a whole is no longer animated", 1f, view.alpha, 0.001f)
        scheduler.unPause()
    }

    // The card's scale is new to the carousel, so the reduce-motion flag has to reach it: a
    // hardcoded false would scale the card for users who asked the system for no motion.
    @Test
    fun entranceUnderReduceMotionFadesTheCardWithoutScalingIt() {
        Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        // Guards the fixture: with reduce motion off this would be a copy of the test above.
        assertTrue(ReduceMotionUtil.isReduceMotionEnabled(activity))
        val view = carousel(item())
        // The flag is captured in the constructor, so the scale can go back to normal now — left at
        // 0 every animator jumps straight to its end value and no entrance state is observable.
        Settings.Global.putFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        activity.setContentView(view)
        val scheduler = Robolectric.getForegroundThreadScheduler()

        scheduler.pause()
        view.animateIn()

        assertEquals("no scale under reduce motion", 1f, card(view).scaleX, 0.001f)
        assertEquals(1f, card(view).scaleY, 0.001f)
        assertEquals("the card still fades in", 0f, card(view).alpha, 0.001f)
        scheduler.unPause()
    }

    // The exit animates the card, not the root: the root's alpha must stay untouched, or the dim
    // would fade twice over. That onEnd fires exactly once (it rides the card only; the dim's
    // fadeOut gets a no-op) is *not* asserted anywhere in unit tests: a child view's exit animation
    // never runs its frames under Robolectric, and the Activity route that would complete it is
    // closed for the carousel — ViewPager2 throws inside an Activity driven to visible(). It is an
    // e2e check (a single close, no double dismiss).
    @Test
    fun exitLeavesTheRootAlphaAlone() {
        val view = carousel(item())
        activity.setContentView(view)

        view.animateOut {}

        assertEquals("the root as a whole is no longer animated", 1f, view.alpha, 0.001f)
    }

    private fun item(
        title: InAppText? = InAppText("Title", Color.WHITE),
        message: InAppText? = InAppText("Body", Color.WHITE),
        action: InAppAction? = null
    ) = CarouselItem(null, title, message, action)

    private fun carousel(vararg items: CarouselItem) =
        CarouselInAppView(activity, CarouselContent(items.toList(), showCloseButton = false))

    private fun dim(view: CarouselInAppView): View = view.getChildAt(0)

    private fun card(view: CarouselInAppView) = view.getChildAt(1) as FrameLayout

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    @Suppress("UNCHECKED_CAST")
    private fun adapter(view: CarouselInAppView) =
        findPager(view)!!.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>

    private fun holder(view: CarouselInAppView) = adapter(view).createViewHolder(FrameLayout(activity), 0)

    private fun bind(view: CarouselInAppView, holder: RecyclerView.ViewHolder, position: Int) =
        adapter(view).bindViewHolder(holder, position)

    private fun slide(holder: RecyclerView.ViewHolder) = holder.itemView as FrameLayout

    private fun slideImage(holder: RecyclerView.ViewHolder) = slide(holder).getChildAt(0) as ImageView

    private fun slideScrim(holder: RecyclerView.ViewHolder): View = slide(holder).getChildAt(1)

    private fun slideText(holder: RecyclerView.ViewHolder) = slide(holder).getChildAt(2) as LinearLayout

    private fun slideTitle(holder: RecyclerView.ViewHolder) = slideText(holder).getChildAt(0) as TextView

    private fun slideMessage(holder: RecyclerView.ViewHolder) = slideText(holder).getChildAt(1) as TextView

    private fun countCloses(view: CarouselInAppView): IntArray {
        val closes = intArrayOf(0)
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}
            override fun onClose() { closes[0]++ }
        }
        return closes
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
