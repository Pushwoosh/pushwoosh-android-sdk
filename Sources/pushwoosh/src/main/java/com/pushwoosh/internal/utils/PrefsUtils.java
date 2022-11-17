package com.pushwoosh.internal.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

public class PrefsUtils {
	private static final String KEY_SEPARATOR = "$##$";
	private static final String EMPTY = "";
	private static final int INDEX_NOT_FOUND = -1;

	/**
	 * Save a Bundle object to SharedPreferences.
	 * <p/>
	 * NOTE: The editor must be writable, and this function does not commit.
	 *
	 * @param editor      SharedPreferences Editor
	 * @param key         SharedPreferences key under which to store the bundle data. Note this key must
	 *                    not contain '$$' as it's used as a delimiter
	 * @param preferences Bundled preferences
	 */
	public static void putBundle(SharedPreferences.Editor editor, String key, Bundle preferences) {
		Set<String> keySet = preferences.keySet();
		Iterator<String> it = keySet.iterator();
		String prefKeyPrefix = key + KEY_SEPARATOR;

		while (it.hasNext()) {
			String bundleKey = it.next();
			Object o = preferences.get(bundleKey);
			if (o == null) {
				editor.remove(prefKeyPrefix + bundleKey);
			} else if (o instanceof Integer) {
				editor.putInt(prefKeyPrefix + bundleKey, (Integer) o);
			} else if (o instanceof Long) {
				editor.putLong(prefKeyPrefix + bundleKey, (Long) o);
			} else if (o instanceof Boolean) {
				editor.putBoolean(prefKeyPrefix + bundleKey, (Boolean) o);
			} else if (o instanceof CharSequence) {
				editor.putString(prefKeyPrefix + bundleKey, o.toString());
			} else if (o instanceof Bundle) {
				putBundle(editor, prefKeyPrefix + bundleKey, ((Bundle) o));
			}
		}
	}

	/**
	 * Load a Bundle object from SharedPreferences.
	 * (that was previously stored using savePreferencesBundle())
	 * <p/>
	 * NOTE: The editor must be writable, and this function does not commit.
	 *
	 * @param prefs SharedPreferences
	 * @param key   SharedPreferences key under which to store the bundle data. Note this key must
	 *              not contain '$$' as it's used as a delimiter
	 * @return bundle loaded from SharedPreferences
	 */
	public static Bundle getBundle(SharedPreferences prefs, String key) {
		Bundle bundle = new Bundle();
		Map<String, ?> all = prefs.getAll();
		String prefKeyPrefix = key + KEY_SEPARATOR;
		Set<String> subBundleKeys = new HashSet<>();

		for (Map.Entry<String, ?> entry : all.entrySet()) {

			String prefKey = entry.getKey();

			if (prefKey.startsWith(prefKeyPrefix)) {
				String bundleKey = removeStart(prefKey, prefKeyPrefix);

				if (!bundleKey.contains(KEY_SEPARATOR)) {

					Object o = entry.getValue();
					if (o == null) {
						// Ignore null keys
					} else if (o instanceof Integer) {
						bundle.putInt(bundleKey, (Integer) o);
					} else if (o instanceof Long) {
						bundle.putLong(bundleKey, (Long) o);
					} else if (o instanceof Boolean) {
						bundle.putBoolean(bundleKey, (Boolean) o);
					} else if (o instanceof CharSequence) {
						bundle.putString(bundleKey, o.toString());
					}
				} else {
					// Key is for a sub bundle
					String subBundleKey = substringBefore(bundleKey, KEY_SEPARATOR);
					subBundleKeys.add(subBundleKey);
				}
			} else {
				// Key is not related to this bundle.
			}
		}

		// Recursively process the sub-bundles
		for (String subBundleKey : subBundleKeys) {
			Bundle subBundle = getBundle(prefs, prefKeyPrefix + subBundleKey);
			bundle.putBundle(subBundleKey, subBundle);
		}


		return bundle;
	}

	private static String removeStart(String str, String remove) {
		if (TextUtils.isEmpty(str) || TextUtils.isEmpty(remove)) {
			return str;
		}
		if (str.startsWith(remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

	private static String substringBefore(String str, String separator) {
		if (TextUtils.isEmpty(str) || separator == null) {
			return str;
		}
		if (separator.length() == 0) {
			return EMPTY;
		}
		int pos = str.indexOf(separator);
		if (pos == INDEX_NOT_FOUND) {
			return str;
		}
		return str.substring(0, pos);
	}
}
