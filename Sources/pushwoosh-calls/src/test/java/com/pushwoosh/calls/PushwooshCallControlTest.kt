package com.pushwoosh.calls

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.telecom.CallAudioState
import android.telecom.Connection
import com.pushwoosh.calls.listener.CallAudioStateListener
import com.pushwoosh.calls.listener.CallEventListener
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.test.ShadowRouteRecordingConnection
import com.pushwoosh.exception.PushwooshException
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.internal.platform.AndroidPlatformModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowRouteRecordingConnection::class])
class PushwooshCallControlTest {

    private lateinit var audioManager: AudioManager

    private class RecordingCallback : Callback<Void, PushwooshException> {
        val results = mutableListOf<Result<Void, PushwooshException>>()
        var callbackLooper: Looper? = null

        override fun process(result: Result<Void, PushwooshException>) {
            results += result
            callbackLooper = Looper.myLooper()
        }

        fun single(): Result<Void, PushwooshException> = results.single()
    }

    private open class SilentCallEventListener : CallEventListener {
        override fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int) {}
        override fun onReject(voIPMessage: PushwooshVoIPMessage) {}
        override fun onDisconnect(voIPMessage: PushwooshVoIPMessage) {}
        override fun onCreateIncomingConnection(payload: Bundle?) {}
        override fun onCallAdded(voIPMessage: PushwooshVoIPMessage) {}
        override fun onCallRemoved(voIPMessage: PushwooshVoIPMessage) {}
        override fun onCallCancelled(voIPMessage: PushwooshVoIPMessage) {}
        override fun onCallCancellationFailed(callId: String?, reason: String) {}
    }

    private class AudioStateRecordingListener : SilentCallEventListener(), CallAudioStateListener {
        val events = mutableListOf<String>()

        override fun onMuteChanged(muted: Boolean) {
            events += "muted=$muted"
        }

        override fun onAudioRouteChanged(route: Int) {
            events += "route=${CallAudioState.audioRouteToString(route)}"
        }
    }

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        AndroidPlatformModule.init(app, true)
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @After
    fun tearDown() {
        PushwooshConnectionService.setActiveConnection(null)
    }

    private fun activeCall(listener: CallEventListener = SilentCallEventListener()): PushwooshConnection {
        val connection = PushwooshConnection(listener)
        connection.setActive()
        PushwooshConnectionService.setActiveConnection(connection)
        return connection
    }

    private fun telecomReports(connection: Connection, muted: Boolean, route: Int, supportedRoutes: Int) {
        ReflectionHelpers.callInstanceMethod<Any>(
            connection,
            "setCallAudioState",
            ClassParameter.from(CallAudioState::class.java, CallAudioState(muted, route, supportedRoutes))
        )
    }

    private fun requestedRoutes(connection: Connection): List<Int> =
        Shadow.extract<ShadowRouteRecordingConnection>(connection).requestedRoutes

    private fun runOnMain(command: (Callback<Void, PushwooshException>) -> Unit): RecordingCallback {
        val callback = RecordingCallback()
        command(callback)
        shadowOf(Looper.getMainLooper()).idle()
        return callback
    }

    // Verifies that mute turns off the microphone (not the speaker) of an answered call and reports success
    @Test
    fun testMuteMutesMicrophoneWhenCallIsActive() {
        activeCall()
        audioManager.isMicrophoneMute = false

        val callback = runOnMain { PushwooshCallControl.mute(it) }

        assertTrue(callback.single().isSuccess)
        assertTrue(audioManager.isMicrophoneMute)
    }

    // Verifies that unmute turns the microphone of an answered call back on and reports success
    @Test
    fun testUnmuteUnmutesMicrophoneWhenCallIsActive() {
        activeCall()
        audioManager.isMicrophoneMute = true

        val callback = runOnMain { PushwooshCallControl.unmute(it) }

        assertTrue(callback.single().isSuccess)
        assertFalse(audioManager.isMicrophoneMute)
    }

    // Verifies that speakerOn asks Telecom for the speaker route of the call and reports success
    @Test
    fun testSpeakerOnRequestsSpeakerRouteWhenCallIsActive() {
        val connection = activeCall()
        telecomReports(connection, false, CallAudioState.ROUTE_EARPIECE, EARPIECE_AND_SPEAKER)

        val callback = runOnMain { PushwooshCallControl.speakerOn(it) }

        assertTrue(callback.single().isSuccess)
        assertEquals(listOf(CallAudioState.ROUTE_SPEAKER), requestedRoutes(connection))
    }

    // Verifies that speakerOff returns the call to the route it had before speakerOn (Bluetooth here, not earpiece)
    @Test
    fun testSpeakerOffRestoresRouteUsedBeforeSpeakerOn() {
        val connection = activeCall()
        val supported = EARPIECE_AND_SPEAKER or CallAudioState.ROUTE_BLUETOOTH
        telecomReports(connection, false, CallAudioState.ROUTE_BLUETOOTH, supported)
        runOnMain { PushwooshCallControl.speakerOn(it) }
        telecomReports(connection, false, CallAudioState.ROUTE_SPEAKER, supported)

        val callback = runOnMain { PushwooshCallControl.speakerOff(it) }

        assertTrue(callback.single().isSuccess)
        assertEquals(CallAudioState.ROUTE_BLUETOOTH, requestedRoutes(connection).last())
    }

    // Verifies that speakerOff without a prior speakerOn keeps a call that is already off the speaker on its route
    @Test
    fun testSpeakerOffKeepsCurrentRouteWhenSpeakerIsNotOn() {
        val connection = activeCall()
        telecomReports(
            connection, false, CallAudioState.ROUTE_BLUETOOTH, EARPIECE_AND_SPEAKER or CallAudioState.ROUTE_BLUETOOTH
        )

        runOnMain { PushwooshCallControl.speakerOff(it) }

        assertEquals(listOf(CallAudioState.ROUTE_BLUETOOTH), requestedRoutes(connection))
    }

    // Verifies that speakerOff on a call that started on the speaker picks an available route (wired headset when there is no earpiece)
    @Test
    fun testSpeakerOffPicksAvailableRouteWhenCallStartedOnSpeaker() {
        val connection = activeCall()
        val supported =
            CallAudioState.ROUTE_SPEAKER or CallAudioState.ROUTE_WIRED_HEADSET or CallAudioState.ROUTE_BLUETOOTH
        telecomReports(connection, false, CallAudioState.ROUTE_SPEAKER, supported)

        runOnMain { PushwooshCallControl.speakerOff(it) }

        assertEquals(listOf(CallAudioState.ROUTE_WIRED_HEADSET), requestedRoutes(connection))
    }

    // Verifies that all four commands report an error and change nothing when there is no call
    @Test
    fun testCommandsFailWhenThereIsNoCall() {
        audioManager.isMicrophoneMute = false

        assertAllCommandsFail()
        assertFalse(audioManager.isMicrophoneMute)
    }

    // Verifies that all four commands report an error and change nothing while the call is still ringing
    @Test
    fun testCommandsFailWhenCallIsRinging() {
        val connection = PushwooshConnection(SilentCallEventListener())
        connection.setRinging()
        PushwooshConnectionService.setActiveConnection(connection)
        audioManager.isMicrophoneMute = false

        assertAllCommandsFail()
        assertFalse(audioManager.isMicrophoneMute)
        assertTrue(requestedRoutes(connection).isEmpty())
    }

    // Verifies that below Android 8.0 (no self-managed calls, no Connection.setAudioRoute) the commands report an error
    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun testCommandsFailBelowAndroidO() {
        activeCall()
        audioManager.isMicrophoneMute = false

        assertAllCommandsFail()
        assertFalse(audioManager.isMicrophoneMute)
    }

    // Verifies that a command issued from a background thread reports its result on the main thread, not synchronously
    @Test
    fun testCallbackRunsOnMainThread() {
        activeCall()
        val callback = RecordingCallback()

        val caller = Thread { PushwooshCallControl.mute(callback) }
        caller.start()
        caller.join()
        assertTrue(callback.results.isEmpty())

        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.single().isSuccess)
        assertSame(Looper.getMainLooper(), callback.callbackLooper)
    }

    // Verifies that an exception thrown by the app's callback does not escape to the main looper and break later commands
    @Test
    fun testThrowingCallbackDoesNotEscapeToMainLooper() {
        activeCall()
        PushwooshCallControl.mute { throw IllegalStateException("call screen is gone") }
        PushwooshCallControl.speakerOn { throw IllegalStateException("call screen is gone") }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(runOnMain { PushwooshCallControl.unmute(it) }.single().isSuccess)
    }

    // Verifies that a CallAudioStateListener receives the mute state and route reported by Telecom, once per change
    @Test
    fun testAudioStateListenerReceivesChangesReportedByTelecom() {
        val listener = AudioStateRecordingListener()
        val connection = activeCall(listener)

        telecomReports(connection, false, CallAudioState.ROUTE_EARPIECE, EARPIECE_AND_SPEAKER)
        telecomReports(connection, true, CallAudioState.ROUTE_EARPIECE, EARPIECE_AND_SPEAKER)
        telecomReports(connection, true, CallAudioState.ROUTE_SPEAKER, EARPIECE_AND_SPEAKER)
        telecomReports(connection, true, CallAudioState.ROUTE_SPEAKER, EARPIECE_AND_SPEAKER)

        assertEquals(
            listOf("muted=false", "route=EARPIECE", "muted=true", "route=SPEAKER"),
            listener.events
        )
    }

    private fun assertAllCommandsFail() {
        val commands: List<(Callback<Void, PushwooshException>) -> Unit> = listOf(
            { PushwooshCallControl.mute(it) },
            { PushwooshCallControl.unmute(it) },
            { PushwooshCallControl.speakerOn(it) },
            { PushwooshCallControl.speakerOff(it) }
        )
        for (command in commands) {
            val result = runOnMain(command).single()
            assertFalse(result.isSuccess)
        }
    }

    private companion object {
        const val EARPIECE_AND_SPEAKER = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
    }
}
