package com.pushwoosh.inapp.ui.view

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo
import org.robolectric.shadows.util.DataSource

private const val VIDEO_URL = "https://x/v.mp4"
private const val DURATION_MS = 5_000

/** A clip whose preparation does not finish inside the surface callback, so the trip to the
 *  background and back fits inside it — the way a real network prepare behaves. */
private const val SLOW_URL = "https://x/slow.mp4"
private const val PREPARE_MS = 500

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InAppVideoPlayerViewTest {

    private lateinit var activity: Activity
    private lateinit var audioManager: AudioManager
    private var player: MediaPlayer? = null
    private var playerShadow: ShadowMediaPlayer? = null
    private var playersCreated = 0

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ShadowMediaPlayer.setCreateListener { mp, shadow ->
            player = mp
            playerShadow = shadow
            playersCreated++
        }
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(VIDEO_URL), MediaInfo(DURATION_MS, 0))
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(SLOW_URL), MediaInfo(DURATION_MS, PREPARE_MS))
    }

    // Child order: texture(0), poster(1), spinner(2).
    private fun poster(view: InAppVideoPlayerView) = view.getChildAt(1) as ImageView

    private fun spinner(view: InAppVideoPlayerView) = view.getChildAt(2)

    private fun texture(view: InAppVideoPlayerView) = view.getChildAt(0) as TextureView

    /** The one thing Robolectric will not do on its own: a TextureView there is never handed a real
     *  SurfaceTexture, so the callback the engine waits for is delivered by hand. Everything past it
     *  — MediaPlayer, prepareAsync, playback state — is the shadow player's own machinery, so the
     *  branches this unlocks are exercised for real rather than poked at through internals. */
    private fun deliverSurface(view: InAppVideoPlayerView) {
        texture(view).surfaceTextureListener!!.onSurfaceTextureAvailable(SurfaceTexture(0), 1080, 1920)
        ShadowLooper.idleMainLooper()
    }

    /** ShadowMediaPlayer can invoke the prepared, error, info and completion listeners but has no
     *  video-size one, so the engine's handler is called directly through its test seam rather than
     *  through MediaPlayer's private listener field. */
    private fun deliverVideoSize(view: InAppVideoPlayerView, width: Int, height: Int) =
        view.onVideoSize(width, height)

    private fun layOut(view: View, width: Int, height: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
    }

    @Test
    fun posterAndSpinnerHoldTheFrameWhileTheVideoLoads() {
        val view = InAppVideoPlayerView(activity)

        view.configure("https://x/v.mp4", "https://x/p.jpg", null, loop = true, muted = true)

        assertEquals(View.VISIBLE, poster(view).visibility)
        assertEquals(View.VISIBLE, spinner(view).visibility)
    }

    @Test
    fun withoutAPosterNothingCoversTheVideo() {
        val view = InAppVideoPlayerView(activity)

        view.configure("https://x/v.mp4", null, null, loop = true, muted = true)

        assertEquals(View.GONE, poster(view).visibility)
    }

    // A dead video with a fallback is still a shown in-app: the image takes over and the user keeps
    // the message. iOS switches the same image view to aspect-fill (PWInAppVideoPlayerView.swift:141-145).
    @Test
    fun failureWithAFallbackSwapsInTheImageAndKeepsTheInApp() {
        var failed = 0
        val view = InAppVideoPlayerView(activity)
        view.onFailed = { failed++ }
        view.configure("https://x/broken.mp4", null, "https://x/f.jpg", loop = true, muted = true)

        view.handlePlaybackFailure()

        assertEquals(View.VISIBLE, poster(view).visibility)
        assertEquals(ImageView.ScaleType.CENTER_CROP, poster(view).scaleType)
        assertEquals(View.GONE, spinner(view).visibility)
        assertEquals("a covered failure must not dismiss the in-app", 0, failed)
    }

    // Without a fallback there is nothing to look at — leaving a black screen up is the one outcome
    // the template must never produce.
    @Test
    fun failureWithoutAFallbackAsksTheHostToDismiss() {
        var failed = 0
        val view = InAppVideoPlayerView(activity)
        view.onFailed = { failed++ }
        view.configure("https://x/broken.mp4", null, null, loop = true, muted = true)

        view.handlePlaybackFailure()

        assertEquals(1, failed)
        assertEquals(View.GONE, spinner(view).visibility)
    }

    // Ducking someone's music is only acceptable while we actually make sound, so focus follows the
    // mute state — not the lifetime of the view.
    @Test
    fun soundTakesTransientDuckFocusAndMutingGivesItBack() {
        val view = InAppVideoPlayerView(activity)

        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        val request = shadowOf(audioManager).lastAudioFocusRequest
        assertNotNull("an unmuted video must ask for focus", request)
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, request.durationHint)
        assertNull("nothing abandoned yet", shadowOf(audioManager).lastAbandonedAudioFocusListener)

        view.isMuted = true

        assertNotNull("muting hands the focus back", shadowOf(audioManager).lastAbandonedAudioFocusListener)
    }

    // Preparing a network clip takes seconds, and a spinner makes no sound: focus taken in configure
    // would sit on the user's music through the whole wait — and through a load that may end in the
    // fallback image and never make a sound at all.
    @Test
    fun aClipStillLoadingDoesNotDuckTheUsersMusicYet() {
        val view = InAppVideoPlayerView(activity)

        view.configure(VIDEO_URL, null, null, loop = true, muted = false)

        assertNull(shadowOf(audioManager).lastAudioFocusRequest)
    }

    @Test
    fun mutedStartNeverTouchesTheUsersAudio() {
        val view = InAppVideoPlayerView(activity)

        view.configure("https://x/v.mp4", null, null, loop = true, muted = true)

        assertNull(shadowOf(audioManager).lastAudioFocusRequest)
        assertTrue(view.isMuted)
    }

    @Test
    fun mutingNotifiesTheHostSoTheIconStaysTruthful() {
        var notified = 0
        val view = InAppVideoPlayerView(activity)
        view.onMuteStateChanged = { notified++ }
        view.configure("https://x/v.mp4", null, null, loop = true, muted = true)

        view.isMuted = false
        view.isMuted = false

        assertEquals("only a real change is worth a redraw", 1, notified)
    }

    // Teardown is reached from two places (the template's exit animation and detach) and they
    // overlap on every normal dismissal.
    @Test
    fun teardownHandsTheAudioFocusBack() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        view.teardown()

        assertNotNull(shadowOf(audioManager).lastAbandonedAudioFocusListener)
    }

    // The view stays on screen and clickable through the 220ms exit fade, so the chip outlives the
    // engine. Taking focus in that window would duck the user's music with nobody left to hand it
    // back — teardown has already run and leaves early on a second call.
    @Test
    fun unmutingDuringTheExitAnimationDoesNotDuckTheUsersMusic() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(view)
        view.teardown()

        view.isMuted = false

        assertNull("nothing is going to play; the music must keep its volume", shadowOf(audioManager).lastAudioFocusRequest)
    }

    // Same window, other door: after a failure the fallback image is on screen with the chip still
    // on top of it. A picture makes no sound, so unmuting it must not cost the user their music.
    @Test
    fun unmutingAFailedVideoDoesNotDuckTheUsersMusic() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, "https://x/f.jpg", loop = true, muted = true)
        deliverSurface(view)
        playerShadow!!.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO)

        view.isMuted = false

        assertNull(shadowOf(audioManager).lastAudioFocusRequest)
    }

    // Focus can be taken from us mid-clip — a call, another app's video. Going silent, and saying so,
    // is what keeps the chip from offering to mute sound the user can no longer hear.
    @Test
    fun losingAudioFocusMutesTheVideoAndTellsTheHost() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)
        var notified = 0
        view.onMuteStateChanged = { notified++ }

        shadowOf(audioManager).lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertTrue(view.isMuted)
        assertEquals(1, notified)
    }

    // An incoming call takes the channel outright rather than ducking it — the same silence as a
    // permanent loss for as long as it lasts, so the chip must stop claiming sound.
    @Test
    fun aTransientLossToACallMutesTheVideo() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        shadowOf(audioManager)
            .lastAudioFocusRequest
            .listener
            .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        assertTrue(view.isMuted)
    }

    // Ducking is not losing: the system lowers our volume for a notification and hands the channel
    // straight back, so treating it as a loss would kill the sound for the rest of the clip.
    @Test
    fun aDuckingNeighbourLeavesTheSoundOn() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        shadowOf(audioManager)
            .lastAudioFocusRequest
            .listener
            .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertFalse(view.isMuted)
    }

    // The poster is a placeholder, not the show: once the clip is prepared it has to be uncovered and
    // actually started, or the in-app is a still image with a spinner burned into the middle.
    @Test
    fun playbackStartsWhenTheSurfaceArrives() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, "https://x/p.jpg", null, loop = true, muted = true)

        deliverSurface(view)

        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
        assertEquals(View.GONE, poster(view).visibility)
        assertEquals(View.GONE, spinner(view).visibility)
    }

    // `loop` is a contract field: dropped on the way to the player it turns a looping teaser into a
    // clip that plays once and then holds its last frame.
    @Test
    fun theLoopFlagReachesThePlayer() {
        val looping = InAppVideoPlayerView(activity)
        looping.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(looping)
        assertTrue("loop:true must keep the clip running", player!!.isLooping)

        val once = InAppVideoPlayerView(activity)
        once.configure(VIDEO_URL, null, null, loop = false, muted = true)
        deliverSurface(once)
        assertFalse("loop:false must play through once", player!!.isLooping)
    }

    // Both the contract's `muted` and the chip tap have to reach the player itself — the mute state
    // that only drives the icon is the one bug this template cannot afford.
    @Test
    fun theMuteStateDrivesThePlayerVolume() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(view)
        assertEquals("muted:true must reach the player, not just the icon", 0f, playerShadow!!.leftVolume, 0f)

        view.isMuted = false

        assertEquals(1f, playerShadow!!.leftVolume, 0f)
        assertEquals(1f, playerShadow!!.rightVolume, 0f)

        view.isMuted = true

        assertEquals(0f, playerShadow!!.leftVolume, 0f)
    }

    // iOS puts the spinner back on FailedToPlayToEndTime; the buffering pair is the Android
    // counterpart, and swapping the two constants leaves the wheel spinning over a playing clip.
    @Test
    fun bufferingBringsTheSpinnerBack() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(view)

        playerShadow!!.invokeInfoListener(MediaPlayer.MEDIA_INFO_BUFFERING_START, 0)
        assertEquals(View.VISIBLE, spinner(view).visibility)

        playerShadow!!.invokeInfoListener(MediaPlayer.MEDIA_INFO_BUFFERING_END, 0)
        assertEquals(View.GONE, spinner(view).visibility)
    }

    // The funnel's two outcomes are covered above; this is the wiring into it. An error the player
    // reports mid-play (a CDN that dies halfway) has to reach it, or the in-app sits on a black frame
    // with the user's music still ducked under silence.
    @Test
    fun aPlayerErrorRunsTheFailureFunnel() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, "https://x/f.jpg", loop = true, muted = false)
        deliverSurface(view)

        playerShadow!!.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO)

        assertEquals(View.VISIBLE, poster(view).visibility)
        assertEquals(ImageView.ScaleType.CENTER_CROP, poster(view).scaleType)
        assertNotNull(
            "a template that will never make sound again must stop ducking",
            shadowOf(audioManager).lastAbandonedAudioFocusListener
        )
        assertEquals(
            "a player in Error is as unusable as one that threw on open — hand the decoder back",
            ShadowMediaPlayer.State.END,
            playerShadow!!.state
        )
    }

    // A source the device cannot open throws straight out of setDataSource/prepareAsync instead of
    // arriving as onError (Robolectric refuses an unregistered one exactly the way a device refuses a
    // malformed url). Same outcome for the user, and the half-built player must not be left holding
    // the surface.
    @Test
    fun anUnopenableSourceReleasesThePlayerAndAsksForDismissal() {
        var failed = 0
        val view = InAppVideoPlayerView(activity)
        view.onFailed = { failed++ }
        view.configure("https://x/never-registered.mp4", null, null, loop = true, muted = true)

        deliverSurface(view)

        assertEquals(1, failed)
        assertEquals(
            "a throwing player is unusable and must be let go",
            ShadowMediaPlayer.State.END,
            playerShadow!!.state
        )
    }

    // The sound has to stop with the tap, not when the fade ends: a player left alive keeps playing
    // over whatever comes next.
    @Test
    fun teardownReleasesTheRunningPlayer() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)
        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)

        view.teardown()

        assertEquals(ShadowMediaPlayer.State.END, playerShadow!!.state)
    }

    // Teardown and the surface callbacks overlap on every dismissal; the `released` flag is what
    // stops a late surface from starting an invisible clip nobody can reach to stop.
    @Test
    fun aSurfaceArrivingAfterTeardownStartsNothing() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)

        view.teardown()
        deliverSurface(view)

        assertEquals(0, playersCreated)
    }

    // The Activity stays alive behind the home screen, so without this the clip plays — and sounds —
    // on top of whatever the user switched to.
    @Test
    fun theHostPauseStopsPlaybackAndTheReturnResumesIt() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        view.pauseForHost()
        assertEquals(ShadowMediaPlayer.State.PAUSED, playerShadow!!.state)

        view.resumeForHost()
        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
    }

    // Preparing a network clip takes seconds, so the app can reach the background before the player
    // is ready. The pause has already been and gone by then, so nothing would stop the prepared
    // clip from starting itself — out loud, over another app, and with loop:true forever.
    @Test
    fun aClipThatBecomesReadyInTheBackgroundDoesNotStartItself() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, "https://x/p.jpg", null, loop = true, muted = false)

        view.pauseForHost()
        deliverSurface(view)

        assertNotEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
        assertEquals("the poster holds the frame until playback really begins", View.VISIBLE, poster(view).visibility)

        view.resumeForHost()

        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
        assertEquals(View.GONE, poster(view).visibility)
    }

    // The whole trip to the background and back can fit inside a network prepare: the pause finds
    // nothing playing yet, so the resume revives nothing and returns early, and the clip is started
    // by onPrepared instead. The focus has to come with that start — otherwise an unmuted clip plays
    // at full volume on top of music that was never asked to step aside.
    @Test
    fun aClipPreparedAfterTheReturnFromTheBackgroundStillDucksTheUsersMusic() {
        val view = InAppVideoPlayerView(activity)
        view.configure(SLOW_URL, null, null, loop = true, muted = false)
        deliverSurface(view)
        assertEquals(ShadowMediaPlayer.State.PREPARING, playerShadow!!.state)

        view.pauseForHost()
        // ShadowAudioManager keeps the last request around after an abandon, so "focus was taken"
        // is only true if this is a *different* request than whatever the trip left behind.
        val beforeStart = shadowOf(audioManager).lastAudioFocusRequest
        view.resumeForHost()
        ShadowLooper.idleMainLooper(PREPARE_MS + 100L, TimeUnit.MILLISECONDS)

        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
        val request = shadowOf(audioManager).lastAudioFocusRequest
        assertNotNull("a clip that starts with no focus plays over the user's music", request)
        assertNotSame("the focus handed back on the way out has to be taken again", beforeStart, request)
    }

    @Test
    fun theHostPauseHandsTheUsersAudioBackForAsLongAsWeAreSilent() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = false)
        deliverSurface(view)

        view.pauseForHost()

        assertNotNull(shadowOf(audioManager).lastAbandonedAudioFocusListener)
    }

    // The surface belongs to the host window, not to this view: backgrounding the app destroys it
    // and returning brings a new one. Tearing down there would be a one-way latch — the user would
    // come back to a black screen with the text still on it and no way to restart the clip.
    @Test
    fun theSurfaceGoingAwayAndComingBackKeepsTheSameClip() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(view)
        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)

        texture(view).surfaceTextureListener!!.onSurfaceTextureDestroyed(SurfaceTexture(1))

        assertEquals(
            "losing the window surface must not release the decoder",
            ShadowMediaPlayer.State.STARTED,
            playerShadow!!.state
        )

        deliverSurface(view)

        assertEquals("the surviving player re-binds instead of a second one being built", 1, playersCreated)
        assertEquals(ShadowMediaPlayer.State.STARTED, playerShadow!!.state)
    }

    // A failure releases the player but leaves the in-app up with the fallback image on it, and the
    // window surface keeps coming and going with the host. Without the gate the returning surface
    // finds no player and builds a second one on the same broken url: another network trip at best,
    // and at worst a clip that comes back to life under the image — with the chip unable to take the
    // focus back to mute it.
    @Test
    fun aFailedClipIsNotRebuiltWhenTheSurfaceComesBack() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, "https://x/f.jpg", loop = true, muted = false)
        deliverSurface(view)
        playerShadow!!.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO)

        texture(view).surfaceTextureListener!!.onSurfaceTextureDestroyed(SurfaceTexture(1))
        deliverSurface(view)

        assertEquals("a broken url must not be retried behind the fallback image", 1, playersCreated)
    }

    // Resume revives only what the pause stopped (iOS resumes only if it was playing): a one-shot
    // clip that already finished must not start over just because the user came back.
    @Test
    fun aFinishedClipDoesNotRestartWhenTheHostComesBack() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = false, muted = true)
        deliverSurface(view)
        ShadowLooper.idleMainLooper(DURATION_MS + 100L, TimeUnit.MILLISECONDS)
        assertEquals(ShadowMediaPlayer.State.PLAYBACK_COMPLETED, playerShadow!!.state)

        view.pauseForHost()
        view.resumeForHost()

        assertEquals(ShadowMediaPlayer.State.PLAYBACK_COMPLETED, playerShadow!!.state)
    }

    // A one-shot clip that played through makes no more sound and no more motion, and this template
    // has no auto-dismiss: without the completion listener the user's music stays ducked under
    // silence and the screen refuses to sleep for as long as the in-app is up.
    @Test
    fun aFinishedClipHandsTheUsersAudioBackAndLetsTheScreenSleep() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = false, muted = false)
        deliverSurface(view)
        assertNull("nothing abandoned while it plays", shadowOf(audioManager).lastAbandonedAudioFocusListener)

        ShadowLooper.idleMainLooper(DURATION_MS + 100L, TimeUnit.MILLISECONDS)

        assertEquals(ShadowMediaPlayer.State.PLAYBACK_COMPLETED, playerShadow!!.state)
        assertNotNull(shadowOf(audioManager).lastAbandonedAudioFocusListener)
        assertFalse("a still last frame must not keep the device awake", view.keepScreenOn)
    }

    // The chip outlives the clip the same way it outlives a failure or a teardown: the last frame
    // stays on screen with the mute icon on top of it. Unmuting a video that already ended must not
    // buy the user's music back a second time.
    @Test
    fun unmutingAFinishedClipDoesNotDuckTheUsersMusicAgain() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = false, muted = true)
        deliverSurface(view)
        ShadowLooper.idleMainLooper(DURATION_MS + 100L, TimeUnit.MILLISECONDS)

        view.isMuted = false

        assertNull("nothing is going to play; the music must keep its volume", shadowOf(audioManager).lastAudioFocusRequest)
    }

    // A TextureView stretches whatever it is given onto its own bounds, so "fit inside, keep the
    // ratio" only exists as this matrix — without it a square or portrait clip comes out smeared
    // across a landscape screen. Same result as iOS's videoGravity = .resizeAspect.
    @Test
    fun theFrameIsFittedInsteadOfStretched() {
        val view = InAppVideoPlayerView(activity)
        view.configure(VIDEO_URL, null, null, loop = true, muted = true)
        deliverSurface(view)
        layOut(view, 1000, 500)

        deliverVideoSize(view, 400, 400)

        val transform = Matrix()
        texture(view).getTransform(transform)
        val values = FloatArray(9)
        transform.getValues(values)
        assertEquals("a square clip stays square: 500x500 in a 1000x500 view", 0.5f, values[Matrix.MSCALE_X], 0.001f)
        assertEquals(1f, values[Matrix.MSCALE_Y], 0.001f)
        assertEquals("centred, with the pillarbox split evenly", 250f, values[Matrix.MTRANS_X], 0.001f)
        assertEquals(0f, values[Matrix.MTRANS_Y], 0.001f)
    }
}
