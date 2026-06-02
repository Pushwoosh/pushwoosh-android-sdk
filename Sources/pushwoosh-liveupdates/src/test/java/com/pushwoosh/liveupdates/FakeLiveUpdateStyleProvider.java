package com.pushwoosh.liveupdates;

import android.annotation.SuppressLint;
import android.app.Notification;

import androidx.annotation.NonNull;

/** Valid provider with a public no-arg ctor, resolved by FQN in LiveUpdatesPluginTest. */
@SuppressLint("NewApi")
public class FakeLiveUpdateStyleProvider implements LiveUpdateProgressStyleProvider {
    public FakeLiveUpdateStyleProvider() {}

    @NonNull @Override
    public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
        return new Notification.ProgressStyle();
    }
}
