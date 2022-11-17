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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest(FileUtils.class)
public class InAppSecureTest {
	@Rule
	public PowerMockRule rule = new PowerMockRule();

	private FileHashChecker fileHashChecker = new FileHashChecker();
	private File file;

	@Before
	public void setUp() {
		file = new File("");
		mockStatic(FileUtils.class);
		when(FileUtils.getMd5Hash(file)).thenReturn("test_hash");
	}

	@Test
	public void testValidHash() {
		Resource resource = new Resource("", "", "test_hash", 0, InAppLayout.DIALOG, null, false, 0, null, null);
		assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
	}

	@Test
	public void testInvalidHash() {
		Resource resource = new Resource("", "", "invalid_hash", 0, InAppLayout.DIALOG, null, false, 0, null, null);
		assertFalse(fileHashChecker.check(new Pair<>(file, resource)));
	}

	@Test
	public void testEmptyHash() {
		Resource resource = new Resource("", "", "", 0, InAppLayout.DIALOG, null, false, 0, null, null);
		assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
	}

	@Test
	public void testNullHash() {
		Resource resource = new Resource("", "", null, 0, InAppLayout.DIALOG, null, false, 0, null, null);
		assertTrue(fileHashChecker.check(new Pair<>(file, resource)));
	}
}
