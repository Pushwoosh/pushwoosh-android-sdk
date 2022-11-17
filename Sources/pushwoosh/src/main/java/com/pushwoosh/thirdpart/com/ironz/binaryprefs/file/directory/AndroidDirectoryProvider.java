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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.directory;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.FileOperationException;

import java.io.File;

/**
 * Provides default android cache directory or external (if possible) cache directory.
 */
public final class AndroidDirectoryProvider implements DirectoryProvider {

	private static final String CANNOT_CREATE_DIR_MESSAGE = "Can't create preferences directory in %s";

	static final String PREFERENCES_ROOT_DIRECTORY_NAME = "preferences";

	static final String STORE_DIRECTORY_NAME = "values";
	static final String BACKUP_DIRECTORY_NAME = "backup";
	static final String LOCK_DIRECTORY_NAME = "lock";

	private final File storeDirectory;
	private final File backupDirectory;
	private final File lockDirectory;

	/**
	 * Creates instance for default or external (if enabled) persistent cache directory.
	 *
	 * @param prefName preferences name
	 * @param baseDir  all data will be saved inside this directory.
	 */
	public AndroidDirectoryProvider(String prefName, File baseDir) {
		this.storeDirectory = createAndValidate(baseDir, prefName, STORE_DIRECTORY_NAME);
		this.backupDirectory = createAndValidate(baseDir, prefName, BACKUP_DIRECTORY_NAME);
		this.lockDirectory = createAndValidate(baseDir, prefName, LOCK_DIRECTORY_NAME);
	}

	private File createAndValidate(File baseDir, String prefName, String subDirectory) {
		File targetDirectory = create(baseDir, prefName, subDirectory);
		if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
			String absolutePath = targetDirectory.getAbsolutePath();
			throw new FileOperationException(String.format(CANNOT_CREATE_DIR_MESSAGE, absolutePath));
		}
		return targetDirectory;
	}

	private File create(File baseDir, String prefName, String subDirectory) {
		File prefsDir = new File(baseDir, PREFERENCES_ROOT_DIRECTORY_NAME);
		File prefNameDir = new File(prefsDir, prefName);
		return new File(prefNameDir, subDirectory);
	}

	@Override
	public File getStoreDirectory() {
		return storeDirectory;
	}

	@Override
	public File getBackupDirectory() {
		return backupDirectory;
	}

	@Override
	public File getLockDirectory() {
		return lockDirectory;
	}
}