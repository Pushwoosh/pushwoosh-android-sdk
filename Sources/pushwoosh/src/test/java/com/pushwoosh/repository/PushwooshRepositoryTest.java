/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.math.BigDecimal;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshRepositoryTest {

    private PushwooshRepository pushwooshRepository;

    private RequestManager requestManager;
    private SendTagsProcessor sendTagsProcessor;
    private RegistrationPrefs registrationPrefs;
    private NotificationPrefs notificationPrefs;

    private PreferenceJsonObjectValue tagsPref;
    private PreferenceStringValue lastNotificationHashPref;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        requestManager = Mockito.mock(RequestManager.class);
        sendTagsProcessor = Mockito.mock(SendTagsProcessor.class);

        registrationPrefs = Mockito.mock(RegistrationPrefs.class);
        when(registrationPrefs.communicationEnable()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.removeAllDeviceData()).thenReturn(mock(PreferenceBooleanValue.class));
        when(registrationPrefs.advertisingId()).thenReturn(mock(PreferenceStringValue.class));
        when(registrationPrefs.getTrackingUrl()).thenReturn("https://tracking.example/");

        notificationPrefs = Mockito.mock(NotificationPrefs.class);

        tagsPref = Mockito.mock(PreferenceJsonObjectValue.class);
        when(notificationPrefs.tags()).thenReturn(tagsPref);

        lastNotificationHashPref = mock(PreferenceStringValue.class);
        when(notificationPrefs.lastNotificationHash()).thenReturn(lastNotificationHashPref);

        pushwooshRepository =
                new PushwooshRepository(requestManager, sendTagsProcessor, registrationPrefs, notificationPrefs);
    }

    @Test
    public void testRemoveTag() throws Exception {
        pushwooshRepository.removeAllDeviceData();
        verify(notificationPrefs.tags()).set(null);
        verify(registrationPrefs.advertisingId()).set("");
        verify(registrationPrefs.removeAllDeviceData()).set(true);
    }

    // ---------- sendTags ----------

    @Test
    public void sendTags_mergesIntoPrefsAndForwardsToProcessor() {
        TagsBundle tags = new TagsBundle.Builder().putString("Language", "en").build();
        JSONObject expectedJson = tags.toJson();
        @SuppressWarnings("unchecked")
        Callback<Void, PushwooshException> listener = mock(Callback.class);

        pushwooshRepository.sendTags(tags, listener);

        ArgumentCaptor<JSONObject> mergeCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(tagsPref).merge(mergeCaptor.capture());
        assertEquals(expectedJson.toString(), mergeCaptor.getValue().toString());

        ArgumentCaptor<JSONObject> processorCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(sendTagsProcessor).sendTags(processorCaptor.capture(), eq(listener));
        assertEquals(expectedJson.toString(), processorCaptor.getValue().toString());
    }

    @Test
    public void sendTags_continuesToProcessorEvenWhenMergeThrows() {
        TagsBundle tags = new TagsBundle.Builder().putString("k", "v").build();
        @SuppressWarnings("unchecked")
        Callback<Void, PushwooshException> listener = mock(Callback.class);
        doThrow(new RuntimeException("merge boom")).when(tagsPref).merge(any(JSONObject.class));

        pushwooshRepository.sendTags(tags, listener);

        verify(sendTagsProcessor).sendTags(any(JSONObject.class), eq(listener));
    }

    // ---------- sendAdvertisingId ----------

    @Test
    @SuppressWarnings("unchecked")
    public void sendAdvertisingId_sendsRequestToTrackingUrl() {
        Callback<Void, NetworkException> callback = mock(Callback.class);

        pushwooshRepository.sendAdvertisingId("ad-id-1", callback);

        verify(requestManager)
                .sendRequest(any(SetAdvertisingIdRequest.class), eq("https://tracking.example/"), eq(callback));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sendAdvertisingId_withNullRequestManager_invokesCallbackWithNetworkException() {
        PushwooshRepository repoWithoutManager =
                new PushwooshRepository(null, sendTagsProcessor, registrationPrefs, notificationPrefs);
        Callback<Void, NetworkException> callback = mock(Callback.class);

        repoWithoutManager.sendAdvertisingId("ad-id-1", callback);

        ArgumentCaptor<Result<Void, NetworkException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Void, NetworkException> result = resultCaptor.getValue();
        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        verify(requestManager, never()).sendRequest(any(SetAdvertisingIdRequest.class), any(), any());
    }

    // ---------- sendEmailTags ----------

    @Test
    @SuppressWarnings("unchecked")
    public void sendEmailTags_forwardsSuccessResultToListener() {
        TagsBundle tags = new TagsBundle.Builder().putString("k", "v").build();
        Callback<Void, PushwooshException> listener = mock(Callback.class);

        pushwooshRepository.sendEmailTags(tags, "user@example.com", listener);

        ArgumentCaptor<Callback<Void, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(SetEmailTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromData(null));

        ArgumentCaptor<Result<Void, PushwooshException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(listener).process(outerCaptor.capture());
        assertTrue(outerCaptor.getValue().isSuccess());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sendEmailTags_forwardsFailureExceptionToListener() {
        TagsBundle tags = new TagsBundle.Builder().putString("k", "v").build();
        Callback<Void, PushwooshException> listener = mock(Callback.class);

        pushwooshRepository.sendEmailTags(tags, "user@example.com", listener);

        ArgumentCaptor<Callback<Void, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(SetEmailTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromException(new NetworkException("boom")));

        ArgumentCaptor<Result<Void, PushwooshException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(listener).process(outerCaptor.capture());
        Result<Void, PushwooshException> forwarded = outerCaptor.getValue();
        assertFalse(forwarded.isSuccess());
        assertTrue(forwarded.getException() instanceof NetworkException);
        assertEquals("boom", forwarded.getException().getMessage());
    }

    // ---------- getTags ----------

    @Test
    @SuppressWarnings("unchecked")
    public void getTags_returnsServerDataAndCachesIt() throws Exception {
        Callback<TagsBundle, GetTagsException> callback = mock(Callback.class);
        TagsBundle serverTags =
                new TagsBundle.Builder().putString("Language", "en").build();

        pushwooshRepository.getTags(callback);

        ArgumentCaptor<Callback<TagsBundle, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(GetTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromData(serverTags));

        ArgumentCaptor<JSONObject> setCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(tagsPref).set(setCaptor.capture());
        assertEquals(serverTags.toJson().toString(), setCaptor.getValue().toString());

        ArgumentCaptor<Result<TagsBundle, GetTagsException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(outerCaptor.capture());
        assertTrue(outerCaptor.getValue().isSuccess());
        assertEquals("en", outerCaptor.getValue().getData().getString("Language"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTags_returnsEmptyBundle_whenServerDataIsNullButSuccess() {
        Callback<TagsBundle, GetTagsException> callback = mock(Callback.class);

        pushwooshRepository.getTags(callback);

        ArgumentCaptor<Callback<TagsBundle, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(GetTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromData(null));

        ArgumentCaptor<Result<TagsBundle, GetTagsException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(outerCaptor.capture());
        Result<TagsBundle, GetTagsException> result = outerCaptor.getValue();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().toJson().length());

        ArgumentCaptor<JSONObject> cacheCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(tagsPref).set(cacheCaptor.capture());
        assertEquals(0, cacheCaptor.getValue().length());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTags_fallsBackToCachedTags_whenServerFails() throws Exception {
        Callback<TagsBundle, GetTagsException> callback = mock(Callback.class);
        JSONObject cached = new JSONObject().put("k", "v");
        when(tagsPref.get()).thenReturn(cached);

        pushwooshRepository.getTags(callback);

        ArgumentCaptor<Callback<TagsBundle, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(GetTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromException(new NetworkException("x")));

        ArgumentCaptor<Result<TagsBundle, GetTagsException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(outerCaptor.capture());
        Result<TagsBundle, GetTagsException> result = outerCaptor.getValue();
        assertTrue(result.isSuccess());
        assertEquals("v", result.getData().getString("k"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTags_returnsGetTagsException_whenServerFailedAndNoCache() {
        Callback<TagsBundle, GetTagsException> callback = mock(Callback.class);
        when(tagsPref.get()).thenReturn(null);

        pushwooshRepository.getTags(callback);

        ArgumentCaptor<Callback<TagsBundle, NetworkException>> innerCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(requestManager).sendRequest(any(GetTagsRequest.class), innerCaptor.capture());
        innerCaptor.getValue().process(Result.fromException(new NetworkException("boom")));

        ArgumentCaptor<Result<TagsBundle, GetTagsException>> outerCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(outerCaptor.capture());
        Result<TagsBundle, GetTagsException> result = outerCaptor.getValue();
        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof GetTagsException);
        assertTrue(result.getException().getMessage().contains("boom"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTags_withNullRequestManager_invokesCallbackWithGetTagsException() {
        PushwooshRepository repoWithoutManager =
                new PushwooshRepository(null, sendTagsProcessor, registrationPrefs, notificationPrefs);
        Callback<TagsBundle, GetTagsException> callback = mock(Callback.class);

        repoWithoutManager.getTags(callback);

        ArgumentCaptor<Result<TagsBundle, GetTagsException>> captor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(captor.capture());
        Result<TagsBundle, GetTagsException> result = captor.getValue();
        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof GetTagsException);
        verify(requestManager, never()).sendRequest(any(GetTagsRequest.class), any());
    }

    // ---------- sendPushOpenedSync ----------

    @Test
    public void sendPushOpenedSync_storesHashOnSuccess() {
        when(lastNotificationHashPref.get()).thenReturn(null);
        when(requestManager.sendRequestSync(any(PushStatRequest.class))).thenReturn(Result.fromData(null));

        Result<Void, NetworkException> result = pushwooshRepository.sendPushOpenedSync("hash-1", "meta");

        assertTrue(result.isSuccess());
        verify(lastNotificationHashPref).set("hash-1");
    }

    @Test
    public void sendPushOpenedSync_skipsNetwork_whenHashAlreadySent() {
        when(lastNotificationHashPref.get()).thenReturn("hash-1");

        Result<Void, NetworkException> result = pushwooshRepository.sendPushOpenedSync("hash-1", "meta");

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verify(requestManager, never()).sendRequestSync(any(PushStatRequest.class));
        verify(lastNotificationHashPref, never()).set("hash-1");
    }

    @Test
    public void sendPushOpenedSync_withNullRequestManager_returnsNetworkException() {
        PushwooshRepository repoWithoutManager =
                new PushwooshRepository(null, sendTagsProcessor, registrationPrefs, notificationPrefs);
        when(lastNotificationHashPref.get()).thenReturn(null);

        Result<Void, NetworkException> result = repoWithoutManager.sendPushOpenedSync("hash-1", "meta");

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("Request manager is null", result.getException().getMessage());
    }

    @Test
    public void sendPushOpenedSync_wrapsThrownRuntimeException_intoNetworkExceptionResult() {
        when(lastNotificationHashPref.get()).thenReturn(null);
        when(requestManager.sendRequestSync(any(PushStatRequest.class))).thenThrow(new RuntimeException("io fail"));

        Result<Void, NetworkException> result = pushwooshRepository.sendPushOpenedSync("hash-1", "meta");

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("io fail", result.getException().getMessage());
        verify(lastNotificationHashPref, never()).set("hash-1");
    }

    @Test
    public void sendPushOpenedSync_doesNotStoreHash_whenSendFailed() {
        when(lastNotificationHashPref.get()).thenReturn(null);
        when(requestManager.sendRequestSync(any(PushStatRequest.class)))
                .thenReturn(Result.fromException(new NetworkException("x")));

        Result<Void, NetworkException> result = pushwooshRepository.sendPushOpenedSync("hash-1", "meta");

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("x", result.getException().getMessage());
        verify(lastNotificationHashPref, never()).set(any());
    }

    // ---------- sendPushDeliveredSync ----------

    @Test
    public void sendPushDeliveredSync_withNullRequestManager_returnsNetworkException() {
        PushwooshRepository repoWithoutManager =
                new PushwooshRepository(null, sendTagsProcessor, registrationPrefs, notificationPrefs);

        Result<Void, NetworkException> result = repoWithoutManager.sendPushDeliveredSync("hash-1", "meta");

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("Request manager is null", result.getException().getMessage());
    }

    @Test
    public void sendPushDeliveredSync_wrapsThrownException() {
        when(requestManager.sendRequestSync(any(MessageDeliveredRequest.class)))
                .thenThrow(new RuntimeException("oops"));

        Result<Void, NetworkException> result = pushwooshRepository.sendPushDeliveredSync("hash-1", "meta");

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("oops", result.getException().getMessage());
    }

    // ---------- sendInappPurchase ----------

    @Test
    @SuppressWarnings("unchecked")
    public void sendInappPurchase_postsEventWithFormattedAttributes() {
        InAppRepository inAppRepository = mock(InAppRepository.class);
        try (MockedStatic<InAppModule> mocked = mockStatic(InAppModule.class)) {
            mocked.when(InAppModule::getInAppRepository).thenReturn(inAppRepository);

            pushwooshRepository.sendInappPurchase("SKU1", new BigDecimal("9.99"), "USD", new Date(0));

            ArgumentCaptor<TagsBundle> tagsCaptor = ArgumentCaptor.forClass(TagsBundle.class);
            verify(inAppRepository).postEvent(eq("PW_InAppPurchase"), tagsCaptor.capture(), any());
            TagsBundle attributes = tagsCaptor.getValue();
            assertEquals("SKU1", attributes.getString("productIdentifier"));
            assertEquals("9.99", attributes.getString("amount"));
            assertEquals("USD", attributes.getString("currency"));
            assertEquals(1, attributes.getInt("quantity", 0));
            assertEquals("success", attributes.getString("status"));
            assertNotNull(attributes.getString("transactionDate"));
        }
    }

    // ---------- prefetchTags ----------

    @Test
    public void prefetchTags_cachesNonEmptyServerTags() {
        TagsBundle bundle = new TagsBundle.Builder().putString("Language", "en").build();
        when(requestManager.sendRequestSync(any(GetTagsRequest.class))).thenReturn(Result.fromData(bundle));

        pushwooshRepository.prefetchTags();

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(tagsPref).set(captor.capture());
        assertEquals(bundle.toJson().toString(), captor.getValue().toString());
    }

    @Test
    public void prefetchTags_doesNotCacheEmptyTags() {
        TagsBundle emptyBundle = new TagsBundle.Builder().build();
        when(requestManager.sendRequestSync(any(GetTagsRequest.class))).thenReturn(Result.fromData(emptyBundle));

        pushwooshRepository.prefetchTags();

        verify(tagsPref, never()).set(any(JSONObject.class));
    }

    @Test
    public void prefetchTags_doesNothingOnFailure() {
        when(requestManager.sendRequestSync(any(GetTagsRequest.class)))
                .thenReturn(Result.fromException(new NetworkException("x")));

        pushwooshRepository.prefetchTags();

        verify(tagsPref, never()).set(any(JSONObject.class));
    }
}
