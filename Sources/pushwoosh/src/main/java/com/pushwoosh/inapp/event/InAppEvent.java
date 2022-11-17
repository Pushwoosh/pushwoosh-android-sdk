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

package com.pushwoosh.inapp.event;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.event.Event;

/**
 * Event class for internal testing API
 * Is needed to test key events during inapp deployment and presenting
 */
public class InAppEvent implements Event {
	private final Resource mInapp;

	private final EventType mType;

	public InAppEvent(EventType event, Resource inapp) {
		mType = event;
		mInapp = inapp;
	}

	public String getCode() {
		return mInapp.getCode();
	}

	public EventType getType() {
		return mType;
	}

	public enum EventType {
		DOWNLOADING_ZIP,
		DOWNLOADED_ZIP,

		DEPLOYED,
		DEPLOY_FAILED
	}
}
