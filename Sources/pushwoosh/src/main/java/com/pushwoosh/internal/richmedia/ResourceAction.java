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

package com.pushwoosh.internal.richmedia;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.richmedia.RichMediaController;

/**
 * Action which happen when click on {@link com.pushwoosh.inapp.network.model.Resource} push
 */
public final class ResourceAction {

	/**
	 * Show view associated with {@param richMedia}
	 * @param richMedia id of rich media
	 */
	public static void performRichMediaAction(String richMedia) {
		ResourceWrapper resourceWrapper = new ResourceWrapper.Builder()
				.setRichMedia(richMedia)
				.setDelay(RepositoryModule.getNotificationPreferences().richMediaDelayMs().get())
				.build();

		RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
		if (richMediaController != null)
			richMediaController.showResourceWrapper(resourceWrapper);
	}

	/**
	 * Show view associated with {@param remoteUrl}
	 * @param remoteUrl - url from push
	 */
	public static void performRemoteUrlAction(String remoteUrl){
		ResourceWrapper resourceWrapper = new ResourceWrapper.Builder()
				.setRemoteUrl(remoteUrl)
				.build();

		RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
		if (richMediaController != null)
			richMediaController.showResourceWrapper(resourceWrapper);
	}

	private ResourceAction() {
	}
}
