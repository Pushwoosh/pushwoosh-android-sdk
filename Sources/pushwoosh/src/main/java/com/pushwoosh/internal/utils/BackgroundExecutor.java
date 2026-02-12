package com.pushwoosh.internal.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Thread-safe utility class for executing tasks on background threads.
 * Replaces deprecated AsyncTask usage throughout the SDK.
 */
public final class BackgroundExecutor {
    private static final String TAG = "BackgroundExecutor";

    // Thread-safe initialization guaranteed by JVM (JLS §12.4.2)
    private static final Executor NETWORK = Executors.newSingleThreadExecutor();
    private static final Executor SERIAL = Executors.newSingleThreadExecutor();
    private static final Executor PARALLEL = Executors.newFixedThreadPool(4);

    // Lazy initialization via holder idiom - Handler created only on first main() call
    private static class MainHandlerHolder {
        static final Handler INSTANCE = new Handler(Looper.getMainLooper());
    }

    private BackgroundExecutor() {}

    /**
     * Execute task on dedicated network thread.
     * HTTP requests run sequentially to avoid race conditions.
     */
    public static void network(Runnable task) {
        NETWORK.execute(wrapWithErrorHandling(task));
    }

    /**
     * Execute task on serial executor.
     * Tasks run sequentially in FIFO order.
     */
    public static void execute(Runnable task) {
        SERIAL.execute(wrapWithErrorHandling(task));
    }

    /**
     * Execute task on parallel executor.
     * Tasks run concurrently on cached thread pool.
     */
    public static void executeOnPool(Runnable task) {
        PARALLEL.execute(wrapWithErrorHandling(task));
    }

    /**
     * Post task to main thread.
     * Use for callbacks that may update UI.
     */
    public static void main(Runnable task) {
        MainHandlerHolder.INSTANCE.post(task);
    }

    private static Runnable wrapWithErrorHandling(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                PWLog.error(TAG, "Uncaught exception in background task", t);
            }
        };
    }
}
