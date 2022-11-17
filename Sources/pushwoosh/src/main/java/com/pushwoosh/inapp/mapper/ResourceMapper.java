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

package com.pushwoosh.inapp.mapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.pushwoosh.inapp.InAppConfig;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Class helper which map network {@link com.pushwoosh.inapp.network.model.Resource} model to view
 * {@link com.pushwoosh.inapp.model.HtmlData} model.
 */
public class ResourceMapper {
	private static final String TAG = "[InApp]ResourceMapper";

	private final InAppFolderProvider inAppFolderProvider;
	private final InAppConfig config;

	public ResourceMapper(InAppFolderProvider inAppFolderProvider) {
		this.inAppFolderProvider = inAppFolderProvider;
		config = new InAppConfig(inAppFolderProvider);
	}

	@WorkerThread
	public HtmlData map(Resource resource) throws IOException {
		String baseUrl = Uri.fromFile(inAppFolderProvider.getInAppFolder(resource.getCode())).toString();
		String htmlData = getHtmlData(resource.getCode(), resource.getTags());

		return new HtmlData(resource.getCode(), baseUrl, htmlData);
	}

	protected String getHtmlData(String code, Map<String, String> tags) throws IOException {
		File html = inAppFolderProvider.getInAppHtmlFile(code);
		String content = FileUtils.readFile(html);

		try {

			content = postProcessHtml(content, Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}\\}", Pattern.DOTALL), config.parseLocalizedStrings(code));

			// DOTALL is not safe here for it can false-positively match javascript like { if (a|b|c) {} }
			content = postProcessHtml(content, Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}\\}"), tags);

			// support template syntax like {{ Placeholder name | Type }}
			content = postProcessHtml(content, Pattern.compile("\\{\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\}\\}"), config.parseLocalizedStrings(code));

			// support dynamic content in Rich Medias with no default value
			content = postProcessHtml(content, Pattern.compile("\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|\\}"), tags);

			// support dynamic content in Rich Medias with a default value
			content = postProcessHtml(content, Pattern.compile("\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|(.[^\\}]*?)\\}"), tags);

		} catch (Exception e) {
			// Not error. Early inapps do not contain pushwoosh.json
			PWLog.warn(TAG, "Failed to process html: " + e.getMessage());
		}

		return content;
	}

	private String postProcessHtml(String content, Pattern pattern, Map<String, String> tags) {
		Matcher matcher = pattern.matcher(content);

		while (matcher.find()) {
			if (matcher.groupCount() == 3) {
				content = processKeyTypeDefaultValuePattern(content, matcher.group(0), matcher.group(1), matcher.group(2), matcher.group(3), tags);
			} else if (matcher.groupCount() == 2) {
				//replace dynamic content placeholder with no default value with empty string
				if (pattern.toString().equals("\\{(.[^\\}]+?)\\|(.[^\\}]+?)\\|\\}")) {
					content = processKeyTypeDefaultValuePattern(content, matcher.group(0), matcher.group(1), matcher.group(2), "", tags);
				}
				content = processKeyTypeDefaultValuePattern(content, matcher.group(0), matcher.group(1), matcher.group(2), matcher.group(1), tags);
			} else {
				PWLog.warn(TAG, "Incorrect matching count");
			}
		}

		return content;
	}

	private String processKeyTypeDefaultValuePattern(String content, String totalKey, String key, String type, String defaultValue, Map<String, String> tags) {
		PWLog.noise(TAG, "Key: \"" + key + "\", Type: \"" + type + "\", Default Value: \"" + defaultValue + "\"");

		String value = defaultValue;
		if (tags.containsKey(key)) {
			value = tags.get(key);
			value = InAppTagFormatModifier.format(value, type);
		}
		content = content.replace(totalKey, value);
		PWLog.debug(TAG, "Replacing \"" + totalKey + "\" with \"" + value + "\"");

		return content;
	}
}
