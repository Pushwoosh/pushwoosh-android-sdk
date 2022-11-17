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

package com.pushwoosh.inapp.storage;

import android.content.Context;
import androidx.annotation.Nullable;

import java.io.File;

public class ContextInAppFolderProvider implements InAppFolderProvider {

	@Nullable
	private final Context context;

	public ContextInAppFolderProvider(@Nullable Context context) {
		this.context = context;
	}

	@Override
	public File getInAppFolder(String code) {
		if (context == null) {
			return null;
		}

		File inAppsDir = context.getDir("htmls", Context.MODE_PRIVATE);
		return new File(inAppsDir, code);
	}

	@Override
	public File getConfigFile(String code) {
		File inAppFolder = getInAppFolder(code);
		if (inAppFolder == null) {
			return null;
		}

		return new File(inAppFolder, "pushwoosh.json");
	}

	@Override
	public File getInAppHtmlFile(String code) {
		File inAppFolder = getInAppFolder(code);
		if (inAppFolder == null) {
			return null;
		}

		return new File(inAppFolder, "index.html");
	}

	@Override
	public File getCacheDir() {
		if (context == null) {
			return null;
		}

		return context.getCacheDir();
	}

	@Override
	public boolean isInAppDownloaded(String code) {
		File inappFolder = getInAppFolder(code);

		return inappFolder != null && inappFolder.exists();
	}
}
