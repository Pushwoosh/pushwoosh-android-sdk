package com.pushwoosh.liveupdates.internal;

import android.app.Notification;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateSegment;
import com.pushwoosh.liveupdates.LiveUpdateState;

/**
 * Default {@link LiveUpdateProgressStyleProvider}: builds a {@link Notification.ProgressStyle}
 * from the parsed state (progress, indeterminate flag, per-segment length and color). Used when
 * no provider is declared in the manifest, and as the fallback when a custom provider throws. A
 * custom provider typically copies this logic as a starting point.
 */
@RequiresApi(36)
public class DefaultProgressStyleProvider implements LiveUpdateProgressStyleProvider {

    @WorkerThread
    @NonNull @Override
    public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
        Notification.ProgressStyle style = new Notification.ProgressStyle();
        if (state.getProgress() != null) {
            style.setProgress(state.getProgress());
        }
        style.setProgressIndeterminate(state.isProgressIndeterminate());
        for (LiveUpdateSegment seg : state.getSegments()) {
            style.addProgressSegment(new Notification.ProgressStyle.Segment(seg.getLength()).setColor(seg.getColor()));
        }
        return style;
    }
}
