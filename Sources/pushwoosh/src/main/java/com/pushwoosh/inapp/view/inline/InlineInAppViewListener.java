package com.pushwoosh.inapp.view.inline;

public interface InlineInAppViewListener {
    /**
     * This method is called to notify you that an inline in-app
     * was loaded and has been added to the view
     */
    void onInlineInAppLoaded();
    /**
     * This method is called to notify you that an inline in-app
     * view has been closed by the user
     */
    void onInlineInAppViewClosed();
    /**
     * This method is called to notify you that an inline in-app
     * view size has been changed
     */
    void onInlineInAppViewChangedSize(int width, int height);
}
