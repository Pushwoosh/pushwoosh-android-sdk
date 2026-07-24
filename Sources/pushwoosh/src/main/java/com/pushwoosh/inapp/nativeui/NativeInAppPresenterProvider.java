package com.pushwoosh.inapp.nativeui;

import androidx.annotation.Nullable;

/**
 * Single-slot holder for the {@link NativeInAppPresenter} implementation (deliberately not a
 * chain, unlike MessageHandleChainProvider). Set from InAppUiPlugin.init(), which runs
 * synchronously in PushwooshInitProvider.onCreate() before Application.onCreate(), so every
 * show path observes the presenter whenever the module is present; null means the module is
 * not connected.
 */
public final class NativeInAppPresenterProvider {
    private static volatile NativeInAppPresenter presenter;

    private NativeInAppPresenterProvider() {}

    public static void set(@Nullable NativeInAppPresenter value) {
        presenter = value;
    }

    @Nullable public static NativeInAppPresenter get() {
        return presenter;
    }
}
