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

import android.os.Handler;

import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.CacheFailedRequestCallback;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.event.InAppEvent;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.mapper.ResourceMapper;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.downloader.DownloadResult;
import com.pushwoosh.inapp.network.downloader.InAppDownloader;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.testutil.CallbackWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by aevstefeev on 07/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InAppRepositoryTest {
    public static final String TEST_EXCEPTION_STRING = "test_exception";
    public static final NetworkException EXCEPTION = new NetworkException(TEST_EXCEPTION_STRING);
    public static final String TEST_EXCEPTION = "TEST_EXCEPTION";
    public static final String RICH_MEDIA = "{\"url\":\"https:\\/\\/richmedia-01.pushwoosh.com\\/9\\/F\\/9F5CD-8579F.zip\",\"code\":\"9AFBB-234CC\",\"layout\":\"topbanner\",\"updated\":1524913801,\"closeButtonType\":0,\"hash\":\"2b690544a8d9da7cd7f7340b40251ea5\",\"required\":true,\"priority\":0, \"ts\":0, \"businessCase\":\"\",\"gdpr\":\"Delete\"}";
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

        inAppRepository = new InAppRepository(requestManagerMock, inAppStorageMock,
                inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock, platformTestManager.getRegistrationPrefs());

        Whitebox.setInternalState(inAppRepository, "inAppDeployedChecker", inAppDeployedCheckerMock);


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
    public void setUserId() throws Exception {
        String userId = "user123";
        inAppRepository.setUserId(userId);
        ArgumentCaptor<RegisterUserRequest> requestCaptor = ArgumentCaptor.forClass(RegisterUserRequest.class);
        verify(requestManagerMock)
                .sendRequest(requestCaptor.capture(), any(CacheFailedRequestCallback.class));
        JSONObject param = new JSONObject();
        requestCaptor.getValue().buildParams(param);
        Assert.assertEquals(userId, param.get("userId"));
    }

    @Test
    public void postEvent() throws Exception {
        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        inAppRepository.postEvent("test_event", Tags.intTag("intTag", 5), callback);


        JSONObject response = new JSONObject();
        response.put("code", "test_code");
        response.put("required", "true");
        Result<PostEventResponse, NetworkException> result = Result.fromData(new PostEventResponse(response));
        emulatePostEventToNetwork(result);

        ArgumentCaptor<Result<Resource, PostEventException>> resultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultArgumentCaptor.capture());
        Assert.assertEquals(1, resultArgumentCaptor.getAllValues().size());
        Result<Resource, PostEventException> value = resultArgumentCaptor.getValue();
        Resource data = value.getData();
        Assert.assertEquals("test_code", data.getCode());
        Assert.assertEquals(true, data.isRequired());
    }

    private void emulatePostEventToNetwork(Result<PostEventResponse, NetworkException> result) throws JSONException {
        ArgumentCaptor<Callback<PostEventResponse, NetworkException>> callbackNetworkArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
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

        ArgumentCaptor<Result<Resource, PostEventException>> resultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultArgumentCaptor.capture());
        Assert.assertEquals(1, resultArgumentCaptor.getAllValues().size());
        Result<Resource, PostEventException> value = resultArgumentCaptor.getValue();
        Assert.assertEquals(TEST_EXCEPTION_STRING, value.getException().getMessage());
    }

    @Test
    public void mergeUserId() throws Exception {
        Callback<Void, MergeUserException> callback = CallbackWrapper.spy();
        inAppRepository.mergeUserId("1", "2", true, callback);

        ArgumentCaptor<Callback<Void, NetworkException>> callbackNetworkArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<MergeUserRequest> mergeUserRequestArgumentCaptor = ArgumentCaptor.forClass(MergeUserRequest.class);
        verify(requestManagerMock).sendRequest(mergeUserRequestArgumentCaptor.capture(), callbackNetworkArgumentCaptor.capture());
        callbackNetworkArgumentCaptor.getValue().process(Result.fromData(null));

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
        Assert.assertTrue(resultList.get(0).isSuccess());
    }

    @Test
    public void mergeUserIdServerError() throws Exception {
        Callback<Void, MergeUserException> callback = CallbackWrapper.spy();
        inAppRepository.mergeUserId("1", "2", true, callback);

        ArgumentCaptor<Callback<Void, NetworkException>> callbackNetworkArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        ArgumentCaptor<MergeUserRequest> mergeUserRequestArgumentCaptor = ArgumentCaptor.forClass(MergeUserRequest.class);
        verify(requestManagerMock).sendRequest(mergeUserRequestArgumentCaptor.capture(), callbackNetworkArgumentCaptor.capture());
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
        Assert.assertEquals(TEST_EXCEPTION, mergeUserExceptionResult.getException().getMessage());

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

        Assert.assertEquals("Can't download or update richMedia: r-9F5CD-8579F", result.getException().getMessage());
    }

    @Test
    public void prefetchRichMediaDeploy() throws Exception {
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(false);
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(Resource.parseRichMedia(RICH_MEDIA));
        DownloadResult downloadResult = DownloadResult.success(resourceList);
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(downloadResult);
        Result<Resource, ResourceParseException> result = inAppRepository.prefetchRichMedia(RICH_MEDIA);

        Assert.assertNull(result.getException());
    }

    @Test
    @Ignore
    public void prefetchRichMediaIsDownload() throws Exception {
        //todo fix this test
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(false);
        when(inAppDownloaderMock.isDownloading(any(Resource.class))).thenReturn(true);
        List<Resource> resourceList = new ArrayList<>();
        Resource resource = Resource.parseRichMedia(RICH_MEDIA);
        resourceList.add(resource);
        DownloadResult downloadResult = DownloadResult.success(resourceList);
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(downloadResult);


        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            EventBus.sendEvent(new InAppEvent(InAppEvent.EventType.DEPLOYED, resource));
        });


        Result<Resource, ResourceParseException> result = inAppRepository.prefetchRichMedia(RICH_MEDIA);

        Assert.assertNull(result.getException());
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

}