package com.pushwoosh.test.manifest;

import android.app.Notification;

import androidx.annotation.NonNull;

import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateState;

// ManifestValidator fixture: implements the interface but has NO public no-arg constructor.
// createStyle is never invoked (see FakeValidLiveUpdateStyleProvider).
public class FakeNoCtorLiveUpdateStyleProvider implements LiveUpdateProgressStyleProvider {
    @SuppressWarnings("unused")
    public FakeNoCtorLiveUpdateStyleProvider(int unused) {}

    @NonNull @Override
    public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
        return null;
    }
}
