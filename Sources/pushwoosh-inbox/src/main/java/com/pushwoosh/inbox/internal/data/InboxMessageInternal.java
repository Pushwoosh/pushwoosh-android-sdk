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

package com.pushwoosh.inbox.internal.data;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.notification.InboxPayloadDataProvider;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import static com.pushwoosh.inbox.network.NetworkDataHelper.ACTION_PARAMS;
import static com.pushwoosh.inbox.network.NetworkDataHelper.ACTION_TYPE;
import static com.pushwoosh.inbox.network.NetworkDataHelper.EXPIRED_DATE;
import static com.pushwoosh.inbox.network.NetworkDataHelper.HASH;
import static com.pushwoosh.inbox.network.NetworkDataHelper.IMAGE;
import static com.pushwoosh.inbox.network.NetworkDataHelper.INBOX_ID;
import static com.pushwoosh.inbox.network.NetworkDataHelper.INBOX_ORDER;
import static com.pushwoosh.inbox.network.NetworkDataHelper.INBOX_STATUS;
import static com.pushwoosh.inbox.network.NetworkDataHelper.SEND_DATE;
import static com.pushwoosh.inbox.network.NetworkDataHelper.TEXT;
import static com.pushwoosh.inbox.network.NetworkDataHelper.TITLE;

public class InboxMessageInternal implements Serializable, Comparable<InboxMessageInternal> {
	private static final long serialVersionUID = -4917174252005788670L;
	private final String id;
	private final long order;
	private final long expiredDate;
	private final long sendDate;
	private final String hash;
	private final String title;
	private final String message;
	private final String image;
	private final InboxMessageType inboxMessageType;
	private final String actionParams;
	private final String bannerUrl;
	private final InboxMessageStatus inboxMessageStatus;
	private final InboxMessageSource source;

	private InboxMessageInternal(String id, long order, long expiredDate, long sendDate, String hash, String title, String message, String image, InboxMessageType inboxMessageType, String actionParams, String bannerUrl, InboxMessageStatus inboxMessageStatus, InboxMessageSource source) {
		this.id = id;
		this.order = order;
		this.expiredDate = expiredDate;
		this.sendDate = sendDate;
		this.hash = hash;
		this.title = title;
		this.message = message;
		this.image = image;
		this.inboxMessageType = inboxMessageType;
		this.actionParams = actionParams;
		this.bannerUrl = bannerUrl;
		this.inboxMessageStatus = inboxMessageStatus;
		this.source = source;
	}

	public String getId() {
		return id;
	}

	public long getOrder() {
		return order;
	}

	/**
	 * @return Expired date in seconds
	 */
	public long getExpiredDate() {
		return expiredDate;
	}

	/**
	 * @return Send date in seconds
	 */
	public long getSendDate() {
		return sendDate;
	}

	public String getHash() {
		return hash;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public String getImage() {
		return image;
	}

	public InboxMessageType getInboxMessageType() {
		return inboxMessageType;
	}

	public String getActionParams() {
		return actionParams;
	}

	public String getBannerUrl() {
		return bannerUrl;
	}

	public InboxMessageStatus getInboxMessageStatus() {
		return inboxMessageStatus;
	}

	public InboxMessageSource getSource() {
		return source;
	}

	boolean isRead() {
		switch (inboxMessageStatus) {
			case DELIVERED:
				return false;
			case READ:
			case OPEN:
				return true;
			case DELETED_BY_USER:
			case DELETED_FROM_SERVICE:
				return false;
		}

		return false;
	}

	boolean isActionCompleted() {
		switch (inboxMessageStatus) {
			case DELIVERED:
			case READ:
				return false;
			case OPEN:
				return true;
			case DELETED_BY_USER:
			case DELETED_FROM_SERVICE:
				return false;
		}

		return false;
	}

	public boolean isDeleted() {
		switch (inboxMessageStatus) {
			case DELIVERED:
			case READ:
			case OPEN:
				return false;
			case DELETED_BY_USER:
			case DELETED_FROM_SERVICE:
				return true;
		}

		return false;
	}

	public String getPushMetadata() {
		if (TextUtils.isEmpty(actionParams)) {
			return null;
		}
		try {
			JSONObject jsonObject = new JSONObject(actionParams);
			if (jsonObject.has("md")) {
				return jsonObject.get("md").toString();
			}
		} catch (JSONException e) {
			PWLog.error(e.getMessage());
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		InboxMessageInternal that = (InboxMessageInternal) o;

		return id != null ? id.equals(that.id) : that.id == null;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public int compareTo(@NonNull InboxMessageInternal o) {
		int compare = source.compare(o.source);
		if (compare == 0) {
			compare = Long.valueOf(sendDate).compareTo(o.sendDate);
		}
		
		if (compare == 0) {
			compare = Long.valueOf(order).compareTo(o.order);
		}

		if (compare == 0) {
			if (title != null && o.title != null) {
				compare = title.compareTo(o.title);
			} else if (title == null && o.title != null) {
				compare = 1;
			} else if (title != null) {
				compare = -1;
			}
		}

		if (compare == 0) {
			compare = id.compareTo(o.id);
		}
		return compare;
	}

	public static class Builder {
		private String id;
		private long order;
		private long expiredDate;
		private long sendDate;
		private String hash;
		private String title;
		private String message;
		private String image;
		private InboxMessageType inboxMessageType;
		private String actionParams;
		private String bannerUrl;
		private InboxMessageStatus inboxMessageStatus;
		private InboxMessageSource source;

		public Builder() {/*do nothing*/}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setOrder(long order) {
			this.order = order;
			return this;
		}

		public Builder setHash(String hash) {
			this.hash = hash;
			return this;
		}

		public Builder setExpiredDate(long expiredDate) {
			this.expiredDate = expiredDate;
			return this;
		}

		public Builder setSendDate(long sendDate) {
			this.sendDate = sendDate;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setMessage(String message) {
			this.message = message;
			return this;
		}

		public Builder setImage(String image) {
			this.image = image;
			return this;
		}

		public Builder setInboxMessageType(InboxMessageType inboxMessageType) {
			this.inboxMessageType = inboxMessageType;
			return this;
		}

		public Builder setActionParams(String actionParams) {
			this.actionParams = actionParams;
			setBannerUrl(actionParams);
			return this;
		}

		public Builder setBannerUrl(String actionParams) {
			if (!TextUtils.isEmpty(actionParams)) {
				try {
					JSONObject actionParamsJson = new JSONObject(actionParams);
					if (actionParamsJson.has("b")) {
						this.bannerUrl = actionParamsJson.get("b").toString();
					}
				}
				catch (JSONException ignore) {
					//ignore.printStackTrace();
				}
			}
			return this;
		}

		public Builder setInboxMessageStatus(InboxMessageStatus inboxMessageStatus) {
			this.inboxMessageStatus = inboxMessageStatus;
			return this;
		}

		public Builder setSource(InboxMessageSource source) {
			this.source = source;
			return this;
		}

		public Builder setJsonObject(JSONObject jsonObject) throws JSONException, InboxInvalidArgumentException {
			if (!jsonObject.has(INBOX_ID) || !jsonObject.has(INBOX_ORDER) || !jsonObject.has(EXPIRED_DATE) || !jsonObject.has(TEXT)
					|| !jsonObject.has(ACTION_TYPE) || !jsonObject.has(INBOX_STATUS)) {
				throw new InboxInvalidArgumentException();
			}

			id = jsonObject.getString(INBOX_ID);

			// as per org.json.JSONObject documentation, JSONObject.getLong() is lossy, so we obtain long values
			// from received JSON as strings and then parse them to long
			// https://developer.android.com/reference/org/json/JSONObject#getLong(java.lang.String)
			order = Long.parseLong(jsonObject.getString(INBOX_ORDER));
			expiredDate = Long.parseLong(jsonObject.getString(EXPIRED_DATE));

			message = jsonObject.getString(TEXT);
			inboxMessageType = InboxMessageType.getByCode(jsonObject.getInt(ACTION_TYPE));
			inboxMessageStatus = InboxMessageStatus.getByCode(jsonObject.getInt(INBOX_STATUS));

			if (jsonObject.has(SEND_DATE)) {
				sendDate = jsonObject.getLong(SEND_DATE);
			}
			if (jsonObject.has(TITLE)) {
				title = jsonObject.getString(TITLE);
			}
			if (jsonObject.has(IMAGE)) {
				image = jsonObject.getString(IMAGE);
			}
			if(jsonObject.has(ACTION_PARAMS)) {
				actionParams = jsonObject.getString(ACTION_PARAMS);
				setBannerUrl(actionParams);
			}
			if (jsonObject.has(HASH)){
				hash = jsonObject.getString(HASH);
			}

			source = InboxMessageSource.SERVICE;
			return this;
		}

		public Builder setInboxMessage(InboxMessage inboxMessage) {
			id = inboxMessage.getCode();
			//order = System.currentTimeMillis();
			expiredDate = -1;
			sendDate = inboxMessage.getSendDate().getTime();
			title = inboxMessage.getTitle();
			message = inboxMessage.getMessage();
			image = inboxMessage.getImageUrl();
			inboxMessageType = inboxMessage.getType();
			inboxMessageStatus = inboxMessage.isActionPerformed() ? InboxMessageStatus.OPEN : inboxMessage.isRead() ? InboxMessageStatus.READ : InboxMessageStatus.DELIVERED;
			actionParams = "";
			source = InboxMessageSource.SERVICE;
			hash = "";
			return this;
		}

		public Builder setPushBundle(Bundle pushBundle) {
			id = InboxPayloadDataProvider.getInboxId(pushBundle);
			//order = System.currentTimeMillis();
			title = InboxPayloadDataProvider.getTitle(pushBundle);
			message = InboxPayloadDataProvider.getMessage(pushBundle);
			try {
				JSONObject inboxParams = new JSONObject(InboxPayloadDataProvider.getInboxParams(pushBundle));
				if (inboxParams.has(IMAGE)) {
					image = inboxParams.getString(IMAGE);
				}
				if (inboxParams.has(EXPIRED_DATE)) {
					expiredDate = inboxParams.getLong(EXPIRED_DATE);
				}
			} catch (JSONException e) {
				PWLog.error("Problem with parsing inboxParams", e);
			}
			sendDate = TimeUnit.MILLISECONDS.toSeconds(InboxPayloadDataProvider.getSentTime(pushBundle));
			inboxMessageType = InboxPayloadDataProvider.getInboxType(pushBundle);
			actionParams = JsonUtils.bundleToJsonWithUserData(pushBundle).toString();
			hash = InboxPayloadDataProvider.getHash(pushBundle);

			inboxMessageStatus = InboxMessageStatus.DELIVERED;
			source = InboxMessageSource.PUSH;
			return this;
		}

		public InboxMessageInternal build() {
			return new InboxMessageInternal(id, order, expiredDate, sendDate, hash, title, message, image, inboxMessageType, actionParams, bannerUrl, inboxMessageStatus, source);
		}
	}

	public static class InboxInvalidArgumentException extends Exception {

	}
}
