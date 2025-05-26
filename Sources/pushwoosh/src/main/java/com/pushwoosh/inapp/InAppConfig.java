package com.pushwoosh.inapp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import androidx.annotation.WorkerThread;

import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;


public class InAppConfig {
	private static final String TAG = "[InApp]InAppConfig";

	private static class Column {
		static final String KEY_LOCALIZATION = "localization";
		static final String KEY_DEFAULT_LANGUAGE = "default_language";
	}

	private InAppFolderProvider inAppFolderProvider;

	public InAppConfig(InAppFolderProvider inAppFolderProvider) {
		this.inAppFolderProvider = inAppFolderProvider;
	}

	@WorkerThread
	public Map<String, String> parseLocalizedStrings(String code) throws IOException, JSONException {
		File configFile = inAppFolderProvider.getConfigFile(code);
		String content = FileUtils.readFile(configFile);
		JSONObject json = new JSONObject(content);
		JSONObject localization = json.getJSONObject(Column.KEY_LOCALIZATION);

		String defaultLanguage = json.getString(Column.KEY_DEFAULT_LANGUAGE);
		PWLog.debug(TAG, "default language : " + defaultLanguage);

		String preferredLanguage = RepositoryModule.getRegistrationPreferences().language().get();
		JSONObject localizedStrings;

		try {
			localizedStrings = localization.getJSONObject(preferredLanguage);
		} catch (JSONException e) {
			// expected
			PWLog.warn(TAG, "Preferred language not found, fall back to default");
			localizedStrings = localization.getJSONObject(defaultLanguage);
		}

		return parseLocalizedStrings(localizedStrings);
	}

	

	private Map<String, String> parseLocalizedStrings(JSONObject json) throws JSONException {
		Map<String, String> result = new HashMap<>();

		PWLog.debug(TAG, "Localization : {");
		Iterator<String> iter = json.keys();
		while (iter.hasNext()) {
			String key = iter.next();
			String value = json.getString(key);
			result.put(key, value);
			PWLog.debug(TAG, "  \"" + key + "\" : \"" + value + "\"");
		}
		PWLog.debug(TAG, "}");

		return result;
	}
}
