package com.pushwoosh.liveupdates;

import android.app.Notification;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

/**
 * The single customization point for Pushwoosh Live Updates: supply the
 * {@link Notification.ProgressStyle} used to render a live-update notification.
 *
 * <p>The SDK owns everything else — channel registration, the {@link Notification.Builder}, the
 * ongoing flag, promoted-ongoing extras, large-icon download, header-time wiring, action mapping
 * and the final {@code NotificationManager.notify()}. A custom provider can only shape the
 * progress bar; it cannot break promoted-ongoing eligibility or skip channel setup. If a provider
 * throws, the SDK falls back to the default style and the notification still posts.
 *
 * <p>Register an implementation via manifest meta-data:
 * <pre>{@code
 * <meta-data
 *     android:name="com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER"
 *     android:value="com.example.MyStyleProvider" />
 * }</pre>
 * The class must have a public no-argument constructor; it is instantiated once via reflection
 * during SDK initialization.
 *
 * <p><b>Example</b> — map the payload's segments onto the bar and mark each phase boundary with a
 * point:
 * <pre>{@code
 * public class OrderStyleProvider implements LiveUpdateProgressStyleProvider {
 *     @NonNull
 *     @Override
 *     public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
 *         Notification.ProgressStyle style = new Notification.ProgressStyle();
 *         if (state.getProgress() != null) {
 *             style.setProgress(state.getProgress());
 *         }
 *         style.setProgressIndeterminate(state.isProgressIndeterminate());
 *
 *         List<LiveUpdateSegment> segments = state.getSegments();
 *         int boundary = 0;
 *         for (int i = 0; i < segments.size(); i++) {
 *             LiveUpdateSegment seg = segments.get(i);
 *             style.addProgressSegment(
 *                     new Notification.ProgressStyle.Segment(seg.getLength()).setColor(seg.getColor()));
 *             boundary += seg.getLength();
 *             if (i < segments.size() - 1) {
 *                 style.addProgressPoint(new Notification.ProgressStyle.Point(boundary));
 *             }
 *         }
 *         return style;
 *     }
 * }
 * }</pre>
 *
 * <p><b>Threading &amp; per-push contract.</b> {@link #createStyle(LiveUpdateState)} is invoked
 * once per rendered push — on every {@code start} and every {@code update} (an {@code end}
 * dismisses and does not call it) — on a worker thread. The provider is a single instance per
 * process, so implementations <b>must be stateless</b>: derive the returned style only from the
 * supplied {@code state} and keep no mutable state between calls. Vary output using
 * {@link LiveUpdateState#getProgress()}, {@link LiveUpdateState#getSegments()},
 * {@link LiveUpdateState#isProgressIndeterminate()}, {@link LiveUpdateState#getOperation()} and
 * the arbitrary payload JSON in {@link LiveUpdateState#getExtras()}.
 */
public interface LiveUpdateProgressStyleProvider {
    /**
     * Builds the {@link Notification.ProgressStyle} for one rendered push. Invoked once per
     * {@code start} / {@code update} on a worker thread; must be stateless (see the type-level
     * documentation for the full threading and per-push contract).
     *
     * @param state the parsed state of the push being rendered; the sole source of input
     * @return the progress style to apply; if this throws, the SDK falls back to the default style
     */
    @WorkerThread
    @RequiresApi(36)
    @NonNull Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state);
}
