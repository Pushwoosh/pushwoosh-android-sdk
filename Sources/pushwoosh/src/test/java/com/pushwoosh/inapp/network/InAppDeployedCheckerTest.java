package com.pushwoosh.inapp.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.storage.InAppStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Collections;

public class InAppDeployedCheckerTest {

    private static final String CODE = "inapp-code";

    @Mock
    private InAppStorage inAppStorage;

    @Mock
    private InAppFolderProvider inAppFolderProvider;

    @Mock
    private File htmlFile;

    @Mock
    private File nativeConfigFile;

    private AutoCloseable mocks;

    private InAppDeployedChecker checker;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        checker = new InAppDeployedChecker(inAppStorage, inAppFolderProvider);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private Resource resource(String code, long updated) {
        return new Resource(code, "http://example.com", "hash", updated, null, Collections.emptyMap(), false, 0);
    }

    // Verifies that check returns true when storage has matching resource and html file exists.
    @Test
    public void check_storageMatchesAndHtmlExists_returnsTrue() {
        Resource input = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(true);

        assertTrue(checker.check(input));
    }

    // Parameterized negative branches: storage miss, stale updated timestamp, null html file, missing html file.
    @Test
    public void check_negativeBranches_returnsFalse() {
        // (1) storage returns null
        Resource input1 = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(null);
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(true);
        assertFalse("storageNull", checker.check(input1));

        // (2) updated timestamp mismatch
        Resource input2 = resource(CODE, 200L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(true);
        assertFalse("updatedMismatch", checker.check(input2));

        // (3) folder provider returns null html file
        Resource input3 = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(null);
        assertFalse("htmlNull", checker.check(input3));

        // (4) html file does not exist on disk
        Resource input4 = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(false);
        assertFalse("htmlMissing", checker.check(input4));
    }

    // Native-only ZIP (no index.html) counts as deployed — the payload discriminator is
    // file content, not resource type.
    @Test
    public void check_nativeConfigOnlyWithoutHtml_returnsTrue() {
        Resource input = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(false);
        when(inAppFolderProvider.getNativeConfigFile(CODE)).thenReturn(nativeConfigFile);
        when(nativeConfigFile.exists()).thenReturn(true);

        assertTrue(checker.check(input));
    }

    // Neither payload file present -> not deployed.
    @Test
    public void check_noPayloadFiles_returnsFalse() {
        Resource input = resource(CODE, 100L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(false);
        when(inAppFolderProvider.getNativeConfigFile(CODE)).thenReturn(nativeConfigFile);
        when(nativeConfigFile.exists()).thenReturn(false);

        assertFalse(checker.check(input));
    }

    // Stale DB record loses even when native-config.json exists on disk.
    @Test
    public void check_nativeConfigButUpdatedMismatch_returnsFalse() {
        Resource input = resource(CODE, 200L);
        when(inAppStorage.getResource(CODE)).thenReturn(resource(CODE, 100L));
        when(inAppFolderProvider.getInAppHtmlFile(CODE)).thenReturn(htmlFile);
        when(htmlFile.exists()).thenReturn(false);
        when(inAppFolderProvider.getNativeConfigFile(CODE)).thenReturn(nativeConfigFile);
        when(nativeConfigFile.exists()).thenReturn(true);

        assertFalse(checker.check(input));
    }
}
