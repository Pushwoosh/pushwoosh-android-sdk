/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.richmedia;

import android.text.TextUtils;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.inapp.event.RichMediaCloseEvent;
import com.pushwoosh.inapp.event.RichMediaErrorEvent;
import com.pushwoosh.inapp.event.RichMediaPresentEvent;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppFolderProvider;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.inapp.view.strategy.model.ResourceType;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;

import static com.pushwoosh.inapp.view.strategy.model.ResourceType.IN_APP;

public class RichMediaController {
	private String TAG = RichMediaController.class.getSimpleName();

	private ResourceViewStrategyFactory resourceViewStrategyFactory;
	private RichMediaFactory richMediaFactory;

	private RichMediaPresentingDelegate delegate;
	private InAppFolderProvider inAppFolderProvider;
	private RichMediaStyle richMediaStyle;

	public RichMediaController(ResourceViewStrategyFactory resourceViewStrategyFactory,
							   RichMediaFactory richMediaFactory,
							   InAppFolderProvider inAppFolderProvider,
							   RichMediaStyle richMediaStyle) {
		this.richMediaStyle = richMediaStyle;
		this.resourceViewStrategyFactory = resourceViewStrategyFactory;
		this.richMediaFactory = richMediaFactory;
		this.inAppFolderProvider = inAppFolderProvider;
		EventBus.subscribe(RichMediaCloseEvent.class, this::onCloseRichMedia);
		EventBus.subscribe(RichMediaPresentEvent.class, this::onPresentRichMedia);
		EventBus.subscribe(RichMediaErrorEvent.class, this::onErrorRichMedia);
	}

	public void present(RichMedia richMedia) {
		PWLog.noise(TAG, "try show richMedia");
		if (richMedia != null) {
			PWLog.noise(TAG, "showRichMedia with content:" + richMedia.getContent());
			resourceViewStrategyFactory.showResource(richMedia.getResourceWrapper());
		} else {
			PWLog.error(TAG, "richMedia is null");
		}
	}

	private void onPresentRichMedia(RichMediaPresentEvent event) {
		PWLog.noise(TAG, "handle present RichMedia");
		if (delegate != null) {
			PWLog.noise(TAG, "try use delegate onPresent");
			Resource resource = event.getResource();
			if (resource != null) {
				if (isRemoteUrl(resource)) return;
				RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
				delegate.onPresent(richMedia);
			} else {
				PWLog.error(TAG, "resource in event is null");
			}
		} else {
			PWLog.noise(TAG, "delegate is null");
		}
	}

	private boolean isRemoteUrl(Resource resource) {
		if (TextUtils.isEmpty(resource.getCode())) {
			PWLog.noise(TAG, "code is empty, resource is not RichMedia, abort use delegate");
			return true;
		}
		return false;
	}

	private void onErrorRichMedia(RichMediaErrorEvent event) {
		if (event == null) {
			return;
		}
		if (event.getException() != null) {
			PWLog.error(event.getException().getMessage());
		}
		PWLog.noise(TAG, "handle error RichMedia");
		if (delegate != null) {
			PWLog.noise(TAG, "try use delegate onError");
			Resource resource = event.getResource();
			if (resource != null) {
				if (isRemoteUrl(resource)) return;
				RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
				PushwooshException exception = event.getException();
				delegate.onError(richMedia, exception);
			} else {
				PWLog.error(TAG, "resource in event is null");
			}
		} else {
			PWLog.noise(TAG, "delegate is null");
		}
	}


	private void onCloseRichMedia(RichMediaCloseEvent event) {
		PWLog.noise(TAG, "handle close RichMedia");
		if (delegate != null) {
			PWLog.noise(TAG, "try use delegate onClose");
			Resource resource = event.getResource();
			if (resource != null) {
				if (isRemoteUrl(resource)) return;
				RichMedia richMedia = richMediaFactory.buildRichMedia(resource);
				delegate.onClose(richMedia);
			} else {
				PWLog.error(TAG, "resource in event is null");
			}
		} else {
			PWLog.noise(TAG, "delegate is null");
		}
	}

	private RichMedia buildRichMedia(ResourceWrapper resourceWrapper) {
		return richMediaFactory.buildRichMedia(resourceWrapper);
	}

	public void setDelegate(RichMediaPresentingDelegate delegate) {
		this.delegate = delegate;
	}

	private void useDelegate(ResourceWrapper resourceWrapper) {
		if (isCanceled(resourceWrapper)) return;

		boolean isWillShow = delegate.shouldPresent(buildRichMedia(resourceWrapper));
		if (isWillShow) {
			resourceViewStrategyFactory.showResource(resourceWrapper);
		}
	}

	private boolean isCanceled(ResourceWrapper resourceWrapper) {
		Resource resource = resourceWrapper.getResource();
		if(resource == null){
			PWLog.error(TAG, "resource is null, abort show RichMedia");
			return true;
		}
		if (resourceWrapper.getResourceType() == IN_APP && !resource.isRequired()) {
			if (!inAppFolderProvider.isInAppDownloaded(resource.getCode())) {
				PWLog.error(TAG, "resource is not downloaded, abort show RichMedia");
				return true;
			}
		}
		return false;
	}

	public void showResourceWrapper(ResourceWrapper resourceWrapper) {
		if (delegate != null) {
			useDelegate(resourceWrapper);
		} else {
			resourceViewStrategyFactory.showResource(resourceWrapper);
		}
	}

	public RichMediaStyle getRichMediaStyle() {
		return richMediaStyle;
	}
}
