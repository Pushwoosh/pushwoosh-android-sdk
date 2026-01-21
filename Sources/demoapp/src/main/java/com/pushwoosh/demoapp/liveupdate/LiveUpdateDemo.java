package com.pushwoosh.demoapp.liveupdate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.pushwoosh.demoapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Demonstrates Live Update notifications with a simulated delivery tracking scenario.
 * <p>
 * This singleton class provides a ready-to-use demo that progresses through delivery
 * statuses (order confirmed → driver assigned → on the way → delivered) with visual
 * progress updates. Supports both manual and automatic progression modes.
 * <p>
 * Usage example:
 * <pre>{@code
 * LiveUpdateDemo demo = LiveUpdateDemo.getInstance(context);
 * demo.start(true);  // auto-progress every 3 seconds
 * // or
 * demo.start(false); // manual mode
 * demo.update();     // trigger next step manually
 * demo.finish();     // complete with success
 * demo.cancel();     // cancel and dismiss
 * }</pre>
 *
 * @see LiveUpdateNotificationManager
 */
public class LiveUpdateDemo {
    private static final String TAG = "LiveUpdateDemo";

    private static LiveUpdateDemo instance;

    private final LiveUpdateNotificationManager manager;
    private final Handler handler;

    private int updateStep = 0;
    private boolean autoMode = false;
    private long etaTimeMillis = 0;

    public static synchronized LiveUpdateDemo getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new LiveUpdateDemo(context);
        }
        return instance;
    }

    // Demo states for delivery simulation (used as notification title)
    private static final String[] STATUSES = {
            "Your order is being placed",
            "Preparing your order",
            "Driver assigned",
            "Order picked up",
            "Your order is on its way",
            "Driver is nearby",
            "Arriving now"
    };

    private static final int[] PROGRESS_VALUES = {
            10, 25, 40, 55, 70, 85, 95
    };

    private LiveUpdateDemo(@NonNull Context context) {
        Context context1 = context.getApplicationContext();
        this.manager = new LiveUpdateNotificationManager(context);
        this.handler = new Handler(Looper.getMainLooper());

        // Configure notification appearance
        int accentColor = ContextCompat.getColor(context1, R.color.md_theme_primary);
        Bitmap largeIcon = createEmojiBitmap("📦", 128);
        manager.setSmallIcon(R.drawable.ic_delivery)
                .setAccentColor(accentColor)
                .setLargeIcon(largeIcon);

        // Add action buttons
        Intent cancelAction = new Intent(context1, LiveUpdateActionReceiver.class);
        cancelAction.setAction(LiveUpdateActionReceiver.ACTION_CANCEL);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(
                context1,
                0,
                cancelAction,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent tipAction = new Intent(context1, LiveUpdateActionReceiver.class);
        tipAction.setAction(LiveUpdateActionReceiver.ACTION_TIP);
        PendingIntent tipIntent = PendingIntent.getBroadcast(
                context1,
                1,
                tipAction,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        manager.addAction(0, "Cancel", cancelIntent)
                .addAction(0, "Add Tip", tipIntent);
    }

    /**
     * Starts the Live Update demo.
     * <p>
     * Resets to step 0 and displays the initial "Order confirmed" notification.
     * In auto mode, progresses to the next step every 3 seconds until finished.
     *
     * @param autoProgress {@code true} to auto-advance every 3 seconds,
     *                     {@code false} for manual control via {@link #update()}
     */
    public void start(boolean autoProgress) {
        this.autoMode = autoProgress;
        this.updateStep = 0;

        // Calculate ETA (e.g., 15 minutes from now for demo)
        this.etaTimeMillis = System.currentTimeMillis() + (15 * 60 * 1000);

        Log.d(TAG, "Starting Live Update demo (autoProgress=" + autoProgress + ")");

        // Title = status (bold), contentText = order info with ETA
        manager.start(
                STATUSES[0],
                getOrderInfoText(),
                PROGRESS_VALUES[0]
        );

        if (autoProgress) {
            scheduleNextUpdate();
        }
    }

    /**
     * Advances to the next delivery status step.
     * <p>
     * Has no effect if the demo is not active. Automatically calls {@link #finish()}
     * when all steps are complete.
     */
    public void update() {
        if (!manager.isActive()) {
            Log.w(TAG, "Demo not active, call start() first");
            return;
        }

        updateStep++;

        if (updateStep >= STATUSES.length) {
            finish();
            return;
        }

        manager.update(
                STATUSES[updateStep],
                getOrderInfoText(),
                PROGRESS_VALUES[updateStep]
        );

        Log.d(TAG, "Update: step " + updateStep);
    }

    /**
     * Finishes the demo with a "Delivered!" success notification.
     * <p>
     * Stops auto-progress if enabled and shows a dismissible completion notification.
     */
    public void finish() {
        autoMode = false;
        handler.removeCallbacksAndMessages(null);

        manager.finish(
                "Your order is complete",
                "Pizza order • Delivered"
        );

        Log.d(TAG, "Demo finished");
    }

    /**
     * Cancels the demo and dismisses the notification.
     * <p>
     * Stops auto-progress if enabled and resets the step counter.
     */
    public void cancel() {
        autoMode = false;
        handler.removeCallbacksAndMessages(null);

        manager.cancel();
        updateStep = 0;

        Log.d(TAG, "Demo cancelled");
    }

    /**
     * Checks if the demo is currently active.
     *
     * @return {@code true} if a Live Update notification is showing
     */
    public boolean isRunning() {
        return manager.isActive();
    }

    /**
     * Returns the underlying notification manager for advanced use cases.
     *
     * @return the {@link LiveUpdateNotificationManager} instance
     */
    public LiveUpdateNotificationManager getManager() {
        return manager;
    }

    /**
     * Returns the current step index (0-based).
     *
     * @return current step number, 0 to {@link #getTotalSteps()} - 1
     */
    public int getCurrentStep() {
        return updateStep;
    }

    /**
     * Returns the total number of demo steps.
     *
     * @return number of delivery status steps (currently 7)
     */
    public int getTotalSteps() {
        return STATUSES.length;
    }

    private void scheduleNextUpdate() {
        if (!autoMode || !manager.isActive()) {
            return;
        }

        handler.postDelayed(() -> {
            if (autoMode && manager.isActive()) {
                update();
                if (manager.isActive()) {
                    scheduleNextUpdate();
                }
            }
        }, 3000); // 3 seconds between updates
    }

    private String getOrderInfoText() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String eta = timeFormat.format(new Date(etaTimeMillis));
        return "Pizza order • ETA " + eta;
    }

    private Bitmap createEmojiBitmap(String emoji, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(sizePx * 0.8f);
        paint.setTextAlign(Paint.Align.CENTER);
        float x = sizePx / 2f;
        float y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f;
        canvas.drawText(emoji, x, y, paint);
        return bitmap;
    }
}
