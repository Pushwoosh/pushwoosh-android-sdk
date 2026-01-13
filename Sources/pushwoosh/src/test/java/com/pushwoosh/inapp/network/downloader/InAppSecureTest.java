/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.inapp.network.downloader;

import java.io.File;

import androidx.core.util.Pair;

import com.pushwoosh.BuildConfig;
import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.FileUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class InAppSecureTest {
	private FileHashChecker fileHashChecker = new FileHashChecker();

	@Before
	public void setUp() {
	}

	@Test
	public void testValidHash() {
		File file = new File("");
		try(MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
			fileUtilsMockedStatic.when(() -> FileUtils.getMd5Hash(file)).thenReturn("test_hash");
			Resource resource = new Resource("", "", "test_hash", 0, InAppLayout.DIALOG, null, false, 0);
			assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
		}
	}

	@Test
	public void testInvalidHash() {
		File file = new File("");
		try(MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
			fileUtilsMockedStatic.when(() -> FileUtils.getMd5Hash(file)).thenReturn("test_hash");
			Resource resource = new Resource("", "", "invalid_hash", 0, InAppLayout.DIALOG, null, false, 0);
			assertFalse(fileHashChecker.check(new Pair<>(file, resource)));
		}
	}

	@Test
	public void testEmptyHash() {
		File file = new File("");
		try(MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
			fileUtilsMockedStatic.when(() -> FileUtils.getMd5Hash(file)).thenReturn("test_hash");
			Resource resource = new Resource("", "", "", 0, InAppLayout.DIALOG, null, false, 0);
			assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
		}
	}

	@Test
	public void testNullHash() {
		File file = new File("");
		try(MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
			fileUtilsMockedStatic.when(() -> FileUtils.getMd5Hash(file)).thenReturn("test_hash");
			Resource resource = new Resource("", "", null, 0, InAppLayout.DIALOG, null, false, 0);
			assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
		}
	}
}
