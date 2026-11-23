package com.pushwoosh.inline.internal.net

import android.os.Bundle
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.internal.network.ConnectionException
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.network.NetworkModule
import com.pushwoosh.internal.network.PushRequest
import com.pushwoosh.internal.network.RequestBlockedException
import com.pushwoosh.internal.network.RequestManager
import com.pushwoosh.internal.preference.PreferenceStringValue
import com.pushwoosh.repository.RegistrationPrefs
import com.pushwoosh.repository.RepositoryModule
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineWireTest {
    @After
    fun tearDown() {
        NetworkModule.setRequestManager(null)
    }

    // Verifies that getInlineBanners is flat, asks both formats with nativeVersion 1 and goes under api/v2/device-api.
    @Test
    fun testBannersRequestBody() {
        val request = GetInlineBannersRequest(listOf("a", "b"), null)

        assertFalse(request.shouldWrapRequest())
        assertEquals("api/v2/device-api/", request.apiPath)
        val body = request.paramsOver(JSONObject())
        assertEquals(3, body.length())
        assertEquals("""["a","b"]""", body.getJSONArray("slots").toString())
        assertEquals("""["native","html"]""", body.getJSONArray("formats").toString())
        assertEquals(1, body.getInt("nativeVersion"))
    }

    // Verifies that an override URL disables the api path so the request goes to the override root as is.
    @Test
    fun testOverrideUrlDisablesApiPath() {
        assertNull(GetInlineBannersRequest(listOf("a"), "http://10.0.2.2:9595/").apiPath)
        assertNull(TrackInlineEventRequest(JSONObject(), apiPathMode = false).apiPath)
        assertEquals("api/v2/device-api/", TrackInlineEventRequest(JSONObject(), apiPathMode = true).apiPath)
    }

    // Verifies that a track event keeps its own userId/application over the base fields.
    @Test
    fun testTrackEventFieldsOverrideBaseFields() {
        val event = JSONObject().put("userId", "previous").put("application", "OLDAPP").put("type", "show")
        val request = TrackInlineEventRequest(event, apiPathMode = true)
        val base = JSONObject().put("userId", "current").put("application", "NEWAPP").put("hwid", "h")

        request.paramsOver(base)

        assertEquals("previous", base.getString("userId"))
        assertEquals("OLDAPP", base.getString("application"))
        assertEquals("show", base.getString("type"))
        assertEquals("h", base.getString("hwid"))
    }

    // Verifies that a queued event resolves its host by the application it was recorded for, not the current one.
    @Test
    fun testTrackEventHostFollowsItsOwnApplication() {
        Mockito.mockStatic(RepositoryModule::class.java).use { repo ->
            val prefs = Mockito.mock(RegistrationPrefs::class.java)
            val appCode = Mockito.mock(PreferenceStringValue::class.java)
            Mockito.`when`(appCode.get()).thenReturn("NEWAPP")
            Mockito.`when`(prefs.applicationId()).thenReturn(appCode)
            repo.`when`<RegistrationPrefs>(RepositoryModule::getRegistrationPreferences).thenReturn(prefs)

            assertEquals("OLDAPP", TrackInlineEventRequest(JSONObject().put("application", "OLDAPP"), apiPathMode = true).applicationCode)
            assertEquals("NEWAPP", TrackInlineEventRequest(JSONObject().put("type", "show"), apiPathMode = true).applicationCode)
        }
    }

    // Verifies that the banners request remembers the user and app code it serialized.
    @Test
    fun testBannersRequestCapturesSerializedOwner() {
        val request = GetInlineBannersRequest(listOf("a"), null)

        request.paramsOver(JSONObject().put("userId", "u1").put("application", "APP-1"))

        assertEquals("u1", request.sentUserId)
        assertEquals("APP-1", request.sentAppCode)
    }

    // Verifies that the manifest override is trimmed, gets a trailing slash, and blank means absent.
    @Test
    fun testInlineUrlFromManifest() {
        fun bundle(value: String?) = Bundle().apply { if (value != null) putString("com.pushwoosh.inline_url", value) }

        assertEquals("http://10.0.2.2:9595/", InlineEndpoint.overrideUrl(bundle(" http://10.0.2.2:9595 ")))
        assertEquals("http://h/p/", InlineEndpoint.overrideUrl(bundle("http://h/p/")))
        assertNull(InlineEndpoint.overrideUrl(bundle("  ")))
        assertNull(InlineEndpoint.overrideUrl(bundle(null)))
        assertNull(InlineEndpoint.overrideUrl(null))
    }

    // Verifies that a blocked request maps to code 2 and a connection failure to code 0.
    @Test
    fun testFailureMapping() {
        assertEquals(
            PushwooshInlineException.ENDPOINT_UNAVAILABLE,
            mapFailure(RequestBlockedException("blocked")).code,
        )
        assertEquals(PushwooshInlineException.OTHER, mapFailure(ConnectionException("offline", 0, 0)).code)
    }

    // Verifies that an answer without a slots object is code 4.
    @Test
    fun testMissingSlotsIsInvalidResponse() {
        NetworkModule.setRequestManager(AnsweringRequestManager(JSONObject("{}")))
        var result: InlineFetchResult? = null

        CoreInlineTransport { null }.fetch(listOf("a")) { result = it }

        val failure = result as InlineFetchResult.Failure
        assertEquals(PushwooshInlineException.INVALID_RESPONSE, failure.error.code)
    }

    // Verifies that a blocked send surfaces as code 2 and a present slots object as success.
    @Test
    fun testTransportMapsBlockedAndSuccess() {
        NetworkModule.setRequestManager(FailingRequestManager(RequestBlockedException("proxy")))
        var result: InlineFetchResult? = null
        CoreInlineTransport { null }.fetch(listOf("a")) { result = it }
        assertEquals(
            PushwooshInlineException.ENDPOINT_UNAVAILABLE,
            (result as InlineFetchResult.Failure).error.code,
        )

        NetworkModule.setRequestManager(AnsweringRequestManager(JSONObject("""{"slots":{"a":null}}""")))
        CoreInlineTransport { null }.fetch(listOf("a")) { result = it }
        assertTrue((result as InlineFetchResult.Success).slots.has("a"))
    }

    private open class StubRequestManager : RequestManager {
        override fun <Response> sendRequest(request: PushRequest<Response>) = Unit

        override fun <Response> sendRequest(
            request: PushRequest<Response>,
            callback: Callback<Response, NetworkException>,
        ) = Unit

        override fun <Response> sendRequest(
            request: PushRequest<Response>,
            baseUrl: String?,
            callback: Callback<Response, NetworkException>,
        ) = Unit

        override fun <Response> sendRequestSync(request: PushRequest<Response>): Result<Response, NetworkException> =
            error("unused")

        override fun updateBaseUrl(baseUrl: String?) = false

        override fun setReverseProxyUrl(url: String?, headers: MutableMap<String, String>?) = false
    }

    private class AnsweringRequestManager(private val answer: JSONObject) : StubRequestManager() {
        override fun <Response> sendRequest(
            request: PushRequest<Response>,
            baseUrl: String?,
            callback: Callback<Response, NetworkException>,
        ) = callback.process(Result.fromData(request.parseResponse(answer)))
    }

    private class FailingRequestManager(private val error: NetworkException) : StubRequestManager() {
        override fun <Response> sendRequest(
            request: PushRequest<Response>,
            baseUrl: String?,
            callback: Callback<Response, NetworkException>,
        ) = callback.process(Result.fromException(error))
    }
}
