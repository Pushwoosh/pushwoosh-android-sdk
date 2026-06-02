package com.pushwoosh.test.manifest;

import android.app.Notification;

import androidx.annotation.NonNull;

import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateState;

// ManifestValidator fixture: implements the interface with a public no-arg ctor. createStyle is
// NEVER invoked (only base-type + ctor are checked); it returns null on purpose because core
// tests run on Robolectric SDK 35, where Notification.ProgressStyle (API 36) does not exist.
public class FakeValidLiveUpdateStyleProvider implements LiveUpdateProgressStyleProvider {
    public FakeValidLiveUpdateStyleProvider() {}

    @NonNull @Override
    public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
        return null;
    }
}
