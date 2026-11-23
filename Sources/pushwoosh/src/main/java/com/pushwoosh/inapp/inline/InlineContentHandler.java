package com.pushwoosh.inapp.inline;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Receives the {@code inline} block of a successful postEvent / purchase answer. Implemented by the
 * inline module; {@code userId} and {@code appCode} are the ones the request was sent with.
 */
public interface InlineContentHandler {
    @MainThread
    void onInlineContent(@NonNull JSONObject inline, @Nullable String userId, @Nullable String appCode);
}
