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

package com.pushwoosh.inbox.repository;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.event.InboxMessagesUpdatedEvent;
import com.pushwoosh.inbox.event.InboxMessagesUpdatedEventBuilder;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.internal.InboxConfig;
import com.pushwoosh.inbox.internal.data.InboxMessageImpl;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageSource;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.internal.mapping.InternalInboxMessagesToInboxMessagesMapper;
import com.pushwoosh.inbox.network.checker.NeedUpdateChecker;
import com.pushwoosh.inbox.network.data.GetInboxMessagesRequest;
import com.pushwoosh.inbox.network.data.GetInboxMessagesResponse;
import com.pushwoosh.inbox.network.data.UpdateInboxMessageStatusRequest;
import com.pushwoosh.inbox.repository.data.LoadResult;
import com.pushwoosh.inbox.storage.InboxStorage;
import com.pushwoosh.inbox.storage.data.MergeResult;
import com.pushwoosh.internal.command.CommandApplayer;
import com.pushwoosh.internal.command.CommandParams;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.UserIdUpdatedEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.notification.builder.NotificationBuilderManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class InboxRepository {

	private final RequestManager requestManager;
	private final InboxStorage inboxStorage;
	private final CommandApplayer commandApplayer;

	private final InternalInboxMessagesToInboxMessagesMapper mapper = new InternalInboxMessagesToInboxMessagesMapper();
	private final NeedUpdateChecker needUpdateMessages = new NeedUpdateChecker(InboxConfig.getInboxUpdateTime());

	private NotifyListenersHelper<Integer> inboxMessagesWithNoActionPerformedListeners;
	private NotifyListenersHelper<Integer> unreadInboxCountListeners;
	private NotifyListenersHelper<Integer> inboxCountListeners;

	private NotifyObserversHelper<Integer> inboxMessagesWithNoActionPerformedObservers;
	private NotifyObserversHelper<Integer> unreadInboxCountObservers;
	private NotifyObserversHelper<Integer> inboxCountObservers;

	private final Handler handler = new Handler(Looper.getMainLooper());

	public InboxRepository(RequestManager requestManager,
						   InboxStorage inboxStorage,
						   CommandApplayer commandApplayer) {
		this.requestManager = requestManager;
		this.inboxStorage = inboxStorage;
		this.commandApplayer = commandApplayer;

		initListenerHelpers();
		initObserverHelpers();

		EventBus.subscribe(UserIdUpdatedEvent.class, event -> syncStorageIfNeeded(true,null));
	}

	private void initListenerHelpers() {
		inboxMessagesWithNoActionPerformedListeners = new NotifyListenersHelper<>(networkResult -> {
			final InboxMessagesException exception = networkResult.getException() == null ?
					null :
					new InboxMessagesException("Can't update count of the inboxMessages with no action performed", networkResult.getException());

			return Result.from(inboxStorage.getCountWithNoActionPerformed(), exception);
		});

		unreadInboxCountListeners = new NotifyListenersHelper<>(networkResult -> {
			final InboxMessagesException exception = networkResult.getException() == null ?
					null :
					new InboxMessagesException("Can't update count of the unread inboxMessages", networkResult.getException());

			return Result.from(inboxStorage.getUnreadInboxCount(), exception);
		});

		inboxCountListeners = new NotifyListenersHelper<>(networkResult -> {
			final InboxMessagesException exception = networkResult.getException() == null ?
					null :
					new InboxMessagesException("Can't update total count of the inboxMessages", networkResult.getException());

			return Result.from(inboxStorage.getTotalCount(), exception);
		});
	}

	private void initObserverHelpers() {
		inboxMessagesWithNoActionPerformedObservers = new NotifyObserversHelper<>(inboxStorage::getCountWithNoActionPerformed);
		unreadInboxCountObservers = new NotifyObserversHelper<>(inboxStorage::getUnreadInboxCount);
		inboxCountObservers = new NotifyObserversHelper<>(inboxStorage::getTotalCount);
	}

	@WorkerThread
	public void addMessage(InboxMessageInternal inboxMessageInternal) {
		if (inboxMessageInternal.getMessage() != null) {
			final MergeResult mergeResult = mergeStateWithStorage(Collections.singleton(inboxMessageInternal), false);
			notifyEventBus(new LoadResult(mergeResult.getNewItems(), mergeResult.getUpdatedItems(), mergeResult.getDeletedItems()));
			handler.post(this::notifyObservers);
		} else { //silent push
			syncStorageIfNeeded(true, null);
		}
	}

	public void updateStatus(String inboxId, InboxMessageStatus inboxMessageStatus, Callback<InboxMessage, InboxMessagesException> callback) {
		updateStatus(Collections.singletonMap(inboxId, inboxMessageStatus), false, callback);
	}

	public void updateStatus(Map<String, InboxMessageStatus> map, boolean pushStatAllowed, @Nullable Callback<InboxMessage, InboxMessagesException> callback) {
		NetworkModule.execute(() -> {
			boolean isChanged = false;
			for (Map.Entry<String, InboxMessageStatus> statusEntry : map.entrySet()) {
				Collection<String> ids = inboxStorage.updateStatus(statusEntry.getKey(), statusEntry.getValue());
				boolean isStorageUpdated = !ids.isEmpty();
				if (isStorageUpdated) {
					isChanged = true;
					handler.post(this::notifyObservers);
				}

				if (isStorageUpdated) {
					getActualInboxMessageAndUpdateNetworkStatus(statusEntry.getKey(), statusEntry.getValue(), pushStatAllowed);
				}
			}

			Pair<Collection<InboxMessageInternal>, Collection<String>> updateStatus = mapToActualByStatus(map.keySet());

			for (InboxMessageInternal inboxMessageInternal : updateStatus.first) {
				if (callback != null) {
					final InboxMessage inboxMessage = mapper.map(inboxMessageInternal);
					handler.post(() -> {
						if (inboxMessage == null) {
							callback.process(Result.fromException(new InboxMessagesException("Unknown inbox")));
						} else {
							callback.process(Result.fromData(inboxMessage));
						}
					});
				}
			}

			if (isChanged) {
				handler.post(() -> EventBus.sendEvent(new InboxMessagesUpdatedEventBuilder()
															  .setInboxMessagesUpdated(mapToInboxMessages(updateStatus.first))
															  .setInboxMessagesDeleted(updateStatus.second)
															  .build()));
			}
		});
	}

	public void loadCountOfUnread(@Nullable Callback<Integer, InboxMessagesException> callback) {
		unreadInboxCountListeners.addListener(callback);
		syncStorageIfNeeded(false, null);
	}

	public void addUnreadMessagesCountObserver(@Nullable Callback<Integer, InboxMessagesException> callback) {
		unreadInboxCountObservers.addObserver(callback);
	}

	public void removeUnreadMessagesCountObserver(@Nullable Callback<Integer, InboxMessagesException> callback) {
		unreadInboxCountObservers.removeObserver(callback);
	}

	public void loadCountOfInboxMessagesWithNoActionPerformed(@Nullable Callback<Integer, InboxMessagesException> callback) {
		inboxMessagesWithNoActionPerformedListeners.addListener(callback);
		syncStorageIfNeeded(false, null);
	}

	public void addMessagesWithNoActionPerformedCountObserver(@Nullable Callback<Integer, InboxMessagesException> callback) {
		inboxMessagesWithNoActionPerformedObservers.addObserver(callback);
	}

	public void removeMessagesWithNoActionPerformedCountObserver(@Nullable Callback<Integer, InboxMessagesException> callback) {
		inboxMessagesWithNoActionPerformedObservers.removeObserver(callback);
	}

	public void loadCountOfMessages(Callback<Integer, InboxMessagesException> callback) {
		inboxCountListeners.addListener(callback);
		syncStorageIfNeeded(false, null);
	}

	public void addMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		inboxCountObservers.addObserver(callback);
	}

	public void removeMessagesCountObserver(Callback<Integer, InboxMessagesException> callback) {
		inboxCountObservers.removeObserver(callback);
	}

	public Collection<InboxMessage> loadCachedMessages(@Nullable InboxMessage inboxMessage, int limit) throws InboxMessagesException {
		if (inboxMessage != null && !(inboxMessage instanceof InboxMessageImpl)) {
			throw new InboxMessagesException("Provided InboxMessage is not instance of InboxMessageImpl");
		}
		long order = getInboxMessageOrder(inboxMessage);
		return mapToInboxMessages(inboxStorage.getActualMessages(order, limit));
	}

	public void loadCachedMessages(@Nullable Callback<Collection<InboxMessage>, InboxMessagesException> callback,
								   @Nullable InboxMessage inboxMessage,
								   int limit) throws InboxMessagesException {
		if (inboxMessage != null && !(inboxMessage instanceof InboxMessageImpl)) {
			throw new InboxMessagesException("Provided InboxMessage is not instance of InboxMessageImpl");
		}
		long order = getInboxMessageOrder(inboxMessage);
		GetInboxMessagesTask task = new GetInboxMessagesTask(InboxRepository.this, callback, null, order, limit);
		task.execute();
	}

	public void loadMessages(@Nullable Callback<Collection<InboxMessage>, InboxMessagesException> callback, @Nullable InboxMessage inboxMessage, int limit) {
		if (inboxMessage != null && !(inboxMessage instanceof InboxMessageImpl)) {
			if (callback != null) {
				callback.process(Result.fromException(new InboxMessagesException("Provided InboxMessage is not instance of InboxMessageImpl")));
			}
			return;
		}
		long order = getInboxMessageOrder(inboxMessage);
		Callback<LoadResult, NetworkException> networkCallback = networkResult -> {
				GetInboxMessagesTask task = new GetInboxMessagesTask(InboxRepository.this, callback, networkResult, order, limit);
				task.execute();
		};

		syncStorageIfNeeded(false, networkCallback);
	}

	private void syncStorageIfNeeded(boolean forced, @Nullable Callback<LoadResult, NetworkException> callback) {
		if (needUpdateMessages.isLoading()) {
			if (callback != null) {
				callback.process(Result.fromData(null));
			}
			return;
		}

		NetworkModule.execute(() -> {
			if (!needUpdateMessages.check() && !forced) {
				notifyListeners(Result.fromData(LoadResult.EMPTY));
				if (callback != null) {
					callback.process(Result.fromData(null));
				}
				return;
			}

			final Result<LoadResult, NetworkException> result;

			needUpdateMessages.startLoading();
			try {
				Collection<InboxMessageInternal> pushMessages = inboxStorage.getAllPushMessages(); //get inbox messages received via push

				GetInboxMessagesRequest request = new GetInboxMessagesRequest();
				final Result<GetInboxMessagesResponse, NetworkException> networkResult = requestManager.sendRequestSync(request);

				GetInboxMessagesResponse response;
				if (networkResult.isSuccess() && (response = networkResult.getData()) != null) {
					deleteFromStorage(response);
					MergeResult mergeResult = mergeStateWithStorage(response.getMessages(), true);
					mergeResult.getDeletedItems().addAll(response.getDeleted());

					for (InboxMessageInternal message : pushMessages) {
						updateStatus(Collections.singletonMap(message.getId(), message.getInboxMessageStatus()), false, null); //now push messages have service id (sortOrder) and we can update their statuses
					}

					final LoadResult data = new LoadResult(mergeResult.getNewItems(), mergeResult.getUpdatedItems(), response.getDeleted());
					notifyEventBus(data);

					result = Result.fromData(data);
				} else if (networkResult.getException() != null) {
					result = Result.fromException(networkResult.getException());
				} else {
					result = Result.fromData(LoadResult.EMPTY);
				}
			} finally {
				needUpdateMessages.finishLoading();
			}

			handler.post(this::notifyObservers);
			notifyListeners(result);
			if (callback != null) {
				callback.process(result);
			}
		});
	}

	private void notifyListeners(Result<LoadResult, NetworkException> result) {
		inboxMessagesWithNoActionPerformedListeners.notify(result);
		unreadInboxCountListeners.notify(result);
		inboxCountListeners.notify(result);
	}

	private void notifyObservers() {
		inboxCountObservers.notifyObservers();
		inboxMessagesWithNoActionPerformedObservers.notifyObservers();
		unreadInboxCountObservers.notifyObservers();
	}

	private void deleteFromStorage(GetInboxMessagesResponse response) {
		if (response.getDeleted() != null) {
			inboxStorage.deleteList(response.getDeleted());
		}
	}

	private MergeResult mergeStateWithStorage(Collection<InboxMessageInternal> messages, boolean fullList) {
		final MergeResult mergeResult = inboxStorage.mergeState(messages, fullList);
		for (Map.Entry<String, InboxMessageStatus> entry : mergeResult.getIncorrectNetworkStatus().entrySet()) {
			getActualInboxMessageAndUpdateNetworkStatus(entry.getKey(), entry.getValue(), true);
		}

		return mergeResult;
	}

	private void getActualInboxMessageAndUpdateNetworkStatus(String inboxId, InboxMessageStatus status, boolean pushStatAllowed) {
		final InboxMessageInternal actualInboxMessage = inboxStorage.getActualInboxMessage(inboxId);
		updateNetworkStatus(actualInboxMessage, status, pushStatAllowed);
	}

	private void updateNetworkStatus(InboxMessageInternal actualInboxMessage, InboxMessageStatus status, boolean pushStatAllowed) {
		//remove message from status bar
		if (actualInboxMessage != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				NotificationBuilderManager.removeInboxNotificationFromStatusBar(actualInboxMessage.getId());
			} else {
				NotificationBuilderManager.removeInboxNotification(actualInboxMessage.getId());
			}
		}

		if (actualInboxMessage != null && actualInboxMessage.getSource() == InboxMessageSource.SERVICE) {
			UpdateInboxMessageStatusRequest request = new UpdateInboxMessageStatusRequest(actualInboxMessage.getOrder(), status, actualInboxMessage.getHash());
			requestManager.sendRequest(request);
		}

		if (actualInboxMessage != null && pushStatAllowed && status == InboxMessageStatus.OPEN) {
			sendPushStat(actualInboxMessage.getHash(), actualInboxMessage.getPushMetadata());
		}
	}

	private void sendPushStat(String pushHash, String metadata) {
		Pair<String, String> pushStatParams = new Pair<>(pushHash, metadata);
		commandApplayer.applyCommand(() -> "pushStat",
				new CommandParams<>(pushStatParams));
	}

	@WorkerThread
	private void notifyEventBus(LoadResult loadResult) {
		if (loadResult != null && (loadResult != LoadResult.EMPTY || !loadResult.isEmpty())) {
			final Pair<Collection<InboxMessageInternal>, Collection<String>> collectionCollectionPair = mapToActualByStatus(loadResult.getInboxMessagesUpdated());
			collectionCollectionPair.second.addAll(loadResult.getInboxMessagesDeleted());

			final InboxMessagesUpdatedEvent event = new InboxMessagesUpdatedEventBuilder()
					.setInboxMessagesAdded(mapCodesToInboxMessage(loadResult.getInboxMessagesAdded()))
					.setInboxMessagesUpdated(mapToInboxMessages(collectionCollectionPair.first))
					.setInboxMessagesDeleted(collectionCollectionPair.second)
					.build();

			handler.post(() -> EventBus.sendEvent(event));
		}
	}

	private Collection<InboxMessage> mapCodesToInboxMessage(Collection<String> codes) {
		return mapToInboxMessages(inboxStorage.getActualInboxMessages(codes));
	}

	@NonNull
	private Collection<InboxMessage> mapToInboxMessages(@Nullable Collection<InboxMessageInternal> inboxMessages) {
		if (inboxMessages == null || inboxMessages.isEmpty()) {
			return Collections.emptyList();
		}

		final Collection<InboxMessage> result = new ArrayList<>(inboxMessages.size());
		for (InboxMessageInternal inboxMessageInternal : inboxMessages) {
			result.add(mapper.map(inboxMessageInternal));
		}
		return result;
	}

	/**
	 * Split {@link InboxMessageInternal} by deleted and non deleted status
	 *
	 * @param codes - list of codes of {@link InboxMessageInternal} which should be split
	 * @return nonnull pairs collections where first is not deleted {@link InboxMessageInternal} and second is codes of deleted {@link InboxMessageInternal}
	 */
	private Pair<Collection<InboxMessageInternal>, Collection<String>> mapToActualByStatus(@Nullable Collection<String> codes) {
		if (codes == null || codes.isEmpty()) {
			return new Pair<>(new ArrayList<>(), new ArrayList<>());
		}
		final Collection<InboxMessageInternal> actualInboxMessages = inboxStorage.getActualInboxMessages(codes);
		Iterator<InboxMessageInternal> iterator = actualInboxMessages.iterator();
		Collection<String> deleted = new ArrayList<>();
		while (iterator.hasNext()) {
			final InboxMessageInternal next = iterator.next();
			if (next.isDeleted()) {
				deleted.add(next.getId());
				iterator.remove();
			}
		}
		return new Pair<>(actualInboxMessages, deleted);
	}

	public void clearAllInboxMessages() {
		new MergeStateTask(this, Collections.emptyList(), true, mergeStateTaskResult -> {
			MergeResult mergeResult = mergeStateTaskResult.getData();
			if (mergeResult != null) {
				for (Map.Entry<String, InboxMessageStatus> entry : mergeResult.getIncorrectNetworkStatus().entrySet()) {
					new GetActualInboxMessageTask(this, entry.getKey(), getActualInboxMessageResult -> {
						InboxMessageInternal inboxMessageInternal = getActualInboxMessageResult.getData();
						updateNetworkStatus(inboxMessageInternal, entry.getValue(), true);
					}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private long getInboxMessageOrder(InboxMessage inboxMessage) {
		return inboxMessage == null ? Long.MAX_VALUE : ((InboxMessageImpl) inboxMessage).getInboxMessageInternal().getOrder();
	}

	private static class GetInboxMessagesTask extends AsyncTask<Void, Void, Collection<InboxMessage>> {
		private WeakReference<InboxRepository> inboxRepositoryReference;
		private @Nullable Callback<Collection<InboxMessage>, InboxMessagesException> callback;
		private @Nullable Result<LoadResult, NetworkException> networkResult;
		private long order;
		private int limit;

		private GetInboxMessagesTask(InboxRepository inboxRepository,
									 @Nullable Callback<Collection<InboxMessage>, InboxMessagesException> callback,
									 @Nullable Result<LoadResult, NetworkException> networkResult,
									 long order,
									 int limit) {
			inboxRepositoryReference = new WeakReference<>(inboxRepository);
			this.callback = callback;
			this.networkResult = networkResult;
			this.order = order;
			this.limit = limit;
		}

		@Override
		protected Collection<InboxMessage> doInBackground(Void... voids) {
			InboxRepository inboxRepository = inboxRepositoryReference.get();
			if (inboxRepository == null) {
				return null;
			}
			return inboxRepository.mapToInboxMessages(inboxRepository.inboxStorage.getActualMessages(order, limit));
		}

		@Override
		protected void onPostExecute(Collection<InboxMessage> inboxMessages) {
			super.onPostExecute(inboxMessages);
			InboxMessagesException exception = networkResult != null ? (networkResult.getException() == null ?
					null :
					new InboxMessagesException("Can't load inboxList", networkResult.getException())) : null;
			if (callback != null) {
				callback.process(Result.from(inboxMessages, exception));
			}
		}
	}

	private static class MergeStateTask extends AsyncTask<Void, Void, MergeResult> {
		private final WeakReference<InboxRepository> inboxRepositoryReference;
		private final Collection<InboxMessageInternal> messages;
		private final boolean fullList;
		private final @Nullable Callback<MergeResult, PushwooshException> callback;

		private MergeStateTask(InboxRepository inboxRepository,
							   Collection<InboxMessageInternal> messages,
							   boolean fullList,
							   @Nullable Callback<MergeResult, PushwooshException> callback) {
			this.inboxRepositoryReference = new WeakReference<>(inboxRepository);
			this.messages = messages;
			this.fullList = fullList;
			this.callback = callback;
		}

		@Override
		protected MergeResult doInBackground(Void... voids) {
			InboxRepository inboxRepository = inboxRepositoryReference.get();
			if (inboxRepository == null) {
				return null;
			}
			return inboxRepository.inboxStorage.mergeState(messages, fullList);
		}

		@Override
		protected void onPostExecute(MergeResult mergeResult) {
			super.onPostExecute(mergeResult);
			if (callback != null) {
				callback.process(Result.from(mergeResult, null));
			}
		}
	}

	private static class GetActualInboxMessageTask extends AsyncTask<Void, Void, InboxMessageInternal> {
		private final WeakReference<InboxRepository> inboxRepositoryReference;
		private final String inboxId;
		private final @Nullable Callback<InboxMessageInternal, PushwooshException> callback;

		private GetActualInboxMessageTask(InboxRepository inboxRepository,
										 String inboxId,
										 @Nullable Callback<InboxMessageInternal, PushwooshException> callback) {
			this.inboxRepositoryReference = new WeakReference<>(inboxRepository);
			this.inboxId = inboxId;
			this.callback = callback;
		}

		@Override
		protected InboxMessageInternal doInBackground(Void... voids) {
			InboxRepository inboxRepository = inboxRepositoryReference.get();
			if (inboxRepository == null) {
				return null;
			}
			inboxRepository.inboxStorage.getActualInboxMessage(inboxId);
			return null;
		}

		@Override
		protected void onPostExecute(InboxMessageInternal inboxMessageInternal) {
			super.onPostExecute(inboxMessageInternal);
			if (callback != null) {
				callback.process(Result.from(inboxMessageInternal, null));
			}
		}
	}
}
