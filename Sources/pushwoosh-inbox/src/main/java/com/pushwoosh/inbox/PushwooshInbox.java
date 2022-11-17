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

package com.pushwoosh.inbox;

import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.internal.PushwooshInboxModule;
import com.pushwoosh.inbox.internal.action.InboxPerformActionStrategyFactory;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.internal.mapping.InboxMessageToInternalInboxMessageMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PushwooshInbox {

	/**
	 * Get the number of the {@link com.pushwoosh.inbox.data.InboxMessage} with no action performed
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void messagesWithNoActionPerformedCount(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().loadCountOfInboxMessagesWithNoActionPerformed(callback);
	}

	/**
	 * Register the observer to get updates of the number of the {@link com.pushwoosh.inbox.data.InboxMessage} with no action performed
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void registerMessagesWithNoActionPerformedCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().addMessagesWithNoActionPerformedCountObserver(callback);
	}

	/**
	 * Unregister the observer of the number of the {@link com.pushwoosh.inbox.data.InboxMessage} with no action performed
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void unregisterMessagesWithNoActionPerformedCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().removeMessagesWithNoActionPerformedCountObserver(callback);
	}

	/**
	 * Get the number of the unread {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the number of the unread InboxMessages. Otherwise, return error
	 */
	public static void unreadMessagesCount(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().loadCountOfUnread(callback);
	}

	/**
	 * Register the observer to get the number of the unread {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void registerUnreadMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().addUnreadMessagesCountObserver(callback);
	}

	/**
	 * Unregister the observer of the unread {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void unregisterUnreadMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().removeUnreadMessagesCountObserver(callback);
	}

	/**
	 * Get the total number of the {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the total number of the InboxMessages. Otherwise, return error
	 */
	public static void messagesCount(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().loadCountOfMessages(callback);
	}

	/**
	 * Register the observer to get the total number of the {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void registerMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().addMessagesCountObserver(callback);
	}

	/**
	 * Unregister the observer of the total number of the {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param callback - if successful, return the number of the InboxMessages with no action performed. Otherwise, return error
	 */
	public static void unregisterMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		PushwooshInboxModule.getInboxRepository().removeMessagesCountObserver(callback);
	}

	/**
	 * Get the collection of the {@link com.pushwoosh.inbox.data.InboxMessage} that the user received
	 * This method obtains messages from network. In case the network connection is not available messages
	 * will be obtained from local database
	 * @param callback - if successful, return the collection of the InboxMessages. Otherwise, return error
	 */
	public static void loadMessages(Callback<Collection<InboxMessage>, InboxMessagesException> callback) {
		loadMessages(callback, null, -1);
	}

	/**
	 * Get the collection of the {@link com.pushwoosh.inbox.data.InboxMessage} that the user received.
	 * This method obtains messages synchronously from local database
	 *
	 * @param inboxMessage - This parameter provides pagination. Pass the last {@link com.pushwoosh.inbox.data.InboxMessage}
	 *                       that is on your current page as a parameter to get previous messages.
	 *                       To get the latest messages or in case the pagination is not necessary, pass null as a parameter.
	 * @param limit - amount of messages to get. Pass -1 to get all the messages
	 */
	public static Collection<InboxMessage> loadCachedMessages(@Nullable InboxMessage inboxMessage,
															  int limit) throws InboxMessagesException {
		return PushwooshInboxModule.getInboxRepository().loadCachedMessages(inboxMessage, limit);
	}

	/**
	 * Get the collection of the {@link com.pushwoosh.inbox.data.InboxMessage} that the user received.
	 * This method obtains messages asynchronously from local database
	 *
	 * @param callback - if successful, return the collection of the InboxMessages. Otherwise, return error
	 * @param inboxMessage - This parameter provides pagination. Pass the last {@link com.pushwoosh.inbox.data.InboxMessage}
	 *                       that is on your current page as a parameter to get previous messages.
	 *                       To get the latest messages or in case the pagination is not necessary, pass null as a parameter.
	 * @param limit - amount of messages to get. Pass -1 to get all the messages
	 */
	public static void loadCachedMessages(Callback<Collection<InboxMessage>, InboxMessagesException> callback,
										  @Nullable InboxMessage inboxMessage,
										  int limit) throws InboxMessagesException {
		PushwooshInboxModule.getInboxRepository().loadCachedMessages(callback, inboxMessage, limit);
	}

	/**
	 * Get the collection of the {@link com.pushwoosh.inbox.data.InboxMessage} that the user received
	 * This method obtains messages from network. In case the network connection is not available messages
	 * will be obtained from local database
	 *
	 * @param callback - if successful, return the collection of the InboxMessages. Otherwise, return error
	 * @param inboxMessage - This parameter provides pagination. Pass the last {@link com.pushwoosh.inbox.data.InboxMessage}
	 *                       that is on your current page as a parameter to get previous messages.
	 *                       To get latest messages or in case the pagination is not necessary, pass null as a parameter.
	 * @param limit - amount of messages to get. Pass -1 to get all the messages
	 */
	public static void loadMessages(Callback<Collection<InboxMessage>, InboxMessagesException> callback,
									@Nullable InboxMessage inboxMessage,
									int limit) {
		PushwooshInboxModule.getInboxRepository().loadMessages(callback, inboxMessage, limit);
	}

	/**
	 * Call this method, when the user reads the {@link com.pushwoosh.inbox.data.InboxMessage}
	 *
	 * @param code of the inboxMessage
	 */
	public static void readMessage(String code) {
		readMessages(Collections.singleton(code));
	}

	/**
	 * Call this method, when the user reads list of {@link InboxMessage}
	 *
	 * @param codes of the inboxMessages
	 */
	@SuppressWarnings("WeakerAccess")
	public static void readMessages(Collection<String> codes) {
		changeStatus(codes, InboxMessageStatus.READ);
	}

	/**
	 * Call this method, when the user clicks on the {@link com.pushwoosh.inbox.data.InboxMessage} and the message's action is performed
	 *
	 * @param code of the inboxMessage that the user tapped
	 */
	public static void performAction(String code) {
		InboxMessageToInternalInboxMessageMapper mapper = new InboxMessageToInternalInboxMessageMapper();
		PushwooshInboxModule.getInboxRepository().updateStatus(Collections.singletonMap(code, InboxMessageStatus.OPEN), true, result -> {
			if (result.isSuccess()) {
				InboxMessageInternal message = mapper.map(result.getData());
				InboxPerformActionStrategyFactory.onActionPerformed(message);
			}
		});
	}

	/**
	 * Call this method, when the user deletes the {@link com.pushwoosh.inbox.data.InboxMessage} manually
	 *
	 * @param code of the inboxMessage that the user deleted
	 */
	public static void deleteMessage(String code) {
		deleteMessages(Collections.singleton(code));
	}

	/**
	 * Call this method, when the user deletes the list of {@link com.pushwoosh.inbox.data.InboxMessage} manually
	 *
	 * @param codes of the list of {@link com.pushwoosh.inbox.data.InboxMessage#getCode()} that the user deleted
	 */
	@SuppressWarnings("WeakerAccess")
	public static void deleteMessages(Collection<String> codes) {
		changeStatus(codes, InboxMessageStatus.DELETED_BY_USER);
	}

	private static void changeStatus(Collection<String> codes, InboxMessageStatus status) {
		Map<String, InboxMessageStatus> map = new HashMap<>(codes.size());
		for (String code : codes) {
			map.put(code, status);
		}
		PushwooshInboxModule.getInboxRepository().updateStatus(map, false, null);
	}
}
