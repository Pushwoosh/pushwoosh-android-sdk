package com.pushwoosh.inapp.nativeui;

import androidx.annotation.NonNull;

import com.pushwoosh.inapp.network.model.Resource;

/**
 * Entry point for presenting a native (non-WebView) in-app. Implemented by the
 * pushwoosh-inapp-ui module and registered via {@link NativeInAppPresenterProvider}.
 */
public interface NativeInAppPresenter {
    /**
     * Called on the main thread. Heavy work (network, images) must be async inside the renderer.
     *
     * @param configJson raw content of native-config.json from the resource ZIP, passed as is
     * @param resource   resource the config was delivered with (carries code for analytics)
     * @return true if the show was accepted; false on malformed JSON, missing displayType,
     *         unknown template type or failed guard
     */
    boolean present(@NonNull String configJson, @NonNull Resource resource);
}
