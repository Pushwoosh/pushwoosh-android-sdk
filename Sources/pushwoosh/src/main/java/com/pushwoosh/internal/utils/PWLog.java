package com.pushwoosh.internal.utils;

import com.pushwoosh.repository.RepositoryModule;

/**
 * Proxy class for android.util.Log for custom log level managing
 */
public final class PWLog {

	public static void setLogsUpdateListener(final LogsUpdateListener logsUpdateListener) {
		PWLog.logsUpdateListener = logsUpdateListener;
	}

	public enum Level {
		NONE,
		FATAL,
		ERROR,
		WARN,
		INFO,
		DEBUG,
		NOISE,
		INTERNAL
	}

	private static final String DEFAULT_TAG = "Pushwoosh";

	private static boolean initialized = false;

	private static Level currentLevel = Level.INFO;
	private static LogsUpdateListener logsUpdateListener;

	private static boolean isEnabled(Level l) {
		return currentLevel.compareTo(l) >= 0;
	}

	public static void init() {
		if (initialized) {
			return;
		}

		String level = RepositoryModule.getRegistrationPreferences().logLevel().get();
		if (level != null) {
			try {
				currentLevel = Level.valueOf(level);
				if (currentLevel == Level.INTERNAL) {
					// internal level is prohibited in manifest
					currentLevel = Level.INFO;
				}
			} catch (IllegalArgumentException e) {
				error("Unrecognized log level: " + level);
			}
		}

		info("Log level: " + currentLevel.name());
		initialized = true;
	}

	public static void updateLogLevel(String level) {
		if (!level.isEmpty() && Level.INTERNAL != Level.valueOf(level)) {
			currentLevel = Level.valueOf(level);
		}
	}

	private static boolean isErrorLevel() {
		return isEnabled(Level.ERROR);
	}

	private static boolean isWarnLevel() {
		return isEnabled(Level.WARN);
	}

	private static boolean isInfoLevel() {
		return isEnabled(Level.INFO);
	}

	private static boolean isDebugLevel() {
		return isEnabled(Level.DEBUG);
	}

	private static boolean isNoiseLevel() {
		return isEnabled(Level.NOISE);
	}

	private static boolean isFatalLevel() {
		return isEnabled(Level.FATAL);
	}

	private static boolean isInternalLevel() {
		return isEnabled(Level.INTERNAL);
	}

	// android.util.Log public constants

	public static final int ASSERT = android.util.Log.ASSERT;

	public static final int DEBUG = android.util.Log.DEBUG;

	public static final int ERROR = android.util.Log.ERROR;

	public static final int INFO = android.util.Log.INFO;

	public static final int VERBOSE = android.util.Log.VERBOSE;

	public static final int WARN = android.util.Log.WARN;

	// android.util.Log public methods

	public static boolean isLoggable(String tag, int level) {
		switch (level) {

			case android.util.Log.ASSERT:
				return isFatalLevel();

			case android.util.Log.ERROR:
				return isErrorLevel();

			case android.util.Log.WARN:
				return isWarnLevel();

			case android.util.Log.INFO:
				return isInfoLevel();

			case android.util.Log.DEBUG:
				return isDebugLevel();

			case android.util.Log.VERBOSE:
				return isNoiseLevel();

			default:
				return false;

		}
	}

	private static String buildMessage(String subTag, String msg) {
		return (subTag != null) ? ("[" + subTag + "] " + msg) : (msg);
	}

	public static void fatal(String subTag, String msg) {
		if (isFatalLevel()) {
			android.util.Log.wtf(DEFAULT_TAG, buildMessage(subTag, msg));
		}

		notifyListener(Level.FATAL, buildMessage(subTag, msg));
	}

	private static void notifyListener(Level level, final String message) {
		if (logsUpdateListener != null) {
			logsUpdateListener.logUpdated(level, message);
		}
	}

	public static void error(String subTag, String msg) {
		if (isErrorLevel()) {
			android.util.Log.e(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.ERROR, buildMessage(subTag, msg));
	}

	public static void warn(String subTag, String msg) {
		if (isWarnLevel()) {
			android.util.Log.w(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.WARN, buildMessage(subTag, msg));
	}

	public static void info(String subTag, String msg) {
		if (isInfoLevel()) {
			android.util.Log.i(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.INFO, buildMessage(subTag, msg));
	}

	public static void debug(String subTag, String msg) {
		if (isDebugLevel()) {
			android.util.Log.d(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.DEBUG, buildMessage(subTag, msg));
	}

	public static void noise(String subTag, String msg) {
		if (isNoiseLevel()) {
			android.util.Log.v(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.NONE, buildMessage(subTag, msg));
	}

	public static void internal(String subTag, String msg) {
		if (isInternalLevel()) {
			android.util.Log.v(DEFAULT_TAG, buildMessage(subTag, msg));
		}
		notifyListener(Level.INTERNAL, buildMessage(subTag, msg));
	}

	public static void fatal(String subTag, String msg, Throwable tr) {
		if (isFatalLevel()) {
			android.util.Log.wtf(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void error(String subTag, String msg, Throwable tr) {
		if (isErrorLevel()) {
			android.util.Log.e(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void warn(String subTag, String msg, Throwable tr) {
		if (isWarnLevel()) {
			android.util.Log.w(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void info(String subTag, String msg, Throwable tr) {
		if (isInfoLevel()) {
			android.util.Log.i(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void debug(String subTag, String msg, Throwable tr) {
		if (isDebugLevel()) {
			android.util.Log.d(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void noise(String subTag, String msg, Throwable tr) {
		if (isNoiseLevel()) {
			android.util.Log.v(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	public static void internal(String subTag, String msg, Throwable tr) {
		if (isInternalLevel()) {
			android.util.Log.v(DEFAULT_TAG, buildMessage(subTag, msg), tr);
		}
	}

	// default tag methods

	public static void fatal(String msg) {
		fatal(null, msg);
	}

	public static void error(String msg) {
		error(null, msg);
	}

	public static void warn(String msg) {
		warn(null, msg);
	}

	public static void info(String msg) {
		info(null, msg);
	}

	public static void debug(String msg) {
		debug(null, msg);
	}

	public static void noise(String msg) {
		noise(null, msg);
	}

	public static void internal(String msg) {
		internal(null, msg);
	}

	public static void fatal(String msg, Throwable tr) {
		fatal(null, msg, tr);
	}

	public static void error(String msg, Throwable tr) {
		error(null, msg, tr);
	}

	public static void warn(String msg, Throwable tr) {
		warn(null, msg, tr);
	}

	public static void info(String msg, Throwable tr) {
		info(null, msg, tr);
	}

	public static void debug(String msg, Throwable tr) {
		debug(null, msg, tr);
	}

	public static void noise(String msg, Throwable tr) {
		noise(null, msg, tr);
	}

	public static void internal(String msg, Throwable tr) {
		internal(null, msg, tr);
	}

	public static void exception(Throwable tr) {
		error(null, "Exception occurred", tr);
	}

	public interface LogsUpdateListener {
		void logUpdated(Level level, String message);
	}
}
