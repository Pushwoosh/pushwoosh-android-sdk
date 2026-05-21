package com.pushwoosh.inapp.network.downloader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import androidx.core.util.Pair;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class FileHashCheckerTest {

    @Mock
    private Resource resource;

    private AutoCloseable mocks;
    private FileHashChecker checker;
    private File file;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        checker = new FileHashChecker();
        file = new File("/tmp/inapp-resource");
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that check returns true when file md5 matches Resource#getHash.
    @Test
    public void check_md5MatchesResourceHash_returnsTrue() {
        when(resource.getHash()).thenReturn("abc123");
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getMd5Hash(file)).thenReturn("abc123");

            assertTrue(checker.check(Pair.create(file, resource)));
        }
    }

    // Verifies that resource with null hash is treated as valid without computing md5.
    @Test
    public void check_resourceHashIsNull_returnsTrueAndSkipsMd5() {
        when(resource.getHash()).thenReturn(null);
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            assertTrue(checker.check(Pair.create(file, resource)));
            fileUtils.verifyNoInteractions();
        }
    }

    // Verifies that resource with empty hash is treated as valid without computing md5.
    @Test
    public void check_resourceHashIsEmpty_returnsTrueAndSkipsMd5() {
        when(resource.getHash()).thenReturn("");
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            assertTrue(checker.check(Pair.create(file, resource)));
            fileUtils.verifyNoInteractions();
        }
    }

    // Verifies that check returns false when file md5 differs from Resource#getHash.
    @Test
    public void check_md5DiffersFromResourceHash_returnsFalse() {
        when(resource.getHash()).thenReturn("expected");
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getMd5Hash(file)).thenReturn("actual");

            assertFalse(checker.check(Pair.create(file, resource)));
        }
    }
}
