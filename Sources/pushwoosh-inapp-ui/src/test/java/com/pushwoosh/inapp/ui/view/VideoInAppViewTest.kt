package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pushwoosh.inapp.ui.model.InAppAction
import com.pushwoosh.inapp.ui.model.InAppButton
import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppText
import com.pushwoosh.inapp.ui.model.VideoContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo
import org.robolectric.shadows.util.DataSource

private const val VIDEO_URL = "https://x/v.mp4"

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class VideoInAppViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(VIDEO_URL), MediaInfo(5_000, 0))
    }

    /** The engine builds its player only once a surface arrives, and Robolectric never hands a
     *  TextureView one — so it is delivered by hand, the way InAppVideoPlayerViewTest does it. */
    private fun startPlayback(view: VideoInAppView) {
        val texture = (view.getChildAt(0) as InAppVideoPlayerView).getChildAt(0) as TextureView
        texture.surfaceTextureListener!!.onSurfaceTextureAvailable(SurfaceTexture(0), 1080, 1920)
        ShadowLooper.idleMainLooper()
    }

    private fun content(
        showCloseButton: Boolean = true,
        buttons: List<InAppButton> = emptyList(),
        muted: Boolean = true,
        title: InAppText? = InAppText("The reveal", Color.WHITE),
        message: InAppText? = InAppText("Watch it move", Color.WHITE)
    ) = VideoContent(
        videoUrl = VIDEO_URL,
        posterUrl = null,
        fallbackImageUrl = null,
        title = title,
        message = message,
        buttons = buttons,
        loops = true,
        muted = muted,
        showCloseButton = showCloseButton
    )

    private fun button() = InAppButton(
        text = InAppText("Shop", Color.WHITE),
        backgroundColor = Color.BLUE,
        borderColor = Color.BLUE,
        cornerRadiusDp = 14f,
        action = InAppAction.Close
    )

    // Child order once the scrim is attached: player(0), scrim(1), column(2), mute(3), ✕(4).
    private fun scrim(view: VideoInAppView): View = view.getChildAt(1)

    private fun column(view: VideoInAppView): LinearLayout = view.getChildAt(2) as LinearLayout

    private fun muteChip(view: VideoInAppView): ImageView = view.getChildAt(3) as ImageView

    private fun topMarginOf(child: View) = (child.layoutParams as LinearLayout.LayoutParams).topMargin

    private fun dp(value: Float) = InAppViewUtils.dp(activity, value)

    private fun layOut(view: View, width: Int = 1080, height: Int = 1920) {
        view.forceLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
    }

    @Test
    fun stacksThePlayerUnderTheScrimAndTheControlsOnTop() {
        val view = VideoInAppView(activity, content(buttons = listOf(button())))

        assertTrue("the video fills the screen", view.getChildAt(0) is InAppVideoPlayerView)
        assertTrue("a gradient band protects the copy", scrim(view).background is GradientDrawable)
        assertFalse("a band that eats taps would swallow CTA clicks", scrim(view).isClickable)
        assertTrue("the text column draws over the darkening", view.getChildAt(2) is LinearLayout)
        assertTrue("the mute chip sits above everything", muteChip(view).isClickable)
        assertEquals(5, view.childCount)
    }

    // The mute chip is not a way out of the in-app, so with no CTA the ✕ is the only one — same
    // forced-show rule as fullscreen (PWVideoInAppView.swift:90-91).
    @Test
    fun closeButtonForcedWhenNoButtons() {
        val view = VideoInAppView(activity, content(showCloseButton = false))
        assertNotNull("a video with neither CTA nor ✕ would be a trap", findCloseButton(view))
    }

    @Test
    fun closeButtonHiddenWhenButtonsPresent() {
        val view = VideoInAppView(activity, content(showCloseButton = false, buttons = listOf(button())))
        assertNull("CTAs already close the in-app, ✕ obeys showCloseButton=false", findCloseButton(view))
    }

    @Test
    fun closeButtonClosesInApp() {
        var closed = false
        val view = VideoInAppView(activity, content(showCloseButton = true))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}

            override fun onClose() {
                closed = true
            }
        }

        findCloseButton(view)!!.performClick()

        assertTrue(closed)
    }

    @Test
    fun muteChipTogglesTheSoundWithoutClosingTheInApp() {
        var closed = false
        val view = VideoInAppView(activity, content(buttons = listOf(button())))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}

            override fun onClose() {
                closed = true
            }
        }
        val chip = muteChip(view)
        val mutedLabel = chip.contentDescription

        chip.performClick()

        assertNotEquals("the label follows the real player state", mutedLabel, chip.contentDescription)
        assertFalse("tapping mute must never dismiss the in-app", closed)

        chip.performClick()

        assertEquals("and toggles back", mutedLabel, chip.contentDescription)
    }

    // The screen is the card here, so both chips have to pay the status bar and their own side
    // inset. Asserted on the laid-out position, not on the margins: marginStart/marginEnd are
    // relative and nothing re-resolves them on a plain layout pass.
    @Test
    @Config(sdk = [34])
    fun insetsMoveBothChipsClearOfTheSystemBars() {
        val view = VideoInAppView(activity, content(showCloseButton = true))
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )
        layOut(view)

        val close = findCloseButton(view)!!
        assertEquals(1080 - (InAppViewUtils.dp(activity, 9f) + 33), close.right)
        assertEquals(InAppViewUtils.dp(activity, 5f) + 22, close.top)
        assertEquals(InAppViewUtils.dp(activity, 9f) + 11, muteChip(view).left)
        assertEquals(InAppViewUtils.dp(activity, 5f) + 22, muteChip(view).top)
    }

    @Test
    fun scrimRisesFortyEightDpAboveTheVideoText() {
        val view = VideoInAppView(activity, content(buttons = listOf(button())))

        layOut(view)

        val column = column(view)
        assertTrue("an unlaid-out column would make this assertion meaningless", column.height > 0)
        assertEquals(
            "iOS: scrim.top = stack.top − 48 (PWVideoInAppView.swift:77)",
            column.height - column.paddingTop + InAppViewUtils.dp(activity, 48f),
            scrim(view).layoutParams.height
        )
        assertEquals("the column stays pinned to the bottom edge", 1920, column.bottom)
    }

    // The one outcome the spec forbids: a video that cannot play and has no fallback would otherwise
    // leave a black screen, and with showClose=false plus CTAs there is no ✕ to escape through.
    @Test
    fun aDeadVideoWithoutAFallbackClosesTheInApp() {
        var closed = false
        val view = VideoInAppView(activity, content(showCloseButton = false, buttons = listOf(button())))
        view.listener = object : InAppTemplateView.Listener {
            override fun onAction(action: InAppAction) {}

            override fun onClose() {
                closed = true
            }
        }

        (view.getChildAt(0) as InAppVideoPlayerView).handlePlaybackFailure()

        assertTrue(closed)
    }

    // The CTA column is the part that would go under the gesture bar: the chips have their own
    // assertions above, but nothing else pins the bottom inset the column has to pay.
    @Test
    @Config(sdk = [34])
    fun insetsPadTheColumnClearOfTheGestureBar() {
        val view = VideoInAppView(activity, content(buttons = listOf(button())))
        activity.setContentView(view)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )
        layOut(view)

        val column = column(view)
        assertEquals(InAppViewUtils.dp(activity, 24f) + 11, column.paddingLeft)
        assertEquals(InAppViewUtils.dp(activity, 24f) + 33, column.paddingRight)
        assertEquals("iOS pins the stack 28 above the safe area", InAppViewUtils.dp(activity, 28f) + 44, column.paddingBottom)
    }

    // The engine must die at the start of the exit, not when the fade ends 220ms later — the audio
    // focus going back is the observable proof that the sound stopped with the tap.
    @Test
    fun exitTearsDownTheEngineBeforeTheFade() {
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val view = VideoInAppView(activity, content(muted = false))
        startPlayback(view)
        assertNull(shadowOf(audioManager).lastAbandonedAudioFocusListener)

        view.animateOut {}

        assertNotNull("the video must go silent immediately", shadowOf(audioManager).lastAbandonedAudioFocusListener)
    }

    // iOS spaces the stack by 8 throughout and inserts a custom 20 after the last text block, so the
    // wider gap belongs to the seam between copy and CTAs, not to the CTAs themselves
    // (PWVideoInAppView.swift:47,58-59).
    @Test
    fun theColumnKeepsTheIosGapsBetweenTextAndButtons() {
        val view = VideoInAppView(activity, content(buttons = listOf(button(), button())))

        val column = column(view)
        assertEquals("the title opens the column", 0, topMarginOf(column.getChildAt(0)))
        assertEquals(dp(8f), topMarginOf(column.getChildAt(1)))
        assertEquals("the seam between copy and CTAs is the wide one", dp(20f), topMarginOf(column.getChildAt(2)))
        assertEquals("consecutive CTAs are back to 8", dp(8f), topMarginOf(column.getChildAt(3)))
    }

    // A clip with no copy has nothing for the CTA to be separated from, and iOS's custom spacing
    // exists only after a text block — an unconditional 20 would open a gap under a scrim edge that
    // nothing fills.
    @Test
    fun withoutCopyTheFirstButtonOpensTheColumn() {
        val view = VideoInAppView(activity, content(title = null, message = null, buttons = listOf(button())))

        val column = column(view)
        assertEquals("only the CTA is in the column", 1, column.childCount)
        assertEquals(0, topMarginOf(column.getChildAt(0)))
    }

    // The mute chip's inset is a hand-written mirror of the ✕'s and carries both of its traps:
    // marginStart is logical while the insets are physical, and the direction resolves only at the
    // first measure — after the insets pass — so the view has to re-apply them when it lands.
    @Test
    @Config(sdk = [34])
    fun theMuteChipInsetFollowsAnRtlResolution() {
        // Without FLAG_SUPPORTS_RTL every resolution clamps to LTR, whatever layoutDirection says
        // — the library's test manifest carries no supportsRtl, real RTL hosts do.
        activity.applicationInfo.flags = activity.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val view = VideoInAppView(activity, content(showCloseButton = true))
        activity.setContentView(view)
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(11, 22, 33, 44))
                .build()
        )

        view.layoutDirection = View.LAYOUT_DIRECTION_RTL
        layOut(view)

        val lp = muteChip(view).layoutParams as FrameLayout.LayoutParams
        assertEquals("in RTL the START edge is insets.right", InAppViewUtils.dp(activity, 9f) + 33, lp.marginStart)
        assertEquals(
            "and the chip must actually sit there, mirrored to the right edge",
            1080 - (InAppViewUtils.dp(activity, 9f) + 33),
            muteChip(view).right
        )
    }

    // Video is a blocking template and travels through InAppOverlayActivity, which builds its view
    // via the factory: a missing branch there silently finishes the Activity and drops the message.
    @Test
    fun factoryBuildsTheVideoForTheActivityPath() {
        val built = InAppViewFactory.create(
            activity,
            InAppLayout.Video(content()),
            object : InAppTemplateView.Listener {
                override fun onAction(action: InAppAction) {}

                override fun onClose() {}
            }
        )

        assertTrue(built is VideoInAppView)
    }
}
