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

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.utils.PWLog;

import static com.pushwoosh.richmedia.RichMedia.Source.InAppSource;
import static com.pushwoosh.richmedia.RichMedia.Source.PushMessageSource;

/**
 * `RichMedia` class represents Rich Media page object.
 */
public class RichMedia {
	public enum Source {
		/**
		 * RichMedia is presented from push notification.
		 */
		PushMessageSource,

		/**
		 * RichMedia is presented from In-App.
		 */
		InAppSource
	}

	private final String TAG = RichMedia.class.getSimpleName();
	private String content;
	private Source source;
	private ResourceWrapper resourceWrapper;
	private boolean isLockScreen;
	private boolean isRequired;

	RichMedia(ResourceWrapper currentInApp) {
		this.resourceWrapper = currentInApp;
		Resource resource = currentInApp.getResource();
		this.isLockScreen = resourceWrapper.isLockScreen();
		if (resource == null) {
			PWLog.error(TAG, "resource is empty");
			return;
		}
		this.isRequired = resource.isRequired();
		this.isLockScreen = resourceWrapper.isLockScreen();


		switch (resourceWrapper.getResourceType()) {
			case IN_APP:
				this.content = resource.getCode();
				this.source = InAppSource;
				break;
			case RICH_MEDIA:
				this.content = resource.getCode();
				this.source = PushMessageSource;
		}
	}

	/**
	 * Rich Media presenter type.
	 */
	public Source getSource() {
		return source;
	}

	/**
	 * Content of the Rich Media. For InAppSource it's equal to In-App code, for PushMessageSource it's equal to Rich Media code.
	 */
	public String getContent() {
		return content;
	}

	ResourceWrapper getResourceWrapper() {
		return resourceWrapper;
	}

	/**
	 * Check if the Rich Media will show on a lock screen.
	 */
	public boolean isLockScreen() {
		return isLockScreen;
	}

	/**
	 * Checks if InAppSource is a required In-App. Always returns true for PWRichMediaSourcePush.
	 */
	public boolean isRequired() {
		return isRequired;
	}

	@Override
	public String toString() {
		return "RichMedia{" + "content='" + content + '\'' + ", resourceType=" + source + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RichMedia richMedia = (RichMedia) o;

		if (!content.equals(richMedia.content)) return false;
		return source == richMedia.source;
	}

	@Override
	public int hashCode() {
		int result = content.hashCode();
		result = 31 * result + source.hashCode();
		return result;
	}
}
