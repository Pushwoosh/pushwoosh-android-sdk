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

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;

import com.pushwoosh.inapp.event.InAppEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class InAppDownloader {
	private static final String TAG = "[InApp]InAppDownloader";

	private final Object mutex = new Object();
	private final InAppFolderProvider inAppFolderProvider;
	private final FileHashChecker fileHashChecker = new FileHashChecker();

	private final Set<Resource> downloadingResources = new ConcurrentSkipListSet<>();

	public InAppDownloader(InAppFolderProvider inAppFolderProvider) {
		this.inAppFolderProvider = inAppFolderProvider;
	}

	@WorkerThread
	public DownloadResult downloadAndDeploy(List<Resource> resources) {
		if (resources == null || resources.isEmpty()) {
			return DownloadResult.empty();
		}

		final List<Resource> localResource = new ArrayList<>(resources);
		Collections.sort(localResource);
		this.downloadingResources.addAll(localResource);

		synchronized (mutex) {

			final ArrayList<Resource> deployed = new ArrayList<>(localResource.size());
			final ArrayList<Resource> failed = new ArrayList<>();

			for (Resource inapp : localResource) {
				if (downloadAndDeployResource(inapp)) {
					deployed.add(inapp);
					PWLog.debug(TAG, inapp.getCode() + " deployed");
					EventBus.sendEvent(new InAppEvent(InAppEvent.EventType.DEPLOYED, inapp));
				} else {
					failed.add(inapp);
					EventBus.sendEvent(new InAppEvent(InAppEvent.EventType.DEPLOY_FAILED, inapp));
				}

				downloadingResources.remove(inapp);
			}

			return new DownloadResult(deployed, failed);
		}
	}

	private boolean downloadAndDeployResource(Resource inapp) {
		deleteInAppFolder(inapp.getCode());

		File zip = downloadZipFile(inapp);
		if (zip == null) {
			PWLog.error(TAG, "Failed to download " + inapp.getUrl());
			return false;
		}

		if (!checkZipFile(inapp, zip)) {
			PWLog.error(TAG, "File is not valid " + inapp.getUrl());
			return false;
		}

		File deployZip = unzip(inapp, zip);
		if (deployZip == null) {
			PWLog.error(TAG, "Failed to deploy " + inapp.getUrl());
			return false;
		}

		return true;
	}

	private void deleteInAppFolder(String code) {
		File inAppFolder = inAppFolderProvider.getInAppFolder(code);
		if (inAppFolder != null && inAppFolder.exists()) {
			//noinspection ResultOfMethodCallIgnored
			FileUtils.deleteDirectory(inAppFolder);
		}
	}

	@Nullable
	private File downloadZipFile(Resource resource) {
		PWLog.noise(TAG, "Start download: " + resource.getCode());
		EventBus.sendEvent(new InAppEvent(InAppEvent.EventType.DOWNLOADING_ZIP, resource));

		File cacheDir = inAppFolderProvider.getCacheDir();
		if (cacheDir == null) {
			return null;
		}

		final File destinationFile = new File(cacheDir, resource.getCode() + ".zip");
		final File zip = FileUtils.downloadFile(resource.getUrl(), destinationFile);
		if (zip == null) {
			return null;
		}

		EventBus.sendEvent(new InAppEvent(InAppEvent.EventType.DOWNLOADED_ZIP, resource));

		return zip;
	}

	private boolean checkZipFile(Resource inapp, File zip) {
		if (!fileHashChecker.check(new Pair<>(zip, inapp))) {
			//noinspection ResultOfMethodCallIgnored
			zip.delete();
			return false;
		}
		return true;
	}

	@Nullable
	private File unzip(Resource inapp, File zip) {
		zip.deleteOnExit();

		PWLog.noise(TAG, "Start deploy:" + inapp.getCode());

		File deployDir = inAppFolderProvider.getInAppFolder(inapp.getCode());
		return FileUtils.unzip(zip, deployDir);
	}

	public boolean isDownloading(Resource resource) {
		return downloadingResources.contains(resource);
	}

	public void removeResourceFiles(String code) {
		synchronized (mutex) {
			deleteInAppFolder(code);
		}
	}
}
