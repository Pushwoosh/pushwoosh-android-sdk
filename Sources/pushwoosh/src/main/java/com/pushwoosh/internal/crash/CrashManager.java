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

package com.pushwoosh.internal.crash;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import android.text.TextUtils;

import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import static com.pushwoosh.internal.crash.CrashConfig.API_TOKEN;
import static com.pushwoosh.internal.crash.CrashConfig.API_TOKEN_HEADER;
import static com.pushwoosh.internal.crash.CrashConfig.BASE_CRASH_REPORT_URL;

class CrashManager {
	private final WeakReference<Context> weakContext;

	/**
	 * Shared preferences key for always send dialog button.
	 */
	static final String PREFERENCES_NAME = "SdkCrashAnalytics";
	static final String CONFIRMED_FILENAMES_KEY = "ConfirmedFilenames";
	static final String CRASH_ANALYTICS_VERSION_KEY = "CrashAnalyticsVersion";

	static final int STACK_TRACES_FOUND_NONE = 0;
	static final int STACK_TRACES_FOUND_NEW = 1;
	static final int STACK_TRACES_FOUND_CONFIRMED = 2;

	private static final int CRASH_ANALYTICS_VERSION = 1;

	public CrashManager(Context context) {
		weakContext = new WeakReference<>(context);
	}

	/**
	 * Registers the exception handler. If context is not an instance of Activity
	 * (or a subclass of it), crashes will be sent automatically.
	 */
	void register() {
		registerHandler();

		PWLog.debug("Executing CrashManager");
		new ExecuteCrashManagerTask(this)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Executes the crash manager. If context is not an instance of Activity
	 * (or a subclass of it), crashes will be sent automatically.
	 */
	void execute() {
		PWLog.debug("Looking for stacktraces to send.");
		int foundOrSend = hasStackTraces();
		if (foundOrSend == STACK_TRACES_FOUND_NEW
				|| foundOrSend == STACK_TRACES_FOUND_CONFIRMED) {
			try {
				PWLog.debug("Found stacktraces to send.");
				migrateCrashAnalytics();
				sendCrashes();
			} catch (Exception e) {
				PWLog.debug("Exception occurred while executing CrashManager:\n" + e.getMessage());
			}
		} else {
			PWLog.debug("No stacktraces were found.");
		}
	}

	/**
	 * Returns the complete URL for the report API.
	 */
	String getCompleteReportUrl() {
		return BASE_CRASH_REPORT_URL + "api/1/item/";
	}

	/**
	 * Checks if there are any saved stack traces in the files dir.
	 *
	 * @return STACK_TRACES_FOUND_NONE if there are no stack traces,
	 * STACK_TRACES_FOUND_NEW if there are any new stack traces,
	 * STACK_TRACES_FOUND_CONFIRMED if there only are confirmed stack traces.
	 */
	int hasStackTraces() {
		String[] filenames = searchForStackTraces();
		List<String> confirmedFilenames = null;
		int result = STACK_TRACES_FOUND_NONE;
		if ((filenames != null) && (filenames.length > 0)) {
			try {
				Context context = getContext();
				if (context != null) {
					SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
					confirmedFilenames = Arrays.asList(preferences.getString(CONFIRMED_FILENAMES_KEY, "").split("\\|"));
				}
			} catch (Exception ignored) {
				//ignore
			}

			if (confirmedFilenames != null) {
				result = STACK_TRACES_FOUND_CONFIRMED;

				for (String filename : filenames) {
					if (!confirmedFilenames.contains(filename)) {
						result = STACK_TRACES_FOUND_NEW;
						break;
					}
				}
			} else {
				result = STACK_TRACES_FOUND_NEW;
			}
		}

		return result;
	}

	boolean isCrashAnalyticsUpdated() {
		Context context = getContext();
		if (context != null) {
			SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
			int previousVersion = preferences.getInt(CRASH_ANALYTICS_VERSION_KEY, 0);
			return previousVersion < CRASH_ANALYTICS_VERSION;
		}
		return false;
	}

	boolean migrateCrashReports() {
		Context context = getContext();
		if (context != null) {
			File dir = FileProvider.getCrashDir(context);
			if (dir != null) {
				PWLog.debug("Looking for crash reports in: " + dir.getAbsolutePath());
				if (!dir.exists()) {
					PWLog.debug("No crash reports to migrate.");
					return true;
				}

				// Filter for ".pushwoosh.stacktrace" files
				FilenameFilter filter = (dir1, name) -> name.endsWith(FileProvider.getPushwooshStacktraceSuffix());
				File[] list = dir.listFiles(filter);
				if (list == null || list.length == 0) {
					// No .pushwoosh.stacktrace files were found
					PWLog.debug("No crash reports to migrate.");
					return true;
				}
				for (File file : list) {
					if (!file.isDirectory()) {
						// Delete file
						file.delete();
					}
				}
			}  else {
				return false;
			}
			PWLog.debug("Migrated successfully.");
			return true;
		}
		PWLog.debug("Migrating from previous version did not succeed. Context is null.");
		return false;
	}

	void submitStackTrace(String filename, String reportUrl) {
		PWLog.debug("Sending crash report to the server.");
		HttpURLConnection urlConnection = null;
		try {
			String stacktrace = contentsOfFile(filename);
			if (stacktrace.length() > 0) {
				urlConnection = new HttpURLConnectionBuilder(reportUrl)
						.setRequestMethod("POST")
						.setHeader(API_TOKEN_HEADER, API_TOKEN)
						.setRequestBody(stacktrace)
						.build();

				PWLog.debug("Crash report was submitted: " + urlConnection.getResponseCode()
						+ " " +	urlConnection.getResponseMessage());
			}
		} catch (Exception e) {
			PWLog.debug("Exception occurred while submitting crash report to the server " + e.getMessage());
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			deleteStackTrace(filename);
		}
	}

	/**
	 * Registers the exception handler.
	 */
	void registerHandler() {
		// Get current handler
		Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (currentHandler != null) {
			PWLog.debug("Current handler class = " + currentHandler.getClass().getName());
		}

		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(currentHandler, weakContext));
	}

	/**
	 * Deletes the give filename and all corresponding files (same name,
	 * different extension).
	 */
	void deleteStackTrace(String filename) {
		Context context = getContext();
		if (context != null) {
			context.deleteFile(filename);

			String user = filename.replace(FileProvider.getPushwooshStacktraceSuffix(), ".user");
			context.deleteFile(user);

			String contact = filename.replace(FileProvider.getPushwooshStacktraceSuffix(), ".contact");
			context.deleteFile(contact);

			String description = filename.replace(FileProvider.getPushwooshStacktraceSuffix(), ".description");
			context.deleteFile(description);
		}
	}

	/**
	 * Returns the content of a file as a string.
	 */
	String contentsOfFile(String filename) {
		Context context = getContext();
		if (context != null) {
			File file = context.getFileStreamPath(filename);
			if (file == null || !file.exists()) {
				return "";
			}
			StringBuilder contents = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(filename)))) {
				String line;
				while ((line = reader.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			} catch (IOException e) {
				PWLog.error("Failed to read content of " + filename, e);
			}
			//ignored
			return contents.toString();
		}
		return "";
	}

	/**
	 * Saves the list of the stack traces' file names in shared preferences.
	 */
	void saveConfirmedStackTraces(String[] stackTraces) {
		Context context = getContext();
		if (context != null) {
			try {
				SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(CONFIRMED_FILENAMES_KEY, TextUtils.join(",", stackTraces));
				editor.apply();
			} catch (Exception ignored) {
				//ignored
			}
		}
	}

	/**
	 * Searches {@link FileProvider#getPushwooshStacktraceSuffix()} files and returns them as array.
	 */
	String[] searchForStackTraces() {
		Context context = getContext();
		if (context != null) {
			File dir = FileProvider.getCrashDir(context);
			if (dir != null) {
				PWLog.debug("Looking for exceptions in: " + dir.getAbsolutePath());
				if (!dir.exists() && !dir.mkdir()) {
					return new String[0];
				}

				// Filter for ".pushwoosh.stacktrace" files
				FilenameFilter filter = (dir1, name) -> name.endsWith(FileProvider.getPushwooshStacktraceSuffix());
				return dir.list(filter);
			} else {
				PWLog.debug("Can't search for exception as file path is null.");
			}
		}
		return null;
	}

	/**
	 * Checks if CrashManager version is updated and does migration of crash reports
	 * if CrashManager version was updated.
	 */
	void migrateCrashAnalytics() {
		PWLog.debug("Checking CrashManager version.");
		Context context = getContext();
		if (context != null) {
			try {
				if (isCrashAnalyticsUpdated()) {
					PWLog.debug("Found new CrashManager version. Migrating crash reports from previous version.");
					if (migrateCrashReports()) {
						SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = preferences.edit();
						editor.putInt(CRASH_ANALYTICS_VERSION_KEY, CRASH_ANALYTICS_VERSION);
						editor.apply();
					}
				} else {
					PWLog.debug("CrashManager version did not change.");
				}
			} catch (Exception e) {
				PWLog.debug("Exception occurred while doing MigrateCrashAnalyticsTask:\n" + e.getMessage());
			}
		}
	}

	/**
	 * Sends crashes to report server.
	 */
	private void sendCrashes() {
		try {
			PWLog.debug("Sending crashreports to report server.");
			final boolean isConnectedToNetwork = GeneralUtils.isNetworkAvailable();
			final String[] list = searchForStackTraces();
			if (list != null && list.length > 0) {
				saveConfirmedStackTraces(list);
				if (isConnectedToNetwork) {
					for (String file : list) {
						submitStackTrace(file, getCompleteReportUrl());
					}
				} else {
					PWLog.debug("No internet connection available. Pushwoosh will try " +
							"to send crashreports on next app launch.");
				}
			} else {
				PWLog.debug("No new crashreports were found.");
			}
		} catch (Exception e) {
			PWLog.debug("Exception occurred while sending crash reports:\n" + e.getMessage());
		}
	}

	/**
	 * Retrieves the context from the weak reference.
	 *
	 * @return The context object for this instance.
	 */
	private Context getContext() {
		return weakContext != null ? weakContext.get() : null;
	}

	private static class ExecuteCrashManagerTask extends AsyncTask<Void, Object, Integer> {
		private final CrashManager crashManager;

		public ExecuteCrashManagerTask(CrashManager crashManager) {
			this.crashManager = crashManager;
		}

		@Override
		protected Integer doInBackground(Void... voids) {
			if (crashManager != null) {
				crashManager.execute();
			}
			return null;
		}
	}
}
