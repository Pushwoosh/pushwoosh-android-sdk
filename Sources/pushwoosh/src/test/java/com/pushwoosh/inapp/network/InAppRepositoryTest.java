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

package com.pushwoosh.inapp.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RichMediaActionException;
import com.pushwoosh.exception.SetEmailException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.downloader.DownloadResult;
import com.pushwoosh.inapp.network.downloader.InAppDownloader;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by aevstefeev on 07/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class InAppRepositoryTest {
    public static final String TEST_EXCEPTION_STRING = "test_exception";
    public static final NetworkException EXCEPTION = new NetworkException(TEST_EXCEPTION_STRING);
    public static final String TEST_EXCEPTION = "TEST_EXCEPTION";
    public static final String RICH_MEDIA =
            "{\"url\":\"https:\\/\\/richmedia-01.pushwoosh.com\\/9\\/F\\/9F5CD-8579F.zip\",\"code\":\"9AFBB-234CC\",\"layout\":\"topbanner\",\"updated\":1524913801,\"closeButtonType\":0,\"hash\":\"2b690544a8d9da7cd7f7340b40251ea5\",\"required\":true,\"priority\":0, \"ts\":0, \"businessCase\":\"\",\"gdpr\":\"Delete\"}";
    private InAppRepository inAppRepository;

    private RequestManager requestManagerMock;
    private InAppStorage inAppStorageMock;
    private InAppFolderProvider inAppFolderProviderMock;
    private ResourceMapper resourceMapperMock;
    private InAppDownloader inAppDownloaderMock;
    private InAppDeployedChecker inAppDeployedCheckerMock;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        requestManagerMock = mock(RequestManager.class);
        inAppStorageMock = mock(InAppStorage.class);
        inAppFolderProviderMock = mock(InAppFolderProvider.class);
        resourceMapperMock = mock(ResourceMapper.class);
        inAppDownloaderMock = mock(InAppDownloader.class);
        inAppDeployedCheckerMock = mock(InAppDeployedChecker.class);

        inAppRepository = new InAppRepository(
                requestManagerMock,
                inAppStorageMock,
                inAppDownloaderMock,
                resourceMapperMock,
                inAppFolderProviderMock,
                platformTestManager.getRegistrationPrefs());

        WhiteboxHelper.setInternalState(inAppRepository, "inAppDeployedChecker", inAppDeployedCheckerMock);
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
    }

    @Test
    public void loadInApps() throws Exception {
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource("1", true));
        resourceList.add(new Resource("2", true));
        resourceList.add(new Resource("3", true));

        List<String> codeList = new ArrayList<>();
        codeList.add("1");
        codeList.add("2");
        codeList.add("3");

        Result<Object, NetworkException> getInAppsResult = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(getInAppsResult);
        when(inAppStorageMock.saveOrUpdateResources(resourceList)).thenReturn(codeList);
        when(inAppDownloaderMock.downloadAndDeploy(resourceList)).thenReturn(DownloadResult.success(resourceList));

        Result<Void, NetworkException> result = inAppRepository.loadInApps();

        Assert.assertNull(result.getData());
        verify(inAppDownloaderMock, Mockito.times(3)).removeResourceFiles(Mockito.anyString());
        verify(inAppStorageMock).saveOrUpdateResources(resourceList);
        verify(inAppDownloaderMock).downloadAndDeploy(resourceList);
        verify(requestManagerMock).sendRequestSync(any());
    }

    @Test
    public void postEvent() throws Exception {
        Handler inlineMain = mock(Handler.class);
        when(inlineMain.post(any())).thenAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run(); // run immediately on the test thread
            return true;
        });

        List<Resource> resources = new ArrayList<>();
        resources.add(new Resource("test_code", true));
        when(requestManagerMock.sendRequestSync(any())).thenReturn(Result.from(resources, null));

        when(inAppDownloaderMock.downloadAndDeploy(any())).thenReturn(DownloadResult.success(resources));

        ExecutorService directIo = InAppExecutorServiceHelper.createExecutorService();

        WhiteboxHelper.setInternalState(inAppRepository, "main", inlineMain);
        WhiteboxHelper.setInternalState(inAppRepository, "io", directIo);

        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        inAppRepository.postEvent("test_event", Tags.intTag("intTag", 5), callback);

        JSONObject response = new JSONObject();
        response.put("code", "test_code");
        response.put("required", "true");
        Result<PostEventResponse, NetworkException> result = Result.fromData(new PostEventResponse(response));
        emulatePostEventToNetwork(result);

        ArgumentCaptor<Result<Resource, PostEventException>> resultArgumentCaptor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultArgumentCaptor.capture());
        Assert.assertEquals(1, resultArgumentCaptor.getAllValues().size());
        Result<Resource, PostEventException> value = resultArgumentCaptor.getValue();
        Resource data = value.getData();
        Assert.assertEquals("test_code", data.getCode());
        Assert.assertEquals(true, data.isRequired());
    }

    private void emulatePostEventToNetwork(Result<PostEventResponse, NetworkException> result) throws JSONException {
        ArgumentCaptor<Callback<PostEventResponse, NetworkException>> callbackNetworkArgumentCaptor =
                ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<PostEventRequest> reauestCaptor = ArgumentCaptor.forClass(PostEventRequest.class);
        verify(requestManagerMock).sendRequest(reauestCaptor.capture(), callbackNetworkArgumentCaptor.capture());

        JSONObject jsonRequest = new JSONObject();
        reauestCaptor.getValue().buildParams(jsonRequest);
        Assert.assertEquals("test_event", jsonRequest.getString("event"));
        Assert.assertEquals("{\"intTag\":5}", jsonRequest.getString("attributes"));

        callbackNetworkArgumentCaptor.getValue().process(result);
    }

    @Test
    public void postEventErrorServer() throws Exception {
        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        inAppRepository.postEvent("test_event", Tags.intTag("intTag", 5), callback);

        Result<PostEventResponse, NetworkException> result = Result.fromException(EXCEPTION);
        emulatePostEventToNetwork(result);

        ArgumentCaptor<Result<Resource, PostEventException>> resultArgumentCaptor =
                ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultArgumentCaptor.capture());
        Assert.assertEquals(1, resultArgumentCaptor.getAllValues().size());
        Result<Resource, PostEventException> value = resultArgumentCaptor.getValue();
        Assert.assertEquals(TEST_EXCEPTION_STRING, value.getException().getMessage());
    }

    @Test
    public void mergeUserIdServerError() throws Exception {
        Callback<Void, MergeUserException> callback = CallbackWrapper.spy();
        inAppRepository.mergeUserId("1", "2", true, callback);

        ArgumentCaptor<Callback<Void, NetworkException>> callbackNetworkArgumentCaptor =
                ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<MergeUserRequest> mergeUserRequestArgumentCaptor =
                ArgumentCaptor.forClass(MergeUserRequest.class);
        verify(requestManagerMock)
                .sendRequest(mergeUserRequestArgumentCaptor.capture(), callbackNetworkArgumentCaptor.capture());
        callbackNetworkArgumentCaptor.getValue().process(Result.fromException(new NetworkException(TEST_EXCEPTION)));

        List<MergeUserRequest> mergeUserRequestList = mergeUserRequestArgumentCaptor.getAllValues();
        Assert.assertEquals(1, mergeUserRequestList.size());
        JSONObject jsonObject = new JSONObject();
        MergeUserRequest mergeUserRequest = mergeUserRequestList.get(0);
        mergeUserRequest.buildParams(jsonObject);
        Assert.assertEquals("1", jsonObject.getString("oldUserId"));
        Assert.assertEquals("2", jsonObject.getString("newUserId"));
        Assert.assertEquals("true", jsonObject.getString("merge"));
        Assert.assertEquals("mergeUser", mergeUserRequest.getMethod());

        ArgumentCaptor<Result<Void, MergeUserException>> resultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultArgumentCaptor.capture());

        List<Result<Void, MergeUserException>> resultList = resultArgumentCaptor.getAllValues();
        Assert.assertEquals(1, resultList.size());
        Result<Void, MergeUserException> mergeUserExceptionResult = resultList.get(0);
        Assert.assertFalse(mergeUserExceptionResult.isSuccess());
        Assert.assertEquals(
                TEST_EXCEPTION, mergeUserExceptionResult.getException().getMessage());
    }

    @Test
    public void prefetchRichMediaAllReadyDeploy() throws Exception {
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(true);
        Result<Resource, ResourceParseException> result = inAppRepository.prefetchRichMedia(RICH_MEDIA);
        Assert.assertNull(result.getException());
    }

    @Test
    public void prefetchRichMediaFailDeploy() throws Exception {
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(false);
        DownloadResult downloadResult = DownloadResult.empty();
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(downloadResult);
        Result<Resource, ResourceParseException> result = inAppRepository.prefetchRichMedia(RICH_MEDIA);

        Assert.assertEquals(
                "Can't download or update richMedia: r-9F5CD-8579F",
                result.getException().getMessage());
    }

    @Test
    public void mapToHtmlData() throws Exception {
        Resource resource = new Resource("1", true);
        DownloadResult downloadResult = DownloadResult.success(Collections.singletonList(resource));

        when(inAppStorageMock.getResource("1")).thenReturn(resource);
        when(inAppDownloaderMock.downloadAndDeploy(Mockito.anyList())).thenReturn(downloadResult);

        HtmlData htmlData = new HtmlData("1", "url", "html");
        when(resourceMapperMock.map(resource)).thenReturn(htmlData);

        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(resource);
        Result<Object, NetworkException> getInAppsResult = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(getInAppsResult);

        inAppRepository.loadInApps();
        Result<HtmlData, ResourceParseException> result = inAppRepository.mapToHtmlData(resource);
        Assert.assertEquals(htmlData, result.getData());
        Assert.assertNull(result.getException());
    }

    // Verifies that loadInApps sets inAppLoaded flag and skips downloads when server returns empty list.
    @Test
    public void loadInApps_inAppListEmpty_setsInAppLoadedFlagAndReturnsNullData() {
        Result<Object, NetworkException> emptyResult = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyResult);

        Result<Void, NetworkException> result = inAppRepository.loadInApps();

        Assert.assertNull(result.getData());
        Assert.assertNull(result.getException());
        verify(inAppDownloaderMock, never()).downloadAndDeploy(anyList());
        verify(inAppDownloaderMock, never()).removeResourceFiles(Mockito.anyString());
        AtomicBoolean inAppLoaded = (AtomicBoolean) WhiteboxHelper.getInternalState(inAppRepository, "inAppLoaded");
        Assert.assertTrue(inAppLoaded.get());
    }

    // Verifies that loadInApps still sets inAppLoaded flag in finally block when storage throws.
    @Test
    public void loadInApps_storageThrows_stillSetsInAppLoadedFlag() {
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource("1", true));
        Result<Object, NetworkException> result = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(result);
        when(inAppStorageMock.saveOrUpdateResources(anyList())).thenThrow(new RuntimeException("boom"));

        Assert.assertThrows(RuntimeException.class, () -> inAppRepository.loadInApps());

        AtomicBoolean inAppLoaded = (AtomicBoolean) WhiteboxHelper.getInternalState(inAppRepository, "inAppLoaded");
        Assert.assertTrue(inAppLoaded.get());
    }

    // Verifies that setUserId fetches RequestManager from NetworkModule fallback when local one is null.
    @Test
    public void setUserId_requestManagerNull_fetchesFromNetworkModule() {
        WhiteboxHelper.setInternalState(inAppRepository, "requestManager", null);
        RequestManager fallbackManager = mock(RequestManager.class);

        try (MockedStatic<NetworkModule> networkModule = mockStatic(NetworkModule.class)) {
            networkModule.when(NetworkModule::getRequestManager).thenReturn(fallbackManager);

            inAppRepository.setUserId("user42", null);

            verify(fallbackManager).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
            verify(requestManagerMock, never()).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
        }
    }

    // Verifies that setUserId no-ops when both local RequestManager and NetworkModule fallback are null.
    @Test
    public void setUserId_requestManagerNullAndNetworkModuleReturnsNull_noOp() {
        WhiteboxHelper.setInternalState(inAppRepository, "requestManager", null);
        Callback<Boolean, SetUserIdException> callback = CallbackWrapper.spy();

        try (MockedStatic<NetworkModule> networkModule = mockStatic(NetworkModule.class)) {
            networkModule.when(NetworkModule::getRequestManager).thenReturn(null);

            inAppRepository.setUserId("user42", callback);

            verify(requestManagerMock, never()).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
            verify(callback, never()).process(any());
        }
    }

    // Verifies that loadInApps skips downloads when all resources are already deployed.
    @Test
    public void loadInApps_allResourcesAlreadyDeployed_skipsDownload() {
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource("1", true));
        resourceList.add(new Resource("2", true));
        Result<Object, NetworkException> getInAppsResult = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(getInAppsResult);
        when(inAppStorageMock.saveOrUpdateResources(anyList())).thenReturn(Collections.emptyList());
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(true);

        inAppRepository.loadInApps();

        verify(inAppDownloaderMock, never()).downloadAndDeploy(anyList());
        verify(inAppDownloaderMock, never()).removeResourceFiles(Mockito.anyString());
    }

    // Verifies that setUserId with callback delivers success Result when server responds with success.
    @Test
    public void setUserIdWithCallback_serverSuccess_callbackReceivesTrue() {
        Callback<Boolean, SetUserIdException> callback = CallbackWrapper.spy();
        inAppRepository.setUserId("user42", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> cb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), cb.capture());
        cb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserIdException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserIdException> value = resultCaptor.getValue();
        Assert.assertTrue(value.isSuccess());
        Assert.assertEquals(Boolean.TRUE, value.getData());
    }

    // Verifies that setUserId with callback delivers SetUserIdException carrying server message on failure.
    @Test
    public void setUserIdWithCallback_serverFailure_callbackReceivesSetUserIdException() {
        Callback<Boolean, SetUserIdException> callback = CallbackWrapper.spy();
        inAppRepository.setUserId("user42", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> cb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), cb.capture());
        cb.getValue().process(Result.fromException(new NetworkException("boom")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserIdException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserIdException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof SetUserIdException);
        Assert.assertEquals("boom", value.getException().getMessage());
    }

    // Verifies that setUserId callback receives default error message when server failure carries no message.
    @Test
    public void setUserIdWithCallback_serverFailureWithoutMessage_callbackReceivesDefaultErrorMessage() {
        Callback<Boolean, SetUserIdException> callback = CallbackWrapper.spy();
        inAppRepository.setUserId("user42", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> cb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), cb.capture());
        cb.getValue().process(Result.fromException(new NetworkException("")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserIdException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserIdException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertNotNull(value.getException().getMessage());
        Assert.assertTrue(value.getException().getMessage().contains("/registerUser"));
    }

    // Verifies that setUser logs warning and skips request when userId is empty.
    @Test
    public void setUser_emptyUserId_logsWarningAndDoesNotSendRequest() {
        Callback<Boolean, SetUserException> callback = CallbackWrapper.spy();

        inAppRepository.setUser("", Collections.singletonList("a@x.com"), callback);

        verify(requestManagerMock, never()).sendRequest(any(), any(Callback.class));
        verify(callback, never()).process(any());
    }

    // Verifies that setUser invokes callback with success after RegisterUser + RegisterEmail + RegisterEmailUser all
    // succeed.
    @Test
    public void setUser_userIdSuccessAndAllEmailsSuccess_callbackReceivesTrue() {
        Callback<Boolean, SetUserException> callback = CallbackWrapper.spy();
        inAppRepository.setUser("u", Collections.singletonList("a@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), registerUserCb.capture());
        registerUserCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        registerEmailCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailUserRequest.class), registerEmailUserCb.capture());
        registerEmailUserCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserException> value = resultCaptor.getValue();
        Assert.assertTrue(value.isSuccess());
        Assert.assertEquals(Boolean.TRUE, value.getData());
    }

    // Verifies that setUser stops chain and yields SetUserException when RegisterUser fails.
    @Test
    public void setUser_userIdFails_callbackReceivesSetUserException() {
        Callback<Boolean, SetUserException> callback = CallbackWrapper.spy();
        inAppRepository.setUser("u", Collections.singletonList("a@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), registerUserCb.capture());
        registerUserCb.getValue().process(Result.fromException(new NetworkException("user-err")));

        verify(requestManagerMock, never()).sendRequest(any(RegisterEmailRequest.class), any(Callback.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof SetUserException);
        Assert.assertEquals("user-err", value.getException().getMessage());
    }

    // Verifies that setUser yields SetUserException with default registerEmail message when email step fails.
    @Test
    public void setUser_userIdSuccessButEmailFails_callbackReceivesSetUserException() {
        Callback<Boolean, SetUserException> callback = CallbackWrapper.spy();
        inAppRepository.setUser("u", Collections.singletonList("a@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterUserRequest.class), registerUserCb.capture());
        registerUserCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        registerEmailCb.getValue().process(Result.fromException(new NetworkException("")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetUserException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof SetUserException);
        Assert.assertTrue(value.getException().getMessage().contains("/registerEmail"));
    }

    // Verifies that setEmail(List) skips request when list is empty.
    @Test
    public void setEmailList_empty_logsWarningAndDoesNotSendRequest() {
        Callback<Boolean, SetEmailException> callback = CallbackWrapper.spy();

        inAppRepository.setEmail(Collections.<String>emptyList(), callback);

        verify(requestManagerMock, never()).sendRequest(any(), any(Callback.class));
        verify(callback, never()).process(any());
    }

    // Verifies that setEmail(List) yields success once both RegisterEmail and RegisterEmailUser succeed for a single
    // email.
    // Verifies that setEmail(List) invokes success callback exactly once when counter equals list size.
    @Test
    public void setEmailList_twoEmailsBothSucceed_callbackInvokedOnceWhenCounterReachesListSize() {
        Callback<Boolean, SetEmailException> callback = CallbackWrapper.spy();
        inAppRepository.setEmail(Arrays.asList("a@x.com", "b@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock, Mockito.times(2))
                .sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        for (Object cb : registerEmailCb.getAllValues()) {
            ((Callback) cb).process(Result.fromData(null));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock, Mockito.times(2))
                .sendRequest(any(RegisterEmailUserRequest.class), registerEmailUserCb.capture());
        for (Object cb : registerEmailUserCb.getAllValues()) {
            ((Callback) cb).process(Result.fromData(null));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetEmailException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback, Mockito.times(1)).process(resultCaptor.capture());
        Assert.assertTrue(resultCaptor.getValue().isSuccess());
    }

    // Verifies that setEmail(List) emits SetEmailException for the failed email and skips final success callback.
    @Test
    public void setEmailList_oneEmailFails_callbackReceivesSetEmailExceptionForFailedEmailOnly() {
        Callback<Boolean, SetEmailException> callback = CallbackWrapper.spy();
        inAppRepository.setEmail(Arrays.asList("a@x.com", "b@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock, Mockito.times(2))
                .sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());

        // first email succeeds (RegisterEmail + RegisterEmailUser)
        ((Callback) registerEmailCb.getAllValues().get(0)).process(Result.fromData(null));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> emailUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailUserRequest.class), emailUserCb.capture());
        emailUserCb.getValue().process(Result.fromData(null));

        // second email fails on RegisterEmail
        ((Callback) registerEmailCb.getAllValues().get(1))
                .process(Result.fromException(new NetworkException("net-err")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetEmailException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        List<Result<Boolean, SetEmailException>> values = resultCaptor.getAllValues();
        Assert.assertEquals(1, values.size());
        Result<Boolean, SetEmailException> fail = values.get(0);
        Assert.assertFalse(fail.isSuccess());
        Assert.assertTrue(fail.getException() instanceof SetEmailException);
        Assert.assertTrue(fail.getException().getMessage().contains("net-err"));
    }

    // Verifies that setEmail(String) yields success after both RegisterEmail and RegisterEmailUser succeed.
    @Test
    public void setEmailSingle_registerSuccessAndRegisterEmailUserSuccess_callbackReceivesTrue() {
        Callback<Boolean, PushwooshException> callback = CallbackWrapper.spy();
        inAppRepository.setEmail("a@x.com", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        registerEmailCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailUserRequest.class), registerEmailUserCb.capture());
        registerEmailUserCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, PushwooshException> value = resultCaptor.getValue();
        Assert.assertTrue(value.isSuccess());
        Assert.assertEquals(Boolean.TRUE, value.getData());
    }

    // Verifies that setEmail(String) yields PushwooshException when RegisterEmail step fails.
    @Test
    public void setEmailSingle_registerEmailFails_callbackReceivesPushwooshException() {
        Callback<Boolean, PushwooshException> callback = CallbackWrapper.spy();
        inAppRepository.setEmail("a@x.com", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        registerEmailCb.getValue().process(Result.fromException(new NetworkException("re-err")));

        verify(requestManagerMock, never()).sendRequest(any(RegisterEmailUserRequest.class), any(Callback.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, PushwooshException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof PushwooshException);
    }

    // Verifies that setEmail(String) yields PushwooshException when RegisterEmailUser step fails.
    @Test
    public void setEmailSingle_registerEmailUserFails_callbackReceivesPushwooshException() {
        Callback<Boolean, PushwooshException> callback = CallbackWrapper.spy();
        inAppRepository.setEmail("a@x.com", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailRequest.class), registerEmailCb.capture());
        registerEmailCb.getValue().process(Result.fromData(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Callback> registerEmailUserCb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RegisterEmailUserRequest.class), registerEmailUserCb.capture());
        registerEmailUserCb.getValue().process(Result.fromException(new NetworkException("ru-err")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, PushwooshException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof PushwooshException);
    }

    // Verifies that richMediaAction delivers success Result when server responds with success.
    @Test
    public void richMediaAction_serverSuccess_callbackReceivesData() {
        Callback<Void, RichMediaActionException> callback = CallbackWrapper.spy();
        inAppRepository.richMediaAction("rich", "inapp", "hash", "{}", 1, callback);

        ArgumentCaptor<Callback<Void, NetworkException>> cb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RichMediaActionRequest.class), cb.capture());
        cb.getValue().process(Result.fromData(null));

        ArgumentCaptor<Result<Void, RichMediaActionException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Assert.assertTrue(resultCaptor.getValue().isSuccess());
    }

    // Verifies that richMediaAction maps NetworkException to RichMediaActionException carrying same message.
    @Test
    public void richMediaAction_serverFailure_callbackReceivesRichMediaActionException() {
        Callback<Void, RichMediaActionException> callback = CallbackWrapper.spy();
        inAppRepository.richMediaAction("rich", "inapp", "hash", "{}", 1, callback);

        ArgumentCaptor<Callback<Void, NetworkException>> cb = ArgumentCaptor.forClass(Callback.class);
        verify(requestManagerMock).sendRequest(any(RichMediaActionRequest.class), cb.capture());
        cb.getValue().process(Result.fromException(new NetworkException("ra-err")));

        ArgumentCaptor<Result<Void, RichMediaActionException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Void, RichMediaActionException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof RichMediaActionException);
        Assert.assertEquals("ra-err", value.getException().getMessage());
    }

    // Verifies that richMediaAction emits RichMediaActionException when both RequestManager and NetworkModule are null.
    @Test
    public void richMediaAction_requestManagerNull_callbackReceivesRichMediaActionException() {
        WhiteboxHelper.setInternalState(inAppRepository, "requestManager", null);
        Callback<Void, RichMediaActionException> callback = CallbackWrapper.spy();

        try (MockedStatic<NetworkModule> networkModule = mockStatic(NetworkModule.class)) {
            networkModule.when(NetworkModule::getRequestManager).thenReturn(null);

            inAppRepository.richMediaAction("rich", "inapp", "hash", "{}", 1, callback);

            ArgumentCaptor<Result<Void, RichMediaActionException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(resultCaptor.capture());
            Result<Void, RichMediaActionException> value = resultCaptor.getValue();
            Assert.assertFalse(value.isSuccess());
            Assert.assertTrue(value.getException() instanceof RichMediaActionException);
            Assert.assertEquals("Request Manager is null", value.getException().getMessage());
        }
    }

    // Verifies that postEvent emits PostEventException when RequestManager is unavailable.
    @Test
    public void postEvent_requestManagerNull_callbackReceivesPostEventException() {
        WhiteboxHelper.setInternalState(inAppRepository, "requestManager", null);
        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        try (MockedStatic<NetworkModule> networkModule = mockStatic(NetworkModule.class)) {
            networkModule.when(NetworkModule::getRequestManager).thenReturn(null);

            inAppRepository.postEvent("e", null, callback);

            ArgumentCaptor<Result<Resource, PostEventException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(resultCaptor.capture());
            Result<Resource, PostEventException> value = resultCaptor.getValue();
            Assert.assertFalse(value.isSuccess());
            Assert.assertTrue(value.getException() instanceof PostEventException);
            Assert.assertEquals("Request Manager is null", value.getException().getMessage());
        }
    }

    // Verifies that mergeUserId emits MergeUserException when RequestManager is unavailable.
    @Test
    public void mergeUserId_requestManagerNull_callbackReceivesMergeUserException() {
        WhiteboxHelper.setInternalState(inAppRepository, "requestManager", null);
        Callback<Void, MergeUserException> callback = CallbackWrapper.spy();

        try (MockedStatic<NetworkModule> networkModule = mockStatic(NetworkModule.class)) {
            networkModule.when(NetworkModule::getRequestManager).thenReturn(null);

            inAppRepository.mergeUserId("old", "new", true, callback);

            ArgumentCaptor<Result<Void, MergeUserException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(callback).process(resultCaptor.capture());
            Result<Void, MergeUserException> value = resultCaptor.getValue();
            Assert.assertFalse(value.isSuccess());
            Assert.assertTrue(value.getException() instanceof MergeUserException);
            Assert.assertEquals("Request Manager is null", value.getException().getMessage());
        }
    }

    // Verifies that mapToHtmlData returns ResourceParseException when resource is not in storage after inApps loaded.
    @Test
    public void mapToHtmlData_resourceMissingFromStorage_returnsResourceParseException() {
        Result<Object, NetworkException> emptyResult = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyResult);
        inAppRepository.loadInApps();

        when(inAppStorageMock.getResource("missing")).thenReturn(null);

        Resource resource = new Resource("missing", false);
        Result<HtmlData, ResourceParseException> result = inAppRepository.mapToHtmlData(resource);

        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getException());
        Assert.assertTrue(result.getException() instanceof ResourceParseException);
        Assert.assertTrue(result.getException().getMessage().contains("missing"));
    }

    // Verifies that mapToHtmlData returns ResourceParseException when ResourceMapper throws IOException.
    // Uses a Resource with non-null URL so isNotDownload() is false and we bypass the storage-lookup
    // branch (which would otherwise hit waitUntilObtainInApps's 5s Thread.sleep loop).
    @Test
    public void mapToHtmlData_mapperThrowsIOException_returnsResourceParseException() throws Exception {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(true);
        when(resourceMapperMock.map(resource)).thenThrow(new IOException("boom"));

        Result<HtmlData, ResourceParseException> result = inAppRepository.mapToHtmlData(resource);

        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getException());
        Assert.assertTrue(result.getException() instanceof ResourceParseException);
        Assert.assertTrue(result.getException().getMessage().contains("Can't mapping resource"));
    }

    // Verifies that mapToHtmlData returns ResourceParseException when download fails for non-deployed resource.
    // Uses a non-null URL to bypass the isNotDownload() block and reach downloadIfNeeded directly.
    @Test
    public void mapToHtmlData_downloadFails_returnsResourceParseException() {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(false);
        when(inAppDownloaderMock.isDownloading(any(Resource.class))).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(DownloadResult.empty());

        Result<HtmlData, ResourceParseException> result = inAppRepository.mapToHtmlData(resource);

        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getException());
        Assert.assertTrue(result.getException() instanceof ResourceParseException);
        Assert.assertTrue(result.getException().getMessage().contains("Can't download or update"));
    }
}
