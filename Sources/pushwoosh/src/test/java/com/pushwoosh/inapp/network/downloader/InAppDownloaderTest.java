package com.pushwoosh.inapp.network.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.event.InAppEvent;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = "AndroidManifest.xml")
public class InAppDownloaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private InAppFolderProvider inAppFolderProvider;

    private AutoCloseable mocks;
    private MockedStatic<FileUtils> fileUtilsMock;
    private MockedStatic<EventBus> eventBusMock;

    private InAppDownloader downloader;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        fileUtilsMock = mockStatic(FileUtils.class);
        eventBusMock = mockStatic(EventBus.class);
        downloader = new InAppDownloader(inAppFolderProvider);
    }

    @After
    public void tearDown() throws Exception {
        eventBusMock.close();
        fileUtilsMock.close();
        mocks.close();
    }

    private Resource newResource(String code, String url, String hash) {
        return new Resource(code, url, hash, 0L, InAppLayout.FULLSCREEN, null, false, 0);
    }

    private Resource newResource(String code, String url, String hash, boolean required, int priority) {
        return new Resource(code, url, hash, 0L, InAppLayout.FULLSCREEN, null, required, priority);
    }

    private List<InAppEvent.EventType> capturedEventTypesFor(String code) {
        ArgumentCaptor<InAppEvent> captor = ArgumentCaptor.forClass(InAppEvent.class);
        eventBusMock.verify(() -> EventBus.sendEvent(captor.capture()), org.mockito.Mockito.atLeast(0));
        List<InAppEvent.EventType> types = new ArrayList<>();
        for (InAppEvent e : captor.getAllValues()) {
            if (code.equals(e.getCode())) {
                types.add(e.getType());
            }
        }
        return types;
    }

    // Verifies that successful download, hash validation and unzip yield success and DEPLOYED event.
    @Test
    public void downloadAndDeploy_happyPath_resourceDeployed() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDir = tempFolder.newFolder("deploy-r1");
        File zipFile = tempFolder.newFile("r1.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("r1")).thenReturn(deployDir);
        fileUtilsMock
                .when(() -> FileUtils.downloadFile(eq("http://example/r1.zip"), any(File.class)))
                .thenReturn(zipFile);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDir))).thenReturn(deployDir);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertEquals(1, result.getSuccess().size());
        assertEquals(resource, result.getSuccess().get(0));
        assertTrue(result.getFailed().isEmpty());

        List<InAppEvent.EventType> types = capturedEventTypesFor("r1");
        assertTrue(types.contains(InAppEvent.EventType.DOWNLOADING_ZIP));
        assertTrue(types.contains(InAppEvent.EventType.DOWNLOADED_ZIP));
        assertTrue(types.contains(InAppEvent.EventType.DEPLOYED));
        assertFalse(types.contains(InAppEvent.EventType.DEPLOY_FAILED));

        assertFalse(downloader.isDownloading(resource));
    }

    // Verifies that a null cache directory aborts the download and reports failure.
    @Test
    public void downloadAndDeploy_nullCacheDir_resourceFails() {
        when(inAppFolderProvider.getCacheDir()).thenReturn(null);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertTrue(result.getSuccess().isEmpty());
        assertEquals(1, result.getFailed().size());
        assertEquals(resource, result.getFailed().get(0));

        fileUtilsMock.verify(() -> FileUtils.downloadFile(any(), any()), never());

        List<InAppEvent.EventType> types = capturedEventTypesFor("r1");
        assertTrue(types.contains(InAppEvent.EventType.DOWNLOADING_ZIP));
        assertTrue(types.contains(InAppEvent.EventType.DEPLOY_FAILED));
        assertFalse(types.contains(InAppEvent.EventType.DOWNLOADED_ZIP));
        assertFalse(types.contains(InAppEvent.EventType.DEPLOYED));
    }

    // Verifies that a null download result is treated as failure and unzip is not invoked.
    @Test
    public void downloadAndDeploy_downloadReturnsNull_resourceFails() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenReturn(null);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertTrue(result.getSuccess().isEmpty());
        assertEquals(1, result.getFailed().size());

        fileUtilsMock.verify(() -> FileUtils.unzip(any(), any()), never());

        List<InAppEvent.EventType> types = capturedEventTypesFor("r1");
        assertTrue(types.contains(InAppEvent.EventType.DEPLOY_FAILED));
        assertFalse(types.contains(InAppEvent.EventType.DOWNLOADED_ZIP));
    }

    // Verifies that hash mismatch deletes the zip and reports failure without unzipping.
    @Test
    public void downloadAndDeploy_hashMismatch_zipDeletedAndResourceFails() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File zipFile = tempFolder.newFile("r1.zip");
        try (FileWriter w = new FileWriter(zipFile)) {
            w.write("payload");
        }
        assertTrue(zipFile.exists());

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenReturn(zipFile);
        fileUtilsMock.when(() -> FileUtils.getMd5Hash(zipFile)).thenReturn("actual-hash");

        Resource resource = newResource("r1", "http://example/r1.zip", "bogus-hash");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertTrue(result.getSuccess().isEmpty());
        assertEquals(1, result.getFailed().size());

        fileUtilsMock.verify(() -> FileUtils.unzip(any(), any()), never());
        assertFalse(zipFile.exists());

        List<InAppEvent.EventType> types = capturedEventTypesFor("r1");
        assertTrue(types.contains(InAppEvent.EventType.DEPLOY_FAILED));
    }

    // Verifies that empty hash skips validation and the resource is deployed.
    @Test
    public void downloadAndDeploy_emptyHash_skipsValidation() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDir = tempFolder.newFolder("deploy");
        File zipFile = tempFolder.newFile("r1.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("r1")).thenReturn(deployDir);
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenReturn(zipFile);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDir))).thenReturn(deployDir);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertEquals(1, result.getSuccess().size());
        assertTrue(result.getFailed().isEmpty());
        fileUtilsMock.verify(() -> FileUtils.getMd5Hash(any()), never());
    }

    // Verifies that a null unzip result reports failure while DOWNLOADED_ZIP still fires.
    @Test
    public void downloadAndDeploy_unzipReturnsNull_resourceFails() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDir = tempFolder.newFolder("deploy");
        File zipFile = tempFolder.newFile("r1.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("r1")).thenReturn(deployDir);
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenReturn(zipFile);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDir))).thenReturn(null);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertTrue(result.getSuccess().isEmpty());
        assertEquals(1, result.getFailed().size());

        List<InAppEvent.EventType> types = capturedEventTypesFor("r1");
        assertTrue(types.contains(InAppEvent.EventType.DOWNLOADED_ZIP));
        assertTrue(types.contains(InAppEvent.EventType.DEPLOY_FAILED));
        assertFalse(types.contains(InAppEvent.EventType.DEPLOYED));
    }

    // Verifies that a mixed batch splits resources between success and failed buckets.
    @Test
    public void downloadAndDeploy_mixedBatch_splitsBetweenSuccessAndFailed() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDirOk = tempFolder.newFolder("deploy-ok");
        File zipFile = tempFolder.newFile("ok.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("ok")).thenReturn(deployDirOk);
        fileUtilsMock
                .when(() -> FileUtils.downloadFile(eq("http://example/ok.zip"), any(File.class)))
                .thenReturn(zipFile);
        fileUtilsMock
                .when(() -> FileUtils.downloadFile(eq("http://example/bad.zip"), any(File.class)))
                .thenReturn(null);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDirOk))).thenReturn(deployDirOk);

        Resource ok = newResource("ok", "http://example/ok.zip", "");
        Resource bad = newResource("bad", "http://example/bad.zip", "");

        DownloadResult result = downloader.downloadAndDeploy(Arrays.asList(ok, bad));

        assertEquals(Collections.singletonList(ok), result.getSuccess());
        assertEquals(Collections.singletonList(bad), result.getFailed());

        List<InAppEvent.EventType> okTypes = capturedEventTypesFor("ok");
        assertTrue(okTypes.contains(InAppEvent.EventType.DEPLOYED));
        assertFalse(okTypes.contains(InAppEvent.EventType.DEPLOY_FAILED));

        List<InAppEvent.EventType> badTypes = capturedEventTypesFor("bad");
        assertTrue(badTypes.contains(InAppEvent.EventType.DEPLOY_FAILED));
        assertFalse(badTypes.contains(InAppEvent.EventType.DEPLOYED));
    }

    // Verifies that an existing resource folder is deleted before re-downloading.
    @Test
    public void downloadAndDeploy_existingFolder_isDeletedBeforeDownload() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDir = tempFolder.newFolder("deploy");
        File zipFile = tempFolder.newFile("r1.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("r1")).thenReturn(deployDir);
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenReturn(zipFile);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDir))).thenReturn(deployDir);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        downloader.downloadAndDeploy(Collections.singletonList(resource));

        fileUtilsMock.verify(() -> FileUtils.deleteDirectory(deployDir), times(1));
    }

    // Verifies that isDownloading is true during download and false after the call returns.
    @Test
    public void downloadAndDeploy_isDownloadingTransitions_trueDuringFalseAfter() throws Exception {
        File cacheDir = tempFolder.newFolder("cache");
        File deployDir = tempFolder.newFolder("deploy");
        File zipFile = tempFolder.newFile("r1.zip");

        when(inAppFolderProvider.getCacheDir()).thenReturn(cacheDir);
        when(inAppFolderProvider.getInAppFolder("r1")).thenReturn(deployDir);
        fileUtilsMock.when(() -> FileUtils.unzip(eq(zipFile), eq(deployDir))).thenReturn(deployDir);

        Resource resource = newResource("r1", "http://example/r1.zip", "");

        final boolean[] duringDownload = new boolean[1];
        fileUtilsMock.when(() -> FileUtils.downloadFile(any(), any(File.class))).thenAnswer(inv -> {
            duringDownload[0] = downloader.isDownloading(resource);
            return zipFile;
        });

        downloader.downloadAndDeploy(Collections.singletonList(resource));

        assertTrue(duringDownload[0]);
        assertFalse(downloader.isDownloading(resource));
    }
}
