package com.pushwoosh.calls.listener

import android.os.Bundle
import com.pushwoosh.calls.PushwooshVoIPMessage

/**
 * Callback interface for handling VoIP call lifecycle events.
 *
 * Implement this interface to integrate your VoIP calling solution (WebRTC, Twilio, Agora, etc.)
 * with Pushwoosh VoIP push notifications. The SDK invokes these callbacks when users interact
 * with incoming calls or when system events occur.
 *
 * **Registration:**
 *
 * Register your implementation in `AndroidManifest.xml`:
 * ```xml
 * <application>
 *     <meta-data
 *         android:name="com.pushwoosh.CALL_EVENT_LISTENER"
 *         android:value="com.yourapp.YourCallEventListener" />
 * </application>
 * ```
 *
 * **Typical Implementation:**
 * ```kotlin
 * class YourCallEventListener : CallEventListener {
 *     override fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int) {
 *         // User answered - start your VoIP connection and navigate to call screen
 *     }
 *
 *     override fun onReject(voIPMessage: PushwooshVoIPMessage) {
 *         // User rejected - notify your backend if needed
 *     }
 *
 *     override fun onDisconnect(voIPMessage: PushwooshVoIPMessage) {
 *         // Call ended - cleanup resources and close call UI
 *     }
 *
 *     // Implement other methods as needed for your use case
 * }
 * ```
 *
 * **Note:** All callbacks run on the main thread. The default implementation logs events only.
 *
 * @see PushwooshVoIPMessage
 * @see PushwooshCallEventListener
 */
interface CallEventListener {
    /**
     * Called when the user answers an incoming call.
     *
     * Implement this to start your VoIP connection and navigate to your call UI.
     *
     * @param voIPMessage call details (caller name, video flag, call ID, etc.)
     * @param videoState Android Telecom video state
     */
    fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int)

    /**
     * Called when the user rejects an incoming call.
     *
     * Use this to notify your backend or perform cleanup as needed.
     *
     * @param voIPMessage call details
     */
    fun onReject(voIPMessage: PushwooshVoIPMessage)

    /**
     * Called when the call is disconnected (by user or remote party).
     *
     * Implement this to cleanup resources, close VoIP connections, and update UI.
     *
     * @param voIPMessage call details
     */
    fun onDisconnect(voIPMessage: PushwooshVoIPMessage)

    /**
     * Called when Android Telecom creates an incoming connection.
     *
     * Triggered before the call UI is shown. Use for pre-initialization if needed.
     *
     * @param payload raw push notification data
     */
    fun onCreateIncomingConnection(payload: Bundle?)

    /**
     * Called when the call is added to Android InCallService.
     *
     * Use for logging or state tracking if needed.
     *
     * @param voIPMessage call details
     */
    fun onCallAdded(voIPMessage: PushwooshVoIPMessage)

    /**
     * Called when the call is removed from Android InCallService.
     *
     * Use for final cleanup or state updates if needed.
     *
     * @param voIPMessage call details
     */
    fun onCallRemoved(voIPMessage: PushwooshVoIPMessage)

    /**
     * Called when the remote party cancels the call before it's answered.
     *
     * Triggered by a push notification with `cancelCall=true`. Use this to show
     * a missed call notification or update your call history.
     *
     * @param voIPMessage call details including callId
     */
    fun onCallCancelled(voIPMessage: PushwooshVoIPMessage)

    /**
     * Called when call cancellation fails.
     *
     * Triggered when the system cannot cancel the call (e.g., already answered).
     * Use for error logging if needed.
     *
     * @param callId the call ID that failed to cancel
     * @param reason why cancellation failed
     */
    fun onCallCancellationFailed(callId: String?, reason: String)
}