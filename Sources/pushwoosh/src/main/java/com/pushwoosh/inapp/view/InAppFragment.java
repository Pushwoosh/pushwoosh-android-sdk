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

package com.pushwoosh.inapp.view;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.model.Resource;

import java.lang.ref.WeakReference;

import static com.pushwoosh.inapp.view.InAppFragment.Status.ERROR;
import static com.pushwoosh.inapp.view.InAppFragment.Status.LOADING;
import static com.pushwoosh.inapp.view.InAppFragment.Status.SUCCESS;


public class InAppFragment extends Fragment implements DownloadHtmlTask.DownloadListener {
    private static final String TAG = "[InApp]InAppFragment";

    private static final String KEY_INAPP = "keyInapp";

    private static final String KEY_STATE = TAG + ".key_STATE";
    private static final String KEY_HTML_DATA = TAG + ".key_HTML_DATA";
    private static final String KEY_ERROR = TAG + ".key_ERROR";

    public enum Status {
        LOADING, SUCCESS, ERROR, NONE
    }


    public static InAppFragment createInstance(Resource inapp) {
        InAppFragment instance = new InAppFragment();

        Bundle args = new Bundle(1);
        args.putSerializable(KEY_INAPP, inapp);

        instance.setArguments(args);
        return instance;
    }


    private AsyncTask<Void, Void, Result<HtmlData, ResourceParseException>> downloadHtmlDataTask;
    private WeakReference<OnRichMediaListener> onRichMediaListener = new WeakReference<>(null);

    private Status state = Status.NONE;
    private HtmlData htmlData;
    private ResourceParseException resourceParseException;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() == null) {
            return;
        }

        Resource inApp = (Resource) getArguments().getSerializable(KEY_INAPP);
        if (savedInstanceState == null) {
            downloadHtmlData(inApp);
            return;
        }

        int stateInt = savedInstanceState.getInt(KEY_STATE);
        state = Status.values()[stateInt];
        htmlData = (HtmlData) savedInstanceState.getSerializable(KEY_HTML_DATA);
        resourceParseException = (ResourceParseException) savedInstanceState.getSerializable(KEY_ERROR);

        if (state != SUCCESS && state != ERROR) {
            downloadHtmlData(inApp);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downloadHtmlDataTask != null) {
            downloadHtmlDataTask.cancel(true);
            downloadHtmlDataTask = null;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        syncState();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_ERROR, resourceParseException);
        outState.putSerializable(KEY_HTML_DATA, htmlData);
        outState.putInt(KEY_STATE, state.ordinal());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onRichMediaListener = null;
    }

    public void downloadHtmlData(final Resource inApp) {
        downloadHtmlDataTask = new DownloadHtmlTask(inApp, this);
        downloadHtmlDataTask.execute();
    }

    private void updateState(Result<HtmlData, ResourceParseException> result) {
        if (result.isSuccess()) {
            state = SUCCESS;
            htmlData = result.getData();
        } else {
            state = ERROR;
            resourceParseException = result.getException();
        }
    }

    private void notifyListener(Result<HtmlData, ResourceParseException> result) {
        OnRichMediaListener onRichMediaListener = this.onRichMediaListener.get();
        if (onRichMediaListener == null) {
            return;
        }

        if (result.isSuccess()) {
            if (!onRichMediaListener.successLoadingHtmlData(result.getData())) {
                onRichMediaListener.finishLoading();
            }
        } else {
            onRichMediaListener.finishLoading();
            onRichMediaListener.failedLoadingHtmlData(result.getException());
        }
    }

    public void syncState() {
        if (getActivity() instanceof OnRichMediaListener) {
            onRichMediaListener = new WeakReference<>((OnRichMediaListener) getActivity());
        }

        final OnRichMediaListener onRichMediaListener = this.onRichMediaListener.get();
        if (onRichMediaListener == null) {
            return;
        }

        switch (state) {
            case SUCCESS:
                onRichMediaListener.finishLoading();
                onRichMediaListener.successLoadingHtmlData(htmlData);
                break;
            case ERROR:
                onRichMediaListener.failedLoadingHtmlData(resourceParseException);
                onRichMediaListener.finishLoading();
                break;
            case LOADING:
                onRichMediaListener.startLoading();
                break;
            default:
                if (getArguments() == null) {
                    return;
                }

                Resource inApp = (Resource) getArguments().getSerializable(KEY_INAPP);
                downloadHtmlData(inApp);
                break;
        }
    }

    @Override
    public void startLoading() {
        this.state = LOADING;
        final InAppFragment.OnRichMediaListener onRichMediaListener = InAppFragment.this.onRichMediaListener.get();
        if (onRichMediaListener != null) {
            onRichMediaListener.startLoading();
        }

    }

    @Override
    public void sendResult(Result<HtmlData, ResourceParseException> result) {
        updateState(result);
        notifyListener(result);
    }


    interface OnRichMediaListener {
        void startLoading();

        void finishLoading();

        boolean successLoadingHtmlData(HtmlData htmlData);

        void failedLoadingHtmlData(ResourceParseException exception);
    }
}
