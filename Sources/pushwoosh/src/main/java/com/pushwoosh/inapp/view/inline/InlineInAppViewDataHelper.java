package com.pushwoosh.inapp.view.inline;

import android.os.Parcel;

import com.pushwoosh.exception.PostEventException;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.view.DownloadHtmlTask;
import com.pushwoosh.inapp.view.InAppViewEvent;
import com.pushwoosh.inapp.view.WebClient;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.tags.TagsBundle;

class InlineInAppViewDataHelper implements DownloadHtmlTask.DownloadListener {
    private DownloadHtmlTask downloadHtmlDataTask;
    private Resource resource;
    private InAppRepository inAppRepository;
    private InlineInAppView view;
    private boolean inAppActionSent;

    InlineInAppViewDataHelper(InlineInAppView view, InAppRepository inAppRepository) {
        this.inAppRepository = inAppRepository;
        this.view = view;
    }

    void requestData(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return;
        }

        inAppActionSent = false;

        TagsBundle tagsBundle =
                new TagsBundle.Builder()
                        .putString("identifier", identifier)
                        .build();

        inAppRepository.postEvent("inlineInApp", tagsBundle, this::download);
    }

    private void download(Result<Resource, PostEventException> result) {
        resource = result.getData();

        view.resourceUpdated(resource);

        if (resource != null && result.getException() == null) {
            downloadHtmlDataTask = new DownloadHtmlTask(resource, this);
            downloadHtmlDataTask.execute();
        }
    }

    @Override
    public void startLoading() {

    }

    @Override
    public void sendResult(Result<HtmlData, ResourceParseException> result) {
        if (result.isSuccess()) {
            view.htmlDataLoaded(result.getData());
        }
    }


    void sendInAppEvent() {
        if (resource != null && !inAppActionSent) {
            EventBus.sendEvent(new InAppViewEvent(resource));
            inAppActionSent = true;
        }
    }

    SavedState getSaveState() {
        return new SavedState(inAppActionSent);
    }

    void applySavedState(SavedState state) {
        inAppActionSent = state.inAppActionSent;
    }

    static class SavedState {
        private boolean inAppActionSent;
        public void save(Parcel parcel) {
            parcel.writeInt(inAppActionSent ? 1 : 0);
        }
        SavedState(Parcel parcel) {
            inAppActionSent = parcel.readInt() == 1;
        }
        private SavedState(boolean inAppActionSent) {
            this.inAppActionSent = inAppActionSent;
        }
    }
}
