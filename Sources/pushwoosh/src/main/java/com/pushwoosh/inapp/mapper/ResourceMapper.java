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

import androidx.annotation.WorkerThread;

import com.pushwoosh.inapp.InAppConfig;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Class helper which map network {@link com.pushwoosh.inapp.network.model.Resource} model to view
 * {@link com.pushwoosh.inapp.model.HtmlData} model.
 */
public class ResourceMapper {
    private static final String TAG = "[InApp]ResourceMapper";

    // Synthetic https origin replaces legacy file:// so target=_blank can't leak a file:// URI to Chromium →
    // FileUriExposedException.
    public static final String RICH_MEDIA_ASSET_HOST = "appassets.androidplatform.net";
    public static final String RICH_MEDIA_PATH_PREFIX = "/pushwoosh_richmedia/";

    private final InAppFolderProvider inAppFolderProvider;
    private final InAppConfig config;

    public ResourceMapper(InAppFolderProvider inAppFolderProvider) {
        this.inAppFolderProvider = inAppFolderProvider;
        config = new InAppConfig(inAppFolderProvider);
    }

    @WorkerThread
    public HtmlData map(Resource resource) throws IOException {
        String baseUrl = "https://" + RICH_MEDIA_ASSET_HOST + RICH_MEDIA_PATH_PREFIX + resource.getCode() + "/";
        String htmlData = getHtmlData(resource.getCode(), resource.getTags());

        return new HtmlData(resource.getCode(), baseUrl, htmlData);
    }

    protected String getHtmlData(String code, Map<String, String> tags) throws IOException {
        File html = inAppFolderProvider.getInAppHtmlFile(code);
        String content = FileUtils.readFile(html);

        try {
            content = PlaceholderSubstitutor.substitute(content, config.parseLocalizedStrings(code), tags);
        } catch (Exception e) {
            // Not error. Early inapps do not contain pushwoosh.json
            PWLog.warn(TAG, "Failed to process html", e);
        }

        return content;
    }
}
