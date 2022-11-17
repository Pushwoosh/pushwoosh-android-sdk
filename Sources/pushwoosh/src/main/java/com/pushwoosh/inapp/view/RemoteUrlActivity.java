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

package com.pushwoosh.inapp.view;

import android.content.Context;
import android.content.Intent;

import com.pushwoosh.inapp.network.model.Resource;

public class RemoteUrlActivity extends WebActivity {

	private String url;

	public static void startRemotePage(Context context, String url) {
		Intent intent = new Intent(context, RemoteUrlActivity.class);
		applyIntentParams(intent, new Resource(url), "", MODE_REMOTE_PAGE);
		context.startActivity(intent);
	}

	@Override
	protected void resourceChanged(Resource resource, String sound, int mode) {
		if (mode == InAppView.MODE_REMOTE_PAGE) {
			url = resource.getUrl();
		} else {
			close();
		}
	}

	@Override
	protected void updateWebView(ResourceWebView resourceWebView) {
		if(resourceWebView == null){
			return;
		}

		resourceWebView.loadUrl(url);
		setContentView(resourceWebView);
	}
}
