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
import android.os.Build;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.UUID;

/**
 * <h3>Description</h3>
 * Helper class to catch exceptions. Saves the stack trace
 * as a file and executes callback methods to ask the app for
 * additional information and meta data (see CrashManagerListener).
 **/
class ExceptionHandler implements UncaughtExceptionHandler {
	private final UncaughtExceptionHandler defaultExceptionHandler;
	private final WeakReference<Context> weakContext;

	ExceptionHandler(UncaughtExceptionHandler defaultExceptionHandler, WeakReference<Context> weakContext) {
		this.defaultExceptionHandler = defaultExceptionHandler;
		this.weakContext = weakContext;
	}

	/**
	 * Save a caught exception to disk.
	 *
	 * @param exception Exception to save.
	 */
	public void saveException(Throwable exception) {
		Context context = weakContext != null ? weakContext.get() : null;
		if (context == null) {
			PWLog.error("Failed to save exception: context in CrashManager is null");
			return;
		}

		String filename = UUID.randomUUID().toString();
		File file = new File(FileProvider.getCrashDir(context), filename + FileProvider.getPushwooshStacktraceSuffix());
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			JSONObject crashReport = CrashReport.createCrashReport(
					exception,
					CrashConfig.appCode(),
					CrashConfig.hwid(),
					CrashConfig.framework(),
					CrashConfig.isCollectingDeviceModelAllowed(),
					CrashConfig.isCollectingDeviceOsVersionAllowed()
			);
			String crashReportString = crashReport.toString();
			writer.write(crashReportString);
			writer.flush();
		} catch (IOException ioException) {
			PWLog.error("Failed to save exception:\n" + ioException.getMessage());
		} catch (JSONException jsonException) {
			PWLog.error("Failed to save exception:\n" + jsonException.getMessage());
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e1) {
				PWLog.error("Error saving crash report!", e1);
			}
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		Context context = weakContext != null ? weakContext.get() : null;
		if (context != null && FileProvider.getCrashDir(context) != null) {
			StringBuilder checkPackages = new StringBuilder();
			for (StackTraceElement traceElement : exception.getStackTrace()) {
				checkPackages.append(traceElement.getClassName());
			}

			String stackTrace = checkPackages.toString();
			if (isPushwooshError(context, exception)) {
				for (String aPackage : CrashConfig.CATCH_PACKAGES) {
					if (stackTrace.contains(aPackage)) {
						saveException(exception);
						break;
					}
				}
			}
		}
		if (defaultExceptionHandler != null) {
			defaultExceptionHandler.uncaughtException(thread, exception);
		}
	}

	boolean isPushwooshError(Context context, Throwable exception) {
		String packageName = context.getPackageName();
		for (StackTraceElement element : exception.getStackTrace()) {
			String className = element.getClassName();
			for (String catchPackage : CrashConfig.CATCH_PACKAGES) {
				if (className.contains(catchPackage)) {
					return true;
				}
			}
			if (className.contains(packageName)) {
				return false;
			}
		}

		return false;
	}
}