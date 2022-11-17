package com.pushwoosh.internal.crash;

import android.content.Context;

import org.junit.Assert;

import java.io.File;
import java.io.FilenameFilter;

public class CrashManagerTestUtils {

    public void removeCrashReports(Context context) {
        File dir = FileProvider.getCrashDir(context);
        if (dir != null) {
            if (!dir.exists()) {
                return;
            }

            // Filter for ".pushwoosh.stacktrace" files
            FilenameFilter filter = (dir1, name) -> name.endsWith(FileProvider.getPushwooshStacktraceSuffix());
            File[] list = dir.listFiles(filter);
            if (list == null || list.length == 0) {
                return;
            }
            for (File file : list) {
                if (!file.isDirectory()) {
                    // Delete file
                    file.delete();
                }
            }
        }
    }

    public int getSavedCrashReportsNumber(Context context) throws Exception {
        File dir = FileProvider.getCrashDir(context);
        if (dir != null) {
            if (!dir.exists()) {
                Assert.fail("Crash dir doesn't exist");
            }
            FilenameFilter filter = (dir1, name) -> name.endsWith(FileProvider.getPushwooshStacktraceSuffix());
            File[] list = dir.listFiles(filter);
            return list.length;
        } else {
            Assert.fail("Crash dir doesn't exist");
        }
        throw new Exception("Can't get saved crash reports number. Test failed.");
    }

    public Exception getNotPushwooshException() {
        Exception exception = new Exception();

        StackTraceElement[] notPushwooshStackTraceElenements = new StackTraceElement[6];
        notPushwooshStackTraceElenements[0] = new StackTraceElement("com.blah.ExceptionHandlerTest", "testNotPushwooshError", "ExceptionHandlerTest.java", 48);
        notPushwooshStackTraceElenements[1] = new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2);
        notPushwooshStackTraceElenements[2] = new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 62);
        notPushwooshStackTraceElenements[3] = new StackTraceElement("sun.reflect.DelegatingMethodAccessorImpl", "invoke", "DelegatingMethodAccessorImpl.java", 43);
        notPushwooshStackTraceElenements[4] = new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 498);
        notPushwooshStackTraceElenements[5] = new StackTraceElement("org.junit.runners.model.FrameworkMethod$1", "runReflectiveCall", "FrameworkMethod.java", 50);

        exception.setStackTrace(notPushwooshStackTraceElenements);

        return exception;
    }
}
