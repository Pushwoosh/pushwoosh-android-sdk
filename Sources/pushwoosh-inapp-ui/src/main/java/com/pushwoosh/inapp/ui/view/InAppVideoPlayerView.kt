package com.pushwoosh.inapp.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.internal.utils.PWLog
import kotlin.math.min

private const val TAG = "InAppVideoPlayerView"

/**
 * The media engine behind the `video` template: poster until the first frame, looping playback,
 * an image fallback when the video will not play, and a strict teardown. Mirrors iOS's
 * `PWInAppVideoPlayerView` split — the template above knows nothing about [MediaPlayer], this view
 * knows nothing about buttons or scrims.
 *
 * The platform [MediaPlayer] is deliberate: `androidx.media3` would add three transitive artifacts
 * to every integrator (and to the hand-written POM) to buy adaptive HLS and DASH, which a promo
 * clip does not use. MP4/H.264 is CDD-guaranteed on every device; HLS is best-effort, and the
 * contract's `fallback` already covers a clip the device cannot play.
 *
 * [TextureView] rather than `VideoView`/`SurfaceView`: the template fades in inside a translucent
 * Activity, and a SurfaceView punches a hole through the window and cannot animate alpha.
 */
internal class InAppVideoPlayerView(context: Context) : FrameLayout(context) {

    /** The video cannot play and there is no fallback image — the host must dismiss the in-app. */
    var onFailed: (() -> Unit)? = null

    /** [isMuted] changed (a tap, or audio focus lost to someone else) — the host redraws its icon. */
    var onMuteStateChanged: (() -> Unit)? = null

    private val texture = TextureView(context)
    private val poster = ImageView(context)
    private val spinner = ProgressBar(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        // We asked for TRANSIENT_MAY_DUCK, so a loss means someone needs the channel more than a
        // promo clip does. Going silent (instead of ignoring it) keeps the chip honest too.
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            isMuted = true
        }
    }
    private var hasAudioFocus = false

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoUrl: String? = null
    private var fallbackUrl: String? = null
    private var loops = false
    private var released = false
    private var playbackFailed = false
    private var hostPaused = false
    private var wasPlayingBeforeHostPause = false
    private var videoWidth = 0
    private var videoHeight = 0
    private val transform = Matrix()

    /**
     * The real player state, not a flag beside it (iOS reads `player.isMuted` for the same reason).
     * Unmuting takes the audio focus only when there is sound to duck for right now ([isSounding]),
     * because the chip outlives the engine in three different ways — the fallback image after a
     * failure, the exit animation after [teardown], and the last frame of a clip that played
     * through — and a focus taken in any of those windows would duck the user's music under silence
     * with nobody left to hand it back. A clip that is still preparing needs no focus either: the
     * one taken here would sit on the user's music for the whole network round trip, while
     * [startPlayback] takes its own the moment sound actually begins.
     */
    var isMuted: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            player?.let { applyVolume(it) }
            if (value) abandonAudioFocus() else if (isSounding()) requestAudioFocus()
            onMuteStateChanged?.invoke()
        }

    /** No playback is coming: torn down, or failed with no way back. A player that has not been
     *  built yet is *not* dead — the surface simply has not arrived. */
    private val isDead: Boolean
        get() = released || playbackFailed

    init {
        setBackgroundColor(Color.BLACK)
        // A clip longer than the screen timeout generates no input, so without this a full-screen
        // video dims and locks the device mid-playback. MediaPlayer's own
        // setScreenOnWhilePlaying only covers a SurfaceHolder, which a TextureView does not have.
        keepScreenOn = true
        addView(texture, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        poster.scaleType = ImageView.ScaleType.FIT_CENTER
        poster.visibility = View.GONE
        addView(poster, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        spinner.isIndeterminate = true
        spinner.indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
        spinner.visibility = View.GONE
        addView(spinner, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                attachSurface(Surface(st))
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) = applyAspectFit()

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                detachSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    /**
     * The surface comes and goes with the host window, not with this view: backgrounding the app
     * destroys it and returning builds a new one. So arrival either starts the first playback or
     * re-binds the player that survived the trip — it must never be treated as "first time", and
     * departure must never [teardown] (a one-way latch there would bring the user back to a black
     * screen with the text still on it, and would silently kill [pauseForHost]/[resumeForHost]).
     */
    private fun attachSurface(newSurface: Surface) {
        surface = newSurface
        val mp = player
        if (mp == null) {
            startPlaybackIfPossible()
            return
        }
        try {
            mp.setSurface(newSurface)
        } catch (e: IllegalStateException) {
            PWLog.warn(TAG, "video: surface re-attach refused ($e)")
        }
    }

    private fun detachSurface() {
        player?.let {
            try {
                it.setSurface(null)
            } catch (e: IllegalStateException) {
                PWLog.warn(TAG, "video: surface detach refused ($e)")
            }
        }
        surface?.release()
        surface = null
    }

    /**
     * Arms the engine. Playback itself waits for the surface, which cannot exist before the view is
     * attached — so this never fails synchronously, and [onFailed] can never fire while the template
     * is still inside its own constructor with no listener wired up yet.
     */
    fun configure(videoUrl: String, posterUrl: String?, fallbackImageUrl: String?, loop: Boolean, muted: Boolean) {
        this.videoUrl = videoUrl
        this.fallbackUrl = fallbackImageUrl
        this.loops = loop
        InAppImageLoader.load(posterUrl, poster)
        spinner.visibility = View.VISIBLE
        isMuted = muted
        startPlaybackIfPossible()
    }

    /** The surface arrives more than once (see [attachSurface]), so this is also what keeps a clip
     *  that already failed from being built again on the same broken url every time the host window
     *  comes back — behind the fallback image, where the chip can no longer take the focus to mute
     *  it. */
    private fun startPlaybackIfPossible() {
        if (isDead || player != null) return
        val url = videoUrl ?: return
        val target = surface ?: return

        val mp = MediaPlayer()
        try {
            mp.setSurface(target)
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            mp.isLooping = loops
            applyVolume(mp)
            mp.setOnPreparedListener { prepared -> onPrepared(prepared) }
            mp.setOnErrorListener { _, what, extra -> onPlayerError(what, extra) }
            mp.setOnInfoListener { _, what, _ -> onPlayerInfo(what) }
            mp.setOnCompletionListener { onPlaybackCompleted() }
            mp.setOnVideoSizeChangedListener { _, width, height -> onVideoSize(width, height) }
            mp.setDataSource(url)
            mp.prepareAsync()
        } catch (e: Exception) {
            // setDataSource/prepareAsync throw on a malformed url, an unreachable host or a
            // container the device cannot open — the same user-visible outcome as an async onError,
            // so it funnels into the same branch. A throwing player is unusable and must not be left
            // holding the surface.
            mp.release()
            PWLog.warn(TAG, "video: cannot open '$url' ($e)")
            handlePlaybackFailure()
            return
        }
        player = mp
    }

    private fun onPrepared(mp: MediaPlayer) {
        if (released) return
        spinner.visibility = View.GONE
        if (hostPaused) {
            // Preparing takes seconds over the network, so the app can reach the background first.
            // Starting here would play a looping clip out loud over whatever the user switched to,
            // and [pauseForHost] has already come and gone — the resume takes it from here.
            wasPlayingBeforeHostPause = true
            return
        }
        startPlayback(mp)
    }

    /**
     * The poster comes off only when frames are actually about to arrive; hiding it any earlier
     * (on `prepared` alone) leaves the user staring at black while playback waits for the host.
     *
     * Every sound this template makes starts here — the first prepare and the return from the
     * background alike — so this is where the focus is taken. Asking for it at any of the places a
     * play might be *coming* from is what leaves the gaps: a clip that finishes preparing while the
     * app is in the background is started by [onPrepared], which [resumeForHost] never sees.
     */
    private fun startPlayback(mp: MediaPlayer) {
        poster.visibility = View.GONE
        if (!isMuted) requestAudioFocus()
        try {
            mp.start()
        } catch (e: IllegalStateException) {
            PWLog.warn(TAG, "video: start refused ($e)")
            handlePlaybackFailure()
        }
    }

    /** Returns `true` = handled; otherwise MediaPlayer follows the error with an OnCompletion. */
    private fun onPlayerError(what: Int, extra: Int): Boolean {
        if (released) return true
        PWLog.warn(TAG, "video: playback error what=$what extra=$extra")
        handlePlaybackFailure()
        return true
    }

    private fun onPlayerInfo(what: Int): Boolean {
        if (released) return false
        // The Android counterpart of iOS's FailedToPlayToEndTime → spinner (PWInAppVideoPlayerView.swift:162).
        when (what) {
            MediaPlayer.MEDIA_INFO_BUFFERING_START -> spinner.visibility = View.VISIBLE
            MediaPlayer.MEDIA_INFO_BUFFERING_END -> spinner.visibility = View.GONE
        }
        return false
    }

    /**
     * A `loop:false` clip that reached its end holds its last frame and never speaks or moves
     * again, and this template has no auto-dismiss — so without this the user's music would stay
     * ducked under silence and the screen would refuse to sleep until the in-app is closed by hand.
     * The player itself stays alive and bound to the surface: releasing it here would drop that
     * frame to black. A looping clip never gets this callback. A later tap on the chip cannot buy
     * the focus back either — [isSounding] asks the player, and a finished one is not playing.
     */
    private fun onPlaybackCompleted() {
        if (released) return
        abandonAudioFocus()
        keepScreenOn = false
    }

    /** `internal` for the same reason as [handlePlaybackFailure]: `ShadowMediaPlayer` can invoke the
     *  prepared/error/info/completion listeners but has no video-size one, and reaching this through
     *  the framework's private listener field would tie the test to an AOSP internal. */
    internal fun onVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        applyAspectFit()
    }

    /**
     * The single failure funnel: a synchronous open failure and an async
     * [MediaPlayer.OnErrorListener] land here alike. `internal` rather than private so its two
     * outcomes can be asserted without standing a player up; the paths that lead here are covered
     * separately, by tests that hand the [TextureView] a surface themselves.
     */
    internal fun handlePlaybackFailure() {
        playbackFailed = true
        spinner.visibility = View.GONE
        // Nothing is going to make sound or move from here on: stop ducking the user's music, let
        // the screen sleep again, and hand the decoder and the surface back. A MediaPlayer in Error
        // is as unusable as one that threw on open, and that branch already releases it.
        abandonAudioFocus()
        keepScreenOn = false
        releasePlayer()
        val fallback = fallbackUrl
        if (fallback.isNullOrEmpty()) {
            onFailed?.invoke()
            return
        }
        poster.scaleType = ImageView.ScaleType.CENTER_CROP
        InAppImageLoader.load(fallback, poster)
    }

    /**
     * A [TextureView] stretches the frame onto its own bounds, so "fit inside, keep the ratio" has
     * to be spelled out as a matrix — without it a portrait clip on a landscape screen comes out
     * smeared. Same result as iOS's `videoGravity = .resizeAspect`.
     */
    private fun applyAspectFit() {
        val viewWidth = texture.width
        val viewHeight = texture.height
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
        val scale = min(viewWidth.toFloat() / videoWidth, viewHeight.toFloat() / videoHeight)
        val drawnWidth = videoWidth * scale
        val drawnHeight = videoHeight * scale
        transform.setScale(drawnWidth / viewWidth, drawnHeight / viewHeight)
        transform.postTranslate((viewWidth - drawnWidth) / 2f, (viewHeight - drawnHeight) / 2f)
        texture.setTransform(transform)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) applyAspectFit()
    }

    /**
     * The host Activity is going away for now: remember whether we were playing, then stop. The
     * flag is raised before anything else, because a clip still preparing has no player to pause
     * and would otherwise start itself the moment it becomes ready — in the background, out loud.
     */
    fun pauseForHost() {
        hostPaused = true
        // Silent from here on, so the user's music gets its volume back for the whole time we sit
        // in the background; the resume below takes the focus again if we are still unmuted.
        abandonAudioFocus()
        val mp = player ?: return
        wasPlayingBeforeHostPause = try {
            mp.isPlaying
        } catch (e: IllegalStateException) {
            false
        }
        if (!wasPlayingBeforeHostPause) return
        try {
            mp.pause()
        } catch (e: IllegalStateException) {
            PWLog.warn(TAG, "video: pause refused ($e)")
        }
    }

    /** Resumes only what [pauseForHost] actually stopped — a paused-by-buffering clip stays paused.
     *  The focus comes back with the sound, inside [startPlayback]. */
    fun resumeForHost() {
        hostPaused = false
        if (!wasPlayingBeforeHostPause) return
        wasPlayingBeforeHostPause = false
        player?.let { startPlayback(it) }
    }

    /**
     * Idempotent, and reached from two places that overlap on every dismissal: the template's exit
     * animation (so the sound dies with the tap, not 220ms later) and [onDetachedFromWindow]. Losing
     * the window surface is deliberately *not* one of them — see [attachSurface]. [released] also
     * muzzles callbacks that were already in flight.
     */
    fun teardown() {
        if (released) return
        released = true
        abandonAudioFocus()
        keepScreenOn = false
        releasePlayer()
        surface?.release()
        surface = null
    }

    private fun releasePlayer() {
        val mp = player ?: return
        player = null
        mp.setOnPreparedListener(null)
        mp.setOnErrorListener(null)
        mp.setOnInfoListener(null)
        mp.setOnCompletionListener(null)
        mp.setOnVideoSizeChangedListener(null)
        mp.release()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        teardown()
    }

    /** Sound is coming out right now — asked of the player instead of tracked beside it, so no
     *  state can drift away from it (preparing, paused for the host, finished, released). */
    private fun isSounding(): Boolean = try {
        player?.isPlaying == true
    } catch (e: IllegalStateException) {
        false
    }

    private fun applyVolume(mp: MediaPlayer) {
        val volume = if (isMuted) 0f else 1f
        try {
            mp.setVolume(volume, volume)
        } catch (e: IllegalStateException) {
            PWLog.warn(TAG, "video: volume refused ($e)")
        }
    }

    /** The deprecated overload on purpose: `AudioFocusRequest` is API 26+ and would buy a version
     *  split for the identical result on a minSdk 23 module. */
    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val manager = audioManager ?: return
        val granted = manager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
        hasAudioFocus = granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        hasAudioFocus = false
        audioManager?.abandonAudioFocus(focusListener)
    }
}
