package com.pushwoosh.calls.listener

import android.telecom.CallAudioState

/**
 * Optional callback interface for the audio state of the active VoIP call.
 *
 * Implement it in the same class as your [CallEventListener] (the one registered with the
 * `com.pushwoosh.CALL_EVENT_LISTENER` meta-data key) to learn the actual microphone mute state and
 * audio route reported by Android Telecom, for example after
 * [com.pushwoosh.calls.PushwooshCallControl.mute] or
 * [com.pushwoosh.calls.PushwooshCallControl.speakerOn].
 *
 * ```kotlin
 * class YourCallEventListener : CallEventListener, CallAudioStateListener {
 *     // CallEventListener methods ...
 *
 *     override fun onMuteChanged(muted: Boolean) {
 *         // Update the mute button of your call screen
 *     }
 *
 *     override fun onAudioRouteChanged(route: Int) {
 *         val speakerOn = route == CallAudioState.ROUTE_SPEAKER
 *         // Update the speaker button of your call screen
 *     }
 * }
 * ```
 *
 * **Note:** All callbacks run on the main thread.
 *
 * @see CallEventListener
 * @see com.pushwoosh.calls.PushwooshCallControl
 */
interface CallAudioStateListener {
    /**
     * Called when the microphone mute state of the call changes.
     *
     * @param muted `true` if the microphone is muted
     */
    fun onMuteChanged(muted: Boolean)

    /**
     * Called when the audio route of the call changes.
     *
     * @param route one of [CallAudioState.ROUTE_EARPIECE], [CallAudioState.ROUTE_SPEAKER],
     * [CallAudioState.ROUTE_WIRED_HEADSET] or [CallAudioState.ROUTE_BLUETOOTH]
     */
    fun onAudioRouteChanged(route: Int)
}
