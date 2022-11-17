package com.pushwoosh.inbox.repository;

import android.os.AsyncTask;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.exception.InboxMessagesException;

class InboxObserverAsyncTask<T> extends AsyncTask<Void, Void, T> {
    private final InboxRepositoryTask<T> task;
    private final Callback<T, InboxMessagesException> callback;

    public InboxObserverAsyncTask(InboxRepositoryTask<T> task, Callback<T, InboxMessagesException> callback) {
        this.task = task;
        this.callback = callback;
    }

    @Override
    protected T doInBackground(Void... voids) {
        return task.run();
    }

    @Override
    protected void onPostExecute(T result) {
        super.onPostExecute(result);
        if (callback != null) {
            callback.process(Result.fromData(result));
        }
    }
}