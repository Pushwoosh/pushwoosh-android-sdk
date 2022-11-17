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

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.checker.ObjectChecker;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;

/**
 * This class check that md5sum of inApps's file equal to backend md5sum
 * {@link com.pushwoosh.inapp.network.model.Resource#getHash}
 *
 * @see <a href="https://jira.corp.pushwoosh.com/browse/PUSH-10305">jira task</a>
 */
class FileHashChecker implements ObjectChecker<Pair<File, Resource>> {

	private static final String TAG = "[InApp]FileHashChecker";

	@Override
	public boolean check(Pair<File, Resource> check) {
		File file = check.first;
		Resource resource = check.second;

		if(file == null || resource == null){
			PWLog.noise(TAG, "incorrect state of arguments");
			return false;
		}

		String resourceHash = resource.getHash();

		// Resource with empty hash is valid
		if (resourceHash == null || resourceHash.isEmpty()) {
			PWLog.noise(TAG, "Hash is empty for " + resource.getUrl());
			return true;
		}

		String fileHash = FileUtils.getMd5Hash(file);
		PWLog.noise(TAG, "Resource hash " + resourceHash + ", file hash " + fileHash);
		return resourceHash.equals(fileHash);
	}
}
