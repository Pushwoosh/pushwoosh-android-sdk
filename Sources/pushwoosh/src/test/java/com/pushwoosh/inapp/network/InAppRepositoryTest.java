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
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.pushwoosh.inapp.storage.InAppDbHelper;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.inapp.view.InAppViewEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RepositoryModule;
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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

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

    private File statefulRoot;
    private InAppDbHelper realDbHelper;
    private InAppRepository statefulRepository;
    private InAppDownloader statefulDownloaderMock;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        requestManagerMock = mock(RequestManager.class);
        NetworkModule.setRequestManager(requestManagerMock);
        inAppStorageMock = mock(InAppStorage.class);
        inAppFolderProviderMock = mock(InAppFolderProvider.class);
        resourceMapperMock = mock(ResourceMapper.class);
        inAppDownloaderMock = mock(InAppDownloader.class);
        inAppDeployedCheckerMock = mock(InAppDeployedChecker.class);

        inAppRepository =
                new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);

        WhiteboxHelper.setInternalState(inAppRepository, "inAppDeployedChecker", inAppDeployedCheckerMock);
    }

    @After
    public void tearDown() {
        InAppRepository.downloadJoinTimeoutMs = 60_000;
        if (realDbHelper != null) {
            realDbHelper.close();
        }
        if (statefulRoot != null) {
            FileUtils.deleteDirectory(statefulRoot);
        }
        EventBus.clearSubscribersMap();
        NetworkModule.setRequestManager(null);
        platformTestManager.tearDown();
    }

    /** Stateful rig: real InAppDbHelper + real InAppDeployedChecker over a tmp folder. */
    private void setUpStatefulRepository() throws Exception {
        statefulRoot = Files.createTempDirectory("inapp-ondemand").toFile();
        realDbHelper = new InAppDbHelper(RuntimeEnvironment.application);

        InAppFolderProvider folderProvider = mock(InAppFolderProvider.class);
        when(folderProvider.getInAppFolder(Mockito.anyString()))
                .thenAnswer(inv -> new File(statefulRoot, (String) inv.getArgument(0)));
        when(folderProvider.getInAppHtmlFile(Mockito.anyString()))
                .thenAnswer(inv -> new File(new File(statefulRoot, (String) inv.getArgument(0)), "index.html"));
        when(folderProvider.getNativeConfigFile(Mockito.anyString()))
                .thenAnswer(inv -> new File(new File(statefulRoot, (String) inv.getArgument(0)), "native-config.json"));

        statefulDownloaderMock = mock(InAppDownloader.class);
        when(statefulDownloaderMock.downloadAndDeploy(anyList())).thenAnswer(inv -> deployFiles(inv.getArgument(0)));
        Mockito.doAnswer(inv -> {
                    FileUtils.deleteDirectory(new File(statefulRoot, (String) inv.getArgument(0)));
                    return null;
                })
                .when(statefulDownloaderMock)
                .removeResourceFiles(Mockito.anyString());

        statefulRepository =
                new InAppRepository(realDbHelper, statefulDownloaderMock, resourceMapperMock, folderProvider);
    }

    /** Mirror of the real downloader for the stateful rig: wipe, then unpack index.html per resource. */
    private DownloadResult deployFiles(List<Resource> resources) throws IOException {
        for (Resource r : resources) {
            File dir = new File(statefulRoot, r.getCode());
            // Mirror the real downloader: deleteInAppFolder() wipes before unpacking.
            FileUtils.deleteDirectory(dir);
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            Files.write(
                    new File(dir, "index.html").toPath(),
                    String.valueOf(r.getUpdated()).getBytes(StandardCharsets.UTF_8));
        }
        return DownloadResult.success(resources);
    }

    /** Gated downloader: signals downloadStarted, holds until the test opens the gate, then deploys or fails. */
    private void stubGatedDownload(CountDownLatch downloadStarted, CountDownLatch gate, boolean succeed) {
        when(statefulDownloaderMock.downloadAndDeploy(anyList())).thenAnswer(inv -> {
            downloadStarted.countDown();
            if (!gate.await(8, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test gate was never opened");
            }
            return succeed ? deployFiles(inv.getArgument(0)) : DownloadResult.empty();
        });
    }

    /** Waits until b parks in the leader's latch: other.await(timeout) is its only timed wait on the way there. */
    private static void awaitJoined(Thread b) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (b.getState() != Thread.State.TIMED_WAITING) {
            if (System.currentTimeMillis() > deadline) {
                Assert.fail("joiner never reached the latch");
            }
            Thread.sleep(5);
        }
    }

    // Two on-demand shows of the same push rich media must download once: the first
    // ensure call has to leave a DB row so the second one passes InAppDeployedChecker.
    @Test
    public void ensureResolvedAndDeployed_calledTwiceWithoutPrefetch_downloadsOnce() throws Exception {
        setUpStatefulRepository();
        Resource pushRichMedia =
                new Resource("r-AAAA-BBBB", "https://cdn.example.com/r.zip", "", 100L, InAppLayout.TOP, null, false, 0);

        Result<Resource, ResourceParseException> first = statefulRepository.ensureResolvedAndDeployed(pushRichMedia);
        Result<Resource, ResourceParseException> second = statefulRepository.ensureResolvedAndDeployed(pushRichMedia);

        Assert.assertTrue(first.isSuccess());
        Assert.assertTrue(second.isSuccess());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
    }

    // Old-notification tap (ts=1) against a ts=2 row from prefetch: the row must follow the files
    // to ts=1, so the next ts=2 show misses and re-downloads instead of silently serving stale content.
    @Test
    public void ensureResolvedAndDeployed_olderTsAfterNewerRow_rewritesRowAndInvalidatesFiles() throws Exception {
        setUpStatefulRepository();
        String code = "r-AAAA-BBBB";
        Resource ts1 = new Resource(code, "https://cdn.example.com/r.zip", "", 1L, InAppLayout.TOP, null, false, 0);
        Resource ts2 = new Resource(code, "https://cdn.example.com/r.zip", "", 2L, InAppLayout.TOP, null, false, 0);

        // Simulate receive-time prefetch of ts2: DB row + deployed files on disk.
        realDbHelper.saveOrUpdateResources(Collections.singletonList(ts2));
        File dir = new File(statefulRoot, code);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(dir, "index.html").createNewFile();

        Result<Resource, ResourceParseException> oldShow = statefulRepository.ensureResolvedAndDeployed(ts1);

        Assert.assertTrue(oldShow.isSuccess());
        Assert.assertEquals(1L, realDbHelper.getResource(code).getUpdated());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
        // Fresh ts1 files must survive the row write: a wipe here would be updateInAppStorage's.
        Assert.assertEquals(
                "1", new String(Files.readAllBytes(new File(dir, "index.html").toPath()), StandardCharsets.UTF_8));

        // Today (without the fix) this second show would pass check and show ts1 content under ts2.
        Result<Resource, ResourceParseException> newShow = statefulRepository.ensureResolvedAndDeployed(ts2);

        Assert.assertTrue(newShow.isSuccess());
        Assert.assertEquals(2L, realDbHelper.getResource(code).getUpdated());
        verify(statefulDownloaderMock, Mockito.times(2)).downloadAndDeploy(anyList());
    }

    // Stale files without a DB row (what every pre-fix on-demand show left behind): a newer ts must
    // fetch fresh content, not stamp its ts over the old files and serve them as a cache hit forever.
    @Test
    public void ensureResolvedAndDeployed_staleFilesWithoutRow_downloadsFreshContent() throws Exception {
        setUpStatefulRepository();
        String code = "r-AAAA-BBBB";
        File dir = new File(statefulRoot, code);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File html = new File(dir, "index.html");
        Files.write(html.toPath(), "7".getBytes(StandardCharsets.UTF_8));
        Assert.assertNull("precondition: files without a row", realDbHelper.getResource(code));

        Resource ts42 = new Resource(code, "https://cdn.example.com/r.zip", "", 42L, InAppLayout.TOP, null, false, 0);
        Result<Resource, ResourceParseException> result = statefulRepository.ensureResolvedAndDeployed(ts42);

        Assert.assertTrue(result.isSuccess());
        verify(statefulDownloaderMock).downloadAndDeploy(anyList());
        Assert.assertEquals("42", new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8));
        Assert.assertEquals(42L, realDbHelper.getResource(code).getUpdated());
    }

    // Prefetch of a new version over a deployed old one must not touch the row (and must not
    // queue a wipe) until the download succeeds: the pre-write + wipe combo raced concurrent
    // shows of the same code and deleted freshly deployed files (SDK-957).
    @Test(timeout = 10_000)
    public void prefetchRichMedia_newTsOverDeployedOldTs_writesRowOnlyAfterDownload() throws Exception {
        setUpStatefulRepository();
        String code = "r-AAAA-BBBB";
        Resource ts1 =
                new Resource(code, "https://cdn.example.com/AAAA-BBBB.zip", "", 1L, InAppLayout.TOP, null, false, 0);
        realDbHelper.saveOrUpdateResources(Collections.singletonList(ts1));
        File dir = new File(statefulRoot, code);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        Files.write(new File(dir, "index.html").toPath(), "1".getBytes(StandardCharsets.UTF_8));

        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, true);

        String newTsJson = "{\"url\":\"https://cdn.example.com/AAAA-BBBB.zip\",\"ts\":2}";
        Thread prefetch = new Thread(() -> statefulRepository.prefetchRichMedia(newTsJson));
        prefetch.start();
        Assert.assertTrue("download never started", downloadStarted.await(5, TimeUnit.SECONDS));

        // Gate closed: the row must still be ts1 and no wipe may have been queued.
        Assert.assertEquals(1L, realDbHelper.getResource(code).getUpdated());
        verify(statefulDownloaderMock, Mockito.never()).removeResourceFiles(Mockito.anyString());

        gate.countDown();
        prefetch.join(5_000);

        Assert.assertEquals(2L, realDbHelper.getResource(code).getUpdated());
        Assert.assertEquals(
                "2", new String(Files.readAllBytes(new File(dir, "index.html").toPath()), StandardCharsets.UTF_8));
        verify(statefulDownloaderMock, Mockito.never()).removeResourceFiles(Mockito.anyString());
    }

    // Two concurrent shows of one code: the leader downloads once, the joiner waits on the
    // leader's latch and takes its truth from check.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_concurrentShowsOfSameCode_downloadOnceBothSucceed() throws Exception {
        setUpStatefulRepository();
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, true);
        Resource resource =
                new Resource("r-AAAA-BBBB", "https://cdn.example.com/r.zip", "", 100L, InAppLayout.TOP, null, false, 0);

        AtomicBoolean aResult = new AtomicBoolean();
        AtomicBoolean bResult = new AtomicBoolean();
        Thread a = new Thread(() -> aResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));
        Thread b = new Thread(() -> bResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));

        a.start();
        Assert.assertTrue("leader never reached the downloader", downloadStarted.await(5, TimeUnit.SECONDS));
        b.start();
        awaitJoined(b);
        gate.countDown();
        a.join(5_000);
        b.join(5_000);

        Assert.assertTrue(aResult.get());
        Assert.assertTrue(bResult.get());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
        Assert.assertEquals(100L, realDbHelper.getResource("r-AAAA-BBBB").getUpdated());
    }

    // Failed leader: joiner gets an honest false (no retry), and the finally-cleanup makes the
    // very next call a fresh leader instead of a joiner of a dead latch.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_leaderFails_bothFalseAndNextCallLeadsAgain() throws Exception {
        setUpStatefulRepository();
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, false);
        Resource resource =
                new Resource("r-AAAA-BBBB", "https://cdn.example.com/r.zip", "", 100L, InAppLayout.TOP, null, false, 0);

        AtomicBoolean aResult = new AtomicBoolean(true);
        AtomicBoolean bResult = new AtomicBoolean(true);
        Thread a = new Thread(() -> aResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));
        Thread b = new Thread(() -> bResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));

        a.start();
        Assert.assertTrue(downloadStarted.await(5, TimeUnit.SECONDS));
        b.start();
        awaitJoined(b);
        gate.countDown();
        a.join(5_000);
        b.join(5_000);

        Assert.assertFalse(aResult.get());
        Assert.assertFalse(bResult.get());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
        Assert.assertNull("failed leader must not leave a row", realDbHelper.getResource("r-AAAA-BBBB"));

        // map cleaned in finally: the retry must lead and download, not join a dead latch
        when(statefulDownloaderMock.downloadAndDeploy(anyList())).thenAnswer(inv -> deployFiles(inv.getArgument(0)));
        Assert.assertTrue(statefulRepository.ensureResolvedAndDeployed(resource).isSuccess());
        verify(statefulDownloaderMock, Mockito.times(2)).downloadAndDeploy(anyList());
    }

    // Slow leader: the joiner gives up after downloadJoinTimeoutMs with false while the leader
    // is still inside the downloader, and the leader itself finishes with true.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_joinerTimesOut_falseWhileLeaderStillDownloading() throws Exception {
        setUpStatefulRepository();
        InAppRepository.downloadJoinTimeoutMs = 200;
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, true);
        Resource resource =
                new Resource("r-AAAA-BBBB", "https://cdn.example.com/r.zip", "", 100L, InAppLayout.TOP, null, false, 0);

        AtomicBoolean aResult = new AtomicBoolean();
        Thread a = new Thread(() -> aResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));
        a.start();
        Assert.assertTrue(downloadStarted.await(5, TimeUnit.SECONDS));

        // joiner runs on the test thread: with the gate still closed it must give up in ~200ms
        Result<Resource, ResourceParseException> joiner = statefulRepository.ensureResolvedAndDeployed(resource);
        Assert.assertFalse(joiner.isSuccess());
        Assert.assertTrue("leader must still be downloading when the joiner gives up", a.isAlive());

        gate.countDown();
        a.join(5_000);
        Assert.assertTrue(aResult.get());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
    }

    // The map is keyed by code: a ts2 show joins the in-flight ts1 download, wakes up, and its
    // check against the ts1 row is an honest false. It must not download and must not wipe ts1 files.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_joinerWithDifferentTs_falseAndLeaderFilesSurvive() throws Exception {
        setUpStatefulRepository();
        String code = "r-AAAA-BBBB";
        Resource ts1 = new Resource(code, "https://cdn.example.com/r.zip", "", 1L, InAppLayout.TOP, null, false, 0);
        Resource ts2 = new Resource(code, "https://cdn.example.com/r.zip", "", 2L, InAppLayout.TOP, null, false, 0);
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, true);

        AtomicBoolean aResult = new AtomicBoolean();
        AtomicBoolean bResult = new AtomicBoolean(true);
        Thread a = new Thread(() ->
                aResult.set(statefulRepository.ensureResolvedAndDeployed(ts1).isSuccess()));
        Thread b = new Thread(() ->
                bResult.set(statefulRepository.ensureResolvedAndDeployed(ts2).isSuccess()));

        a.start();
        Assert.assertTrue(downloadStarted.await(5, TimeUnit.SECONDS));
        b.start();
        awaitJoined(b);
        gate.countDown();
        a.join(5_000);
        b.join(5_000);

        Assert.assertTrue(aResult.get());
        Assert.assertFalse(bResult.get());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
        Assert.assertEquals(1L, realDbHelper.getResource(code).getUpdated());
        Assert.assertEquals(
                "1",
                new String(
                        Files.readAllBytes(new File(new File(statefulRoot, code), "index.html").toPath()),
                        StandardCharsets.UTF_8));
    }

    // loadInApps goes through downloadIfNeeded one code at a time: a show of the code being fetched
    // right now joins that code's download and wakes when it lands, not when the whole batch does.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_showDuringLoadInAppsBatch_wakesOnItsCodeNotWholeBatch() throws Exception {
        setUpStatefulRepository();
        Resource a =
                new Resource("AAAA-1111", "https://cdn.example.com/a.zip", "", 100L, InAppLayout.TOP, null, true, 0);
        Resource b =
                new Resource("BBBB-2222", "https://cdn.example.com/b.zip", "", 100L, InAppLayout.TOP, null, true, 0);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(Result.fromData(Arrays.asList(a, b)));
        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch aGate = new CountDownLatch(1);
        CountDownLatch bGate = new CountDownLatch(1);
        when(statefulDownloaderMock.downloadAndDeploy(anyList())).thenAnswer(inv -> {
            List<Resource> resources = inv.getArgument(0);
            boolean isA = "AAAA-1111".equals(resources.get(0).getCode());
            if (isA) {
                aStarted.countDown();
            }
            if (!(isA ? aGate : bGate).await(8, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test gate was never opened");
            }
            return deployFiles(resources);
        });

        AtomicBoolean showResult = new AtomicBoolean();
        Thread batch = new Thread(() -> statefulRepository.loadInApps());
        Thread show = new Thread(() ->
                showResult.set(statefulRepository.ensureResolvedAndDeployed(a).isSuccess()));

        batch.start();
        Assert.assertTrue("batch never reached the downloader", aStarted.await(5, TimeUnit.SECONDS));
        show.start();
        awaitJoined(show);
        aGate.countDown();
        show.join(5_000);

        Assert.assertTrue("show must wake as soon as its own code lands", showResult.get());
        Assert.assertTrue("batch must still be fetching the next code", batch.isAlive());
        bGate.countDown();
        batch.join(5_000);

        verify(statefulDownloaderMock, Mockito.times(2)).downloadAndDeploy(anyList());
        Assert.assertEquals(
                "100",
                new String(
                        Files.readAllBytes(new File(new File(statefulRoot, "AAAA-1111"), "index.html").toPath()),
                        StandardCharsets.UTF_8));
    }

    // Check runs under the claim, not before it. The gated checker lets the joiner's second check capture
    // the truth and then holds it until the leader is gone: with check-first that second check is the
    // pre-claim one, it captures false, and the joiner lands in an empty map as a second leader that
    // downloads again. With check-under-claim it is the post-await check and sees the leader's row.
    @Test(timeout = 10_000)
    public void downloadIfNeeded_checkRunsUnderClaim_lateSecondCallerJoinsInsteadOfLeading() throws Exception {
        setUpStatefulRepository();
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch leaderDone = new CountDownLatch(1);
        stubGatedDownload(downloadStarted, gate, true);
        Resource resource =
                new Resource("r-AAAA-BBBB", "https://cdn.example.com/r.zip", "", 100L, InAppLayout.TOP, null, false, 0);

        InAppDeployedChecker realChecker =
                (InAppDeployedChecker) WhiteboxHelper.getInternalState(statefulRepository, "inAppDeployedChecker");
        InAppDeployedChecker gatedChecker = mock(InAppDeployedChecker.class);
        AtomicInteger joinerChecks = new AtomicInteger();
        when(gatedChecker.check(any(Resource.class))).thenAnswer(inv -> {
            boolean deployed = realChecker.check(inv.getArgument(0));
            boolean secondJoinerCheck =
                    "joiner".equals(Thread.currentThread().getName()) && joinerChecks.incrementAndGet() == 2;
            if (secondJoinerCheck && !leaderDone.await(8, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test gate leaderDone was never opened");
            }
            return deployed;
        });
        WhiteboxHelper.setInternalState(statefulRepository, "inAppDeployedChecker", gatedChecker);

        AtomicBoolean aResult = new AtomicBoolean();
        AtomicBoolean bResult = new AtomicBoolean();
        Thread a = new Thread(() -> aResult.set(
                statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()));
        Thread b = new Thread(
                () -> bResult.set(
                        statefulRepository.ensureResolvedAndDeployed(resource).isSuccess()),
                "joiner");

        a.start();
        Assert.assertTrue("leader never reached the downloader", downloadStarted.await(5, TimeUnit.SECONDS));
        b.start();
        awaitJoined(b);
        gate.countDown();
        a.join(5_000);
        leaderDone.countDown();
        b.join(5_000);

        Assert.assertTrue(aResult.get());
        Assert.assertTrue(bResult.get());
        verify(statefulDownloaderMock, Mockito.times(1)).downloadAndDeploy(anyList());
    }

    // The batch is downloaded one code per downloader call, in Resource order: required first, then code.
    @Test
    public void loadInApps() throws Exception {
        Resource optional3 = new Resource("3", false);
        Resource required1 = new Resource("1", true);
        Resource required2 = new Resource("2", true);
        List<Resource> resourceList = new ArrayList<>(Arrays.asList(optional3, required1, required2));

        List<String> codeList = new ArrayList<>();
        codeList.add("1");
        codeList.add("2");
        codeList.add("3");

        Result<Object, NetworkException> getInAppsResult = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(getInAppsResult);
        when(inAppStorageMock.saveOrUpdateResources(resourceList)).thenReturn(codeList);
        when(inAppDownloaderMock.downloadAndDeploy(anyList()))
                .thenAnswer(inv -> DownloadResult.success(inv.getArgument(0)));

        Result<Void, NetworkException> result = inAppRepository.loadInApps();

        Assert.assertNull(result.getData());
        verify(inAppDownloaderMock, Mockito.times(3)).removeResourceFiles(Mockito.anyString());
        verify(inAppStorageMock).saveOrUpdateResources(resourceList);
        InOrder inOrder = Mockito.inOrder(inAppDownloaderMock);
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(required1));
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(required2));
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(optional3));
        verify(requestManagerMock).sendRequestSync(any());
    }

    // A failed code must not stop the batch: every remaining code still gets its own downloader call.
    @Test
    public void loadInApps_oneCodeFails_continuesWithRemainingCodes() {
        Resource required1 = new Resource("1", true);
        Resource required2 = new Resource("2", true);
        Resource required3 = new Resource("3", true);
        List<Resource> resourceList = new ArrayList<>(Arrays.asList(required1, required2, required3));
        Result<Object, NetworkException> getInAppsResult = Result.fromData(resourceList);
        when(requestManagerMock.sendRequestSync(any())).thenReturn(getInAppsResult);
        when(inAppStorageMock.saveOrUpdateResources(anyList())).thenReturn(Collections.emptyList());
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenAnswer(inv -> {
            List<Resource> batch = inv.getArgument(0);
            return "1".equals(batch.get(0).getCode()) ? DownloadResult.empty() : DownloadResult.success(batch);
        });

        inAppRepository.loadInApps();

        verify(inAppDownloaderMock, Mockito.times(3)).downloadAndDeploy(anyList());
        InOrder inOrder = Mockito.inOrder(inAppDownloaderMock);
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(required1));
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(required2));
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(required3));
    }

    @Test
    public void postEvent() throws Exception {
        List<Resource> resources = new ArrayList<>();
        resources.add(new Resource("test_code", true));
        when(requestManagerMock.sendRequestSync(any())).thenReturn(Result.from(resources, null));

        when(inAppDownloaderMock.downloadAndDeploy(any())).thenReturn(DownloadResult.success(resources));

        ExecutorService directIo = InAppExecutorServiceHelper.createExecutorService();

        WhiteboxHelper.setInternalState(inAppRepository, "io", directIo);

        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        inAppRepository.postEvent("test_event", Tags.intTag("intTag", 5), callback);

        JSONObject response = new JSONObject();
        response.put("code", "test_code");
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
        verify(inAppStorageMock, Mockito.never()).saveOrUpdateResources(anyList());
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

    // ensureResolvedAndDeployed: deployed resource short-circuits — no network, no downloader, no DB write.
    @Test
    public void ensureResolvedAndDeployed_alreadyDeployed_fastPathWithoutNetwork() {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(resource)).thenReturn(true);

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(resource);

        Assert.assertTrue(result.isSuccess());
        Assert.assertSame(resource, result.getData());
        verify(inAppDownloaderMock, never()).downloadAndDeploy(anyList());
        verify(requestManagerMock, never()).sendRequestSync(any());
        verify(inAppStorageMock, never()).saveOrUpdateResources(anyList());
    }

    // ensureResolvedAndDeployed: not deployed -> downloadIfNeeded path is taken.
    @Test
    public void ensureResolvedAndDeployed_notDeployed_downloads() {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(resource)).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList()))
                .thenReturn(DownloadResult.success(Collections.singletonList(resource)));

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(resource);

        Assert.assertTrue(result.isSuccess());
        verify(inAppDownloaderMock).downloadAndDeploy(Collections.singletonList(resource));
    }

    // ensureResolvedAndDeployed: code-only in-app resolves the full Resource from storage.
    @Test
    public void ensureResolvedAndDeployed_inAppByCode_resolvesFromStorage() {
        Result<Object, NetworkException> emptyResult = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyResult);
        inAppRepository.loadInApps(); // sets inAppLoaded=true

        Resource stub = new Resource("code1", false);
        Resource full =
                new Resource("code1", "http://example.com/z.zip", "h", 5L, InAppLayout.FULLSCREEN, null, false, 0);
        when(inAppStorageMock.getResource("code1")).thenReturn(full);
        when(inAppDeployedCheckerMock.check(full)).thenReturn(true);

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(stub);

        Assert.assertTrue(result.isSuccess());
        Assert.assertSame(full, result.getData());
    }

    // ensureResolvedAndDeployed: unknown code -> error Result, message carries the code.
    @Test
    public void ensureResolvedAndDeployed_missingFromStorage_returnsError() {
        Result<Object, NetworkException> emptyResult = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyResult);
        inAppRepository.loadInApps();

        when(inAppStorageMock.getResource("missing")).thenReturn(null);

        Result<Resource, ResourceParseException> result =
                inAppRepository.ensureResolvedAndDeployed(new Resource("missing", false));

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getException().getMessage().contains("missing"));
    }

    // ensureResolvedAndDeployed: storage throws during resolve -> wrapped ResourceParseException.
    @Test
    public void ensureResolvedAndDeployed_resolveThrows_returnsWrappedError() {
        Result<Object, NetworkException> emptyResult = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyResult);
        inAppRepository.loadInApps();

        when(inAppStorageMock.getResource("boom")).thenThrow(new RuntimeException("storage failure"));

        Result<Resource, ResourceParseException> result =
                inAppRepository.ensureResolvedAndDeployed(new Resource("boom", false));

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getException().getMessage().contains("Can't download or update"));
    }

    // ensureResolvedAndDeployed: download failure -> error Result.
    @Test
    public void ensureResolvedAndDeployed_downloadFails_returnsError() {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(resource)).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(DownloadResult.empty());

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(resource);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getException().getMessage().contains("Can't download or update"));
    }

    // On-demand miss with a url writes the row AFTER a successful download, so the row never
    // describes files that are not on disk yet.
    @Test
    public void ensureResolvedAndDeployed_missWithUrl_writesRowAfterDownload() {
        Resource resource =
                new Resource("1", "http://example.com/inapp", null, 0L, InAppLayout.FULLSCREEN, null, true, -1);
        when(inAppDeployedCheckerMock.check(resource)).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList()))
                .thenReturn(DownloadResult.success(Collections.singletonList(resource)));

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(resource);

        Assert.assertTrue(result.isSuccess());
        InOrder inOrder = Mockito.inOrder(inAppDownloaderMock, inAppStorageMock);
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(anyList());
        inOrder.verify(inAppStorageMock).saveOrUpdateResources(Collections.singletonList(resource));
    }

    // Outer check missed, inner hit (another thread deployed the code in between): success without
    // a download or a row write. Kills removal of the early return at the top of downloadIfNeeded.
    @Test
    public void ensureResolvedAndDeployed_innerCheckHit_skipsDownloadAndWrite() {
        Resource stub = new Resource("code1", false);
        when(inAppDeployedCheckerMock.check(stub)).thenReturn(false, true);

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(stub);

        Assert.assertTrue(result.isSuccess());
        verify(inAppDownloaderMock, never()).downloadAndDeploy(anyList());
        verify(inAppStorageMock, never()).saveOrUpdateResources(anyList());
    }

    // Verifies that a non-required code-only in-app does not resolve from storage while the in-app
    // list is not loaded yet and goes straight to the download path with the stub.
    // Kills L497 mutant that makes inAppLoaded.get() always true: the mutant enters the resolve
    // block immediately and "successfully" resolves the baited full resource from storage.
    @Test
    public void ensureResolvedAndDeployed_notRequiredListNotLoaded_skipsStorageResolve() {
        Resource stub = new Resource("code1", false);
        Resource full =
                new Resource("code1", "http://example.com/z.zip", "h", 5L, InAppLayout.FULLSCREEN, null, false, 0);
        when(inAppStorageMock.getResource("code1")).thenReturn(full);
        when(inAppDeployedCheckerMock.check(full)).thenReturn(true);
        when(inAppDeployedCheckerMock.check(stub)).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(DownloadResult.empty());

        Result<Resource, ResourceParseException> result = inAppRepository.ensureResolvedAndDeployed(stub);

        verify(inAppStorageMock, never()).getResource(Mockito.anyString());
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getException().getMessage().contains("Can't download or update"));
        verify(inAppDownloaderMock).downloadAndDeploy(anyList());
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

    // Verifies that setUserId pulls the current manager from NetworkModule on every call.
    @Test
    public void setUserId_usesManagerFromNetworkModule() {
        RequestManager fallbackManager = mock(RequestManager.class);
        NetworkModule.setRequestManager(fallbackManager);

        inAppRepository.setUserId("user42", null);

        verify(fallbackManager).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
        verify(requestManagerMock, never()).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
    }

    // Verifies that setUserId reports the seam's terminal error instead of silently dropping the callback.
    @Test
    public void setUserId_sdkNotInitialized_callbackReceivesSetUserIdException() {
        NetworkModule.setRequestManager(null);
        Callback<Boolean, SetUserIdException> callback = CallbackWrapper.spy();

        inAppRepository.setUserId("user42", callback);

        ArgumentCaptor<Result<Boolean, SetUserIdException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, SetUserIdException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof SetUserIdException);
        Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
        verify(requestManagerMock, never()).sendRequest(any(RegisterUserRequest.class), any(Callback.class));
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

    // Verifies that richMediaAction emits RichMediaActionException when the SDK is not initialized.
    @Test
    public void richMediaAction_sdkNotInitialized_callbackReceivesRichMediaActionException() {
        NetworkModule.setRequestManager(null);
        Callback<Void, RichMediaActionException> callback = CallbackWrapper.spy();

        inAppRepository.richMediaAction("rich", "inapp", "hash", "{}", 1, callback);

        ArgumentCaptor<Result<Void, RichMediaActionException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Void, RichMediaActionException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof RichMediaActionException);
        Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
    }

    // Verifies that postEvent emits PostEventException when the SDK is not initialized.
    @Test
    public void postEvent_sdkNotInitialized_callbackReceivesPostEventException() {
        NetworkModule.setRequestManager(null);
        Callback<Resource, PostEventException> callback = CallbackWrapper.spy();

        inAppRepository.postEvent("e", null, callback);

        ArgumentCaptor<Result<Resource, PostEventException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Resource, PostEventException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof PostEventException);
        Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
    }

    // Verifies that mergeUserId emits MergeUserException when the SDK is not initialized.
    @Test
    public void mergeUserId_sdkNotInitialized_callbackReceivesMergeUserException() {
        NetworkModule.setRequestManager(null);
        Callback<Void, MergeUserException> callback = CallbackWrapper.spy();

        inAppRepository.mergeUserId("old", "new", true, callback);

        ArgumentCaptor<Result<Void, MergeUserException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Void, MergeUserException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertTrue(value.getException() instanceof MergeUserException);
        Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
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
    // Uses a Resource with non-null URL so isNotDownload() is false and the storage-lookup branch is bypassed.
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
        when(inAppDownloaderMock.downloadAndDeploy(anyList())).thenReturn(DownloadResult.empty());

        Result<HtmlData, ResourceParseException> result = inAppRepository.mapToHtmlData(resource);

        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getException());
        Assert.assertTrue(result.getException() instanceof ResourceParseException);
        Assert.assertTrue(result.getException().getMessage().contains("Can't download or update"));
    }

    private static final class EntryPointCase {
        final String name;
        final Runnable invoke;
        final BiConsumer<RequestManager, VerificationMode> verifyRequestSent;

        EntryPointCase(String name, Runnable invoke, BiConsumer<RequestManager, VerificationMode> verifyRequestSent) {
            this.name = name;
            this.invoke = invoke;
            this.verifyRequestSent = verifyRequestSent;
        }
    }

    // Verifies that every request entry point re-reads the manager from NetworkModule on each call, so a
    // repository built before NetworkModule.init() still reaches the real manager installed later.
    // Each entry point is called once per installed manager: a repository caching the manager — either at
    // construction or on first use — would send the second round to the first manager and fail.
    @Test
    public void requestEntryPoints_managerReplacedBetweenCalls_sendThroughCurrentManagerEachTime() {
        Result<Object, NetworkException> emptyInApps = Result.fromData(Collections.emptyList());
        when(requestManagerMock.sendRequestSync(any())).thenReturn(emptyInApps);
        RequestManager laterManager = mock(RequestManager.class);
        when(laterManager.sendRequestSync(any())).thenReturn(emptyInApps);

        EntryPointCase[] cases = {
            new EntryPointCase("loadInApps", () -> inAppRepository.loadInApps(), (m, once) -> verify(m, once)
                    .sendRequestSync(any(GetInAppsRequest.class))),
            new EntryPointCase("setEmail", () -> inAppRepository.setEmail("a@x.com", null), (m, once) -> verify(m, once)
                    .sendRequest(any(RegisterEmailRequest.class), any(Callback.class))),
            new EntryPointCase(
                    "postEvent", () -> inAppRepository.postEvent("test_event", null, null), (m, once) -> verify(m, once)
                            .sendRequest(any(PostEventRequest.class), any(Callback.class))),
            new EntryPointCase(
                    "mergeUserId",
                    () -> inAppRepository.mergeUserId("old", "new", true, null),
                    (m, once) -> verify(m, once).sendRequest(any(MergeUserRequest.class), any(Callback.class))),
            new EntryPointCase(
                    "richMediaAction",
                    () -> inAppRepository.richMediaAction("rich", "inapp", "hash", "{}", 1, null),
                    (m, once) -> verify(m, once).sendRequest(any(RichMediaActionRequest.class), any(Callback.class))),
        };

        for (EntryPointCase c : cases) {
            c.invoke.run();
        }
        NetworkModule.setRequestManager(laterManager);
        for (EntryPointCase c : cases) {
            c.invoke.run();
        }

        for (EntryPointCase c : cases) {
            c.verifyRequestSent.accept(requestManagerMock, description("case " + c.name + ", manager installed first"));
            c.verifyRequestSent.accept(laterManager, description("case " + c.name + ", manager installed later"));
        }
    }

    // Verifies that the in-app show analytics subscription sends its trigger request through the manager
    // currently installed in NetworkModule rather than the one present when the repository was built.
    @Test
    public void inAppViewEvent_managerReplacedBetweenShows_sendsTriggerRequestThroughCurrentManager() {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RequestManager laterManager = mock(RequestManager.class);

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false)));
        ShadowLooper.idleMainLooper();
        NetworkModule.setRequestManager(laterManager);
        EventBus.sendEvent(new InAppViewEvent(new Resource("code2", false)));
        ShadowLooper.idleMainLooper();

        verify(requestManagerMock).sendRequest(any(TriggerInAppActionRequest.class));
        verify(laterManager).sendRequest(any(TriggerInAppActionRequest.class));
    }

    // Verifies that the show analytics subscription still clears the stored message hash when the SDK is
    // not initialized: the seam absorbs the dropped request instead of throwing past the hash reset.
    @Test
    public void inAppViewEvent_sdkNotInitialized_stillClearsMessageHash() {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        NetworkModule.setRequestManager(null);
        RepositoryModule.getNotificationPreferences().messageHash().set("hash1");

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false)));
        ShadowLooper.idleMainLooper();

        Assert.assertNull(
                RepositoryModule.getNotificationPreferences().messageHash().get());
    }

    /** Captures the single show request the InAppViewEvent subscriber sent and returns its payload. */
    private JSONObject capturedShowParams() throws JSONException {
        ArgumentCaptor<TriggerInAppActionRequest> captor = ArgumentCaptor.forClass(TriggerInAppActionRequest.class);
        verify(requestManagerMock).sendRequest(captor.capture());
        JSONObject params = new JSONObject();
        captor.getValue().buildParams(params);
        return params;
    }

    // Verifies that an event carrying its own per-message hash is attributed with that hash and leaves a
    // foreign hash sitting in the global slot untouched: the neighbour message must keep its attribution.
    @Test
    public void inAppViewEvent_eventWithOwnHash_sendsOwnHashAndKeepsForeignSlot() throws JSONException {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RepositoryModule.getNotificationPreferences().messageHash().set("hash-neighbour");

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false), "hash-own"));
        ShadowLooper.idleMainLooper();

        JSONObject params = capturedShowParams();
        Assert.assertEquals("hash-own", params.getString("messageHash"));
        Assert.assertEquals(
                "hash-neighbour",
                RepositoryModule.getNotificationPreferences().messageHash().get());
    }

    // Verifies that the slot is consumed when it holds this very message's hash, so a later show cannot
    // read the same hash a second time.
    @Test
    public void inAppViewEvent_eventHashMatchesSlot_clearsSlot() {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RepositoryModule.getNotificationPreferences().messageHash().set("hash-own");

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false), "hash-own"));
        ShadowLooper.idleMainLooper();

        Assert.assertNull(
                RepositoryModule.getNotificationPreferences().messageHash().get());
    }

    // Verifies that the own hash is sent even when the global slot is already empty: this is the second
    // in-app of a pair, whose show went out with no hash at all before the fix because the first show had
    // consumed the slot.
    @Test
    public void inAppViewEvent_eventWithOwnHashAndEmptySlot_sendsOwnHash() throws JSONException {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RepositoryModule.getNotificationPreferences().messageHash().set(null);

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false), "hash-own"));
        ShadowLooper.idleMainLooper();

        JSONObject params = capturedShowParams();
        Assert.assertEquals("hash-own", params.getString("messageHash"));
    }

    // Verifies that a sender which captured no hash sends no hash at all instead of falling back to the
    // slot: that fallback is exactly the hash-stealing bug being fixed.
    @Test
    public void inAppViewEvent_eventWithOwnNullHash_sendsNoHashAndKeepsSlot() throws JSONException {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RepositoryModule.getNotificationPreferences().messageHash().set("hash-neighbour");

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false), null));
        ShadowLooper.idleMainLooper();

        JSONObject params = capturedShowParams();
        Assert.assertFalse(params.has("messageHash"));
        Assert.assertEquals(
                "hash-neighbour",
                RepositoryModule.getNotificationPreferences().messageHash().get());
    }

    // Verifies the HTML rich media path is unchanged: an event without a hash of its own still reads the
    // global slot and still clears it.
    @Test
    public void inAppViewEvent_legacyEvent_readsSlotAndClearsIt() throws JSONException {
        EventBus.clearSubscribersMap();
        // the InAppViewEvent subscription made by this constructor is the subject under test
        new InAppRepository(inAppStorageMock, inAppDownloaderMock, resourceMapperMock, inAppFolderProviderMock);
        RepositoryModule.getNotificationPreferences().messageHash().set("hash-slot");

        EventBus.sendEvent(new InAppViewEvent(new Resource("code1", false)));
        ShadowLooper.idleMainLooper();

        JSONObject params = capturedShowParams();
        Assert.assertEquals("hash-slot", params.getString("messageHash"));
        Assert.assertNull(
                RepositoryModule.getNotificationPreferences().messageHash().get());
    }

    // Verifies that setEmail(String) reports the seam error to the caller when the SDK is not initialized
    // instead of dropping the callback, as the removed request manager guard did.
    @Test
    public void setEmailSingle_sdkNotInitialized_callbackReceivesPushwooshException() {
        NetworkModule.setRequestManager(null);
        Callback<Boolean, PushwooshException> callback = CallbackWrapper.spy();

        inAppRepository.setEmail("a@x.com", callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, PushwooshException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback).process(resultCaptor.capture());
        Result<Boolean, PushwooshException> value = resultCaptor.getValue();
        Assert.assertFalse(value.isSuccess());
        Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
        verify(requestManagerMock, never()).sendRequest(any(), any(Callback.class));
    }

    // Verifies that setEmail(List) fails every email and never reports overall success when the SDK is not
    // initialized: the success counter must not be reached by dropped requests.
    @Test
    public void setEmailList_sdkNotInitialized_callbackReceivesFailurePerEmailAndNoSuccess() {
        NetworkModule.setRequestManager(null);
        Callback<Boolean, SetEmailException> callback = CallbackWrapper.spy();

        inAppRepository.setEmail(Arrays.asList("a@x.com", "b@x.com"), callback);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Result<Boolean, SetEmailException>> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(callback, Mockito.times(2)).process(resultCaptor.capture());
        for (Result<Boolean, SetEmailException> value : resultCaptor.getAllValues()) {
            Assert.assertFalse(value.isSuccess());
            Assert.assertEquals("SDK is not initialized", value.getException().getMessage());
        }
    }

    // Verifies that loadInApps degrades to "nothing to load" when the SDK is not initialized: the seam's
    // failed getInApps result must be handled as an empty list, without storage writes or downloads.
    @Test
    public void loadInApps_sdkNotInitialized_returnsNullDataWithoutTouchingStorage() {
        NetworkModule.setRequestManager(null);

        Result<Void, NetworkException> result = inAppRepository.loadInApps();

        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getData());
        verify(inAppStorageMock, never()).saveOrUpdateResources(anyList());
        verify(inAppDownloaderMock, never()).downloadAndDeploy(anyList());
    }

    /** Captures submitted tasks instead of running them, to prove work left the caller's thread. */
    private static ExecutorService capturingExecutor(List<Runnable> captured) {
        return new AbstractExecutorService() {
            @Override
            public void execute(Runnable command) {
                captured.add(command);
            }

            @Override
            public void shutdown() {}

            @Override
            public List<Runnable> shutdownNow() {
                return Collections.emptyList();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }
        };
    }

    // Async contract of the receive-time prefetch: nothing happens on the caller's thread;
    // the single captured task does ZIP download, then the DB row, then the tags request.
    @Test
    public void prefetchRichMediaAndTags_capturedTask_downloadThenRowThenTags() throws Exception {
        List<Runnable> captured = new ArrayList<>();
        InAppRepository repo = new InAppRepository(
                inAppStorageMock,
                inAppDownloaderMock,
                resourceMapperMock,
                inAppFolderProviderMock,
                capturingExecutor(captured));
        WhiteboxHelper.setInternalState(repo, "inAppDeployedChecker", inAppDeployedCheckerMock);

        PushwooshRepository pushwooshRepositorySpy = platformTestManager.getPushwooshRepositoryMock();
        Mockito.doNothing().when(pushwooshRepositorySpy).prefetchTags();
        when(inAppDeployedCheckerMock.check(any(Resource.class))).thenReturn(false);
        when(inAppDownloaderMock.downloadAndDeploy(anyList()))
                .thenAnswer(inv -> DownloadResult.success(inv.getArgument(0)));

        repo.prefetchRichMediaAndTags(RICH_MEDIA);

        verify(inAppDownloaderMock, Mockito.never()).downloadAndDeploy(anyList());
        verify(inAppStorageMock, Mockito.never()).saveOrUpdateResources(anyList());
        verify(pushwooshRepositorySpy, Mockito.never()).prefetchTags();
        Assert.assertEquals(1, captured.size());

        captured.get(0).run();

        InOrder inOrder = Mockito.inOrder(inAppDownloaderMock, inAppStorageMock, pushwooshRepositorySpy);
        inOrder.verify(inAppDownloaderMock).downloadAndDeploy(anyList());
        inOrder.verify(inAppStorageMock).saveOrUpdateResources(anyList());
        inOrder.verify(pushwooshRepositorySpy).prefetchTags();
    }
}
