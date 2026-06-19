package com.pushwoosh.liveupdates.internal;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.LiveUpdateState;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandler;

/**
 * Intercepts live-update pushes on the system message chain before they reach the default
 * notification path.
 * <p>
 * Registered by {@link com.pushwoosh.liveupdates.LiveUpdatesPlugin} as a {@link MessageSystemHandler}.
 * For each incoming push it recognizes the live-update marker, parses it into a
 * {@link LiveUpdateState}, and dispatches to the renderer obtained from its {@link RendererProvider}
 * — {@code render} on {@code START} / {@code UPDATE}, {@code dismiss} on {@code END}. The renderer is
 * fetched lazily per push (rather than held) so it always reflects the currently installed instance.
 * <p>
 * The renderer is obtained lazily rather than injected directly to avoid a hard reference to a
 * possibly-not-yet-installed renderer (e.g. below API 36 the renderer is never created).
 */
public class LiveUpdatePushHandler implements MessageSystemHandler {

    private static final String TAG = "LiveUpdatePushHandler";

    /** Supplies the currently installed renderer, or {@code null} if none (e.g. below API 36). */
    public interface RendererProvider {
        @Nullable LiveUpdateNotificationRenderer get();
    }

    private final RendererProvider rendererProvider;

    public LiveUpdatePushHandler(@NonNull RendererProvider rendererProvider) {
        this.rendererProvider = rendererProvider;
    }

    /**
     * Handles one incoming push.
     * <p>
     * Returns {@code false} for non-live-update pushes so they fall through to the normal
     * notification path untouched. For a recognized live-update push it always returns {@code true}
     * to consume the push (preventing a duplicate default notification), regardless of whether
     * rendering succeeded — a malformed payload, a missing renderer, or a renderer that throws are
     * all logged and swallowed.
     *
     * @param pushBundle the raw push payload
     * @return {@code true} if the push was a live update and has been consumed; {@code false} otherwise
     */
    @SuppressLint("NewApi")
    @Override
    @WorkerThread
    public boolean preHandleMessage(@Nullable Bundle pushBundle) {
        PWLog.noise(TAG, "preHandleMessage()");
        if (!LiveUpdateStateParser.isLiveUpdatePush(pushBundle)) {
            return false;
        }

        try {
            LiveUpdateState state = LiveUpdateStateParser.parse(pushBundle);
            if (state == null) {
                PWLog.error(TAG, "malformed live-update payload, dropping");
                return true; // eat the push so it doesn't fall through to the default notification path
            }
            PWLog.debug(TAG, "live-update push id=" + state.getActivityId() + " op=" + state.getOperation());

            LiveUpdateNotificationRenderer renderer = rendererProvider.get();
            if (renderer == null) {
                PWLog.warn(TAG, "no renderer installed, dropping live-update push");
                return true;
            }

            switch (state.getOperation()) {
                case START:
                case UPDATE:
                    renderer.render(state);
                    break;
                case END:
                    renderer.dismiss(state.getActivityId());
                    break;
                default:
                    PWLog.warn(TAG, "unhandled live-update operation: " + state.getOperation());
                    break;
            }
        } catch (Throwable t) {
            PWLog.error(TAG, "live-update handling failed, dropping push", t);
        }
        return true;
    }
}
