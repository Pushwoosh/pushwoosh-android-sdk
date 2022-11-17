package com.pushwoosh.inbox.repository;

import android.os.AsyncTask;

import com.pushwoosh.function.Callback;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class NotifyObserversHelper<T> {
    private final Collection<Callback<T, InboxMessagesException>> observers = new ConcurrentLinkedQueue<>();
    private final InboxRepositoryTask<T> task;

    NotifyObserversHelper(InboxRepositoryTask<T> task) {
        this.task = task;
    }

    void addObserver(Callback<T, InboxMessagesException> callback) {
        if (callback == null) {
            return;
        }
        observers.add(callback);
    }

    void removeObserver(Callback<T, InboxMessagesException> callback) {
        if (callback == null) {
            return;
        }
        observers.remove(callback);
    }

    void notifyObservers() {
        new InboxObserverAsyncTask<>(task, result -> {
            for (Callback<T, InboxMessagesException> expectedCallback : observers) {
                if (expectedCallback != null) {
                    try {
                        expectedCallback.process(result);
                    } catch (Exception e) {
                        PWLog.error("Error occurred while processing Callback", e.getMessage());
                    }
                }
            }
        }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }
}
