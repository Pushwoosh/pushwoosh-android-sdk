package com.pushwoosh.demoapp.liveupdate;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pushwoosh.liveupdates.LiveUpdateProgressStyleProvider;
import com.pushwoosh.liveupdates.LiveUpdateSegment;
import com.pushwoosh.liveupdates.LiveUpdateState;

import org.json.JSONObject;

import java.util.List;

/**
 * Demo {@link LiveUpdateProgressStyleProvider} styled as a "pizza on its way" delivery journey.
 * It shows off every {@link Notification.ProgressStyle} ingredient an integrator can reach:
 *
 * <ul>
 *   <li><b>Segments</b> — the colored phases of the order, taken from the push payload (the server
 *       owns the palette, so the bar reacts to per-push data);
 *   <li><b>Points</b> — neutral on-surface milestone squares on each internal phase boundary. The
 *       bar ends stay unmarked, matching the platform delivery example. The payload carries no
 *       points, so they are derived here from the segment lengths;
 *   <li><b>Icon</b> — a single phase-aware tracker that moves along the bar. No start/end icons:
 *       the platform delivery example keeps just the one moving marker.
 * </ul>
 *
 * <p>The bar keeps the platform default {@code setStyledByProgress(true)}: the reached part renders
 * thick, the part ahead stays thin, so progress is readable at a glance. The tracker emoji is driven
 * by the push: a {@code "phase"} value in {@link LiveUpdateState#getExtras()} ("cooking" / "delivery"
 * / "arriving") wins; otherwise it is derived from how far the bar advanced.
 *
 * <p>This is the intended extension shape — the style reacts to per-push data while the SDK still
 * owns the channel, ongoing flag and promoted-ongoing extras. Registered via manifest meta-data
 * ({@code com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER}).
 *
 * <p>Stateless — the returned style is derived only from {@code state}.
 */
@RequiresApi(36)
public class DemoLiveUpdateStyleProvider implements LiveUpdateProgressStyleProvider {

    // Milestone squares use the app's neutral on-surface ink (md_theme_onSurface): a crisp checkpoint
    // that reads cleanly over both the light notification shade and the purple segment colors.
    private static final int MILESTONE_COLOR = 0xFF1E1A20;

    @NonNull @Override
    public Notification.ProgressStyle createStyle(@NonNull LiveUpdateState state) {
        Notification.ProgressStyle style = new Notification.ProgressStyle();
        if (state.getProgress() != null) {
            style.setProgress(state.getProgress());
        }
        style.setProgressIndeterminate(state.isProgressIndeterminate());

        // Segments are the payload's phases; a milestone square marks each phase boundary except the
        // last (the finish is the house end-icon). Positions are the running sum of segment lengths.
        List<LiveUpdateSegment> segments = state.getSegments();
        int boundary = 0;
        for (int i = 0; i < segments.size(); i++) {
            LiveUpdateSegment seg = segments.get(i);
            style.addProgressSegment(new Notification.ProgressStyle.Segment(seg.getLength()).setColor(seg.getColor()));
            boundary += seg.getLength();
            if (i < segments.size() - 1) {
                style.addProgressPoint(new Notification.ProgressStyle.Point(boundary).setColor(MILESTONE_COLOR));
            }
        }

        style.setProgressTrackerIcon(emojiIcon(trackerEmoji(state)));

        return style;
    }

    @NonNull private static String trackerEmoji(@NonNull LiveUpdateState state) {
        JSONObject extras = state.getExtras();
        if (extras != null) {
            switch (extras.optString("phase")) {
                case "cooking":
                case "preparing":
                    return "🍕";
                case "delivery":
                case "on_the_way":
                    return "🚗";
                case "arriving":
                case "nearby":
                    return "🛎️";
            }
        }
        double fraction = progressFraction(state);
        if (fraction < 0.34) {
            return "🍕";
        }
        if (fraction < 0.7) {
            return "🚗";
        }
        return "🛎️";
    }

    private static double progressFraction(@NonNull LiveUpdateState state) {
        if (state.isProgressIndeterminate() || state.getProgress() == null) {
            return 0;
        }
        int max = 0;
        for (LiveUpdateSegment seg : state.getSegments()) {
            max += seg.getLength();
        }
        if (max <= 0) {
            max = 100;
        }
        return Math.max(0, Math.min(1, state.getProgress() / (double) max));
    }

    // A colored emoji has to be rasterized into a bitmap Icon: a monochrome vector via
    // createWithResource renders as a flat tinted silhouette and loses the color the demo wants.
    // The canvas is mirrored horizontally because Noto's vehicle glyphs face left: flipping makes
    // the moving tracker travel in the reading direction (left→right). The non-directional phases
    // (🍕 / 🛎️) are visually unaffected by the flip.
    @NonNull private static Icon emojiIcon(@NonNull String emoji) {
        int size = 128;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(-1f, 1f, size / 2f, size / 2f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size * 0.9f);
        paint.setTextAlign(Paint.Align.CENTER);
        float y = size / 2f - (paint.descent() + paint.ascent()) / 2f;
        canvas.drawText(emoji, size / 2f, y, paint);
        return Icon.createWithBitmap(bitmap);
    }
}
