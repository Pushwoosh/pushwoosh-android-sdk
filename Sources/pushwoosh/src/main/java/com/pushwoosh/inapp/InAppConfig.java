package com.pushwoosh.inapp;

import androidx.annotation.WorkerThread;

import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class InAppConfig {
	private static final String TAG = "[InApp]InAppConfig";

	private static class Column {
		static final String KEY_LOCALIZATION = "localization";
		static final String KEY_DEFAULT_LANGUAGE = "default_language";
		static final String KEY_MODAL_POSITION = "position";
		static final String KEY_PRESENT_ANIMATION = "present_animation";
		static final String KEY_DISMISS_ANIMATION = "dismiss_animation";
		static final String KEY_SWIPE_TO_DISMISS = "swipe_to_dismiss";
	}

	private InAppFolderProvider inAppFolderProvider;

	public InAppConfig(InAppFolderProvider inAppFolderProvider) {
		this.inAppFolderProvider = inAppFolderProvider;
	}

	private void parseModalPosition(JSONObject json, ModalRichmediaConfig config) {
		if (json.has(Column.KEY_MODAL_POSITION)) {
			try {
				String position = json.getString(Column.KEY_MODAL_POSITION);
				ModalRichMediaViewPosition pos = ModalRichMediaViewPosition.fromString(position);
				if (pos != null) {
					config.setViewPosition(pos);
				}
			} catch (JSONException e) {
				PWLog.warn(TAG, "Failed to parse " + Column.KEY_MODAL_POSITION + ": " + e.getMessage());
			}
		}
	}

	private void parsePresentAnimation(JSONObject json, ModalRichmediaConfig config) {
		if (json.has(Column.KEY_PRESENT_ANIMATION)) {
			try {
				String animation = json.getString(Column.KEY_PRESENT_ANIMATION);
				ModalRichMediaPresentAnimationType anim = ModalRichMediaPresentAnimationType.fromString(animation);
				if (anim != null) {
					config.setPresentAnimationType(anim);
				}
			} catch (JSONException e) {
				PWLog.warn(TAG, "Failed to parse " + Column.KEY_PRESENT_ANIMATION + ": " + e.getMessage());
			}
		}
	}

	private void parseDismissAnimation(JSONObject json, ModalRichmediaConfig config) {
		if (json.has(Column.KEY_DISMISS_ANIMATION)) {
			try {
				String animation = json.getString(Column.KEY_DISMISS_ANIMATION);
				ModalRichMediaDismissAnimationType anim = ModalRichMediaDismissAnimationType.fromString(animation);
				if (anim != null) {
					config.setDismissAnimationType(anim);
				}
			} catch (JSONException e) {
				PWLog.warn(TAG, "Failed to parse " + Column.KEY_DISMISS_ANIMATION + ": " + e.getMessage());
			}
		}
	}

	private void parseSwipeGestures(JSONObject json, ModalRichmediaConfig config) {
		if (json.has(Column.KEY_SWIPE_TO_DISMISS)) {
			try {
				JSONArray swipeArray = json.getJSONArray(Column.KEY_SWIPE_TO_DISMISS);
				Set<ModalRichMediaSwipeGesture> swipes = new HashSet<>();
				for (int i = 0; i < swipeArray.length(); i++) {
					ModalRichMediaSwipeGesture gesture = ModalRichMediaSwipeGesture.fromString(swipeArray.getString(i));
					if (gesture != null && gesture != ModalRichMediaSwipeGesture.NONE) {
						swipes.add(gesture);
					}
				}
				config.setSwipeGestures(swipes);
			} catch (JSONException e) {
				PWLog.warn(TAG, "Failed to parse " + Column.KEY_SWIPE_TO_DISMISS + ": " + e.getMessage());
			}
		}
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
			PWLog.warn(TAG, "Preferred language not found, fall back to default");
			localizedStrings = localization.getJSONObject(defaultLanguage);
		}

		return parseLocalizedStrings(localizedStrings);
	}

	@WorkerThread
	public ModalRichmediaConfig parseModalConfig(String code) throws IOException {
		PWLog.noise(TAG, "parseModalConfig started for code: " + code);
		
		File configFile = inAppFolderProvider.getConfigFile(code);
		if (!configFile.exists()) {
			return null;
		}
		
		try {
			String content = FileUtils.readFile(configFile);
			if (content == null || content.trim().isEmpty()) {
				PWLog.warn(TAG, "Config file exists but is empty for code: " + code);
				return null;
			}
			
			JSONObject json = new JSONObject(content);
			JSONObject styleSettingsJson = json.optJSONObject("style_settings");
			
			ModalRichmediaConfig config = new ModalRichmediaConfig();

			if (styleSettingsJson != null) {
				parseModalPosition(styleSettingsJson, config);
				parsePresentAnimation(styleSettingsJson, config);
				parseDismissAnimation(styleSettingsJson, config);
				parseSwipeGestures(styleSettingsJson, config);
			}
			
			return config;
			
		} catch (JSONException e) {
			PWLog.error(TAG, "Invalid JSON in config file for code: " + code, e);
			throw new IOException("Malformed config file", e); // Convert to IOException for consistency
		}
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
