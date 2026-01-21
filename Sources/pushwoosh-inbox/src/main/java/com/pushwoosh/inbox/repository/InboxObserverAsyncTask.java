package com.pushwoosh.inbox.repository;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.internal.utils.BackgroundExecutor;

class InboxObserverAsyncTask<T> {
    private final InboxRepositoryTask<T> task;
    private final Callback<T, InboxMessagesException> callback;

    public InboxObserverAsyncTask(InboxRepositoryTask<T> task, Callback<T, InboxMessagesException> callback) {
        this.task = task;
        this.callback = callback;
    }

    public void execute() {
        BackgroundExecutor.serial(() -> {
            T result = task.run();

            if (callback != null) {
                BackgroundExecutor.main(() -> callback.process(Result.fromData(result)));
            }
        });
    }
}
