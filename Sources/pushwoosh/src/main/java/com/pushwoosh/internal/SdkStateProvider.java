package com.pushwoosh.internal;

import com.pushwoosh.internal.utils.PWLog;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SdkStateProvider {
    private static final String TAG = SdkStateProvider.class.getSimpleName();
    private static final SdkStateProvider INSTANCE = new SdkStateProvider();

    public enum SdkState {
        INITIALIZING,
        READY,
        ERROR
    }

    private volatile SdkState currentState = SdkState.INITIALIZING;
    
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();

    private SdkStateProvider() {
        // private constructor for singleton
    }

    public static SdkStateProvider getInstance() {
        return INSTANCE;
    }

    public void executeOrQueue(Runnable task) {
        PWLog.noise(TAG, "executeOrQueue() called with task: " + task);

        if (currentState == SdkState.READY) {
            task.run();
            return;
        }

        synchronized (lock) {
            if (currentState == SdkState.READY) {
                task.run();
                PWLog.noise(TAG, "SDK is ready, executing task immediately.");
            } else if (currentState == SdkState.INITIALIZING) {
                PWLog.noise(TAG, "SDK is initializing, adding task to the queue.");
                taskQueue.add(task);
            } else {
                PWLog.warn(TAG, "SDK is in ERROR state, task will be ignored.");
            }
        }
    }

    public void setReady() {
        PWLog.noise(TAG, "setReady()");

        Queue<Runnable> tasksToRun;
        synchronized (lock) {
            if (currentState != SdkState.INITIALIZING) {
                return;
            }
            tasksToRun = new ConcurrentLinkedQueue<>(taskQueue);
            taskQueue.clear();
            currentState = SdkState.READY;
        }

        PWLog.debug(TAG, "Executing " + tasksToRun.size() + " queued tasks.");
        for (Runnable task : tasksToRun) {
            try {
                task.run();
            } catch (Exception e) {
                PWLog.error(TAG, "Error executing queued task", e);
            }
        }
        PWLog.debug(TAG, "All queued tasks executed.");
    }

    public void setError() {
        PWLog.noise(TAG, "setError()");
        PWLog.error(TAG, "SDK state is changing to ERROR.");
        synchronized (lock) {
            currentState = SdkState.ERROR;
            taskQueue.clear();
        }
        PWLog.info(TAG, "SDK current state: " + currentState.toString());
    }

    public SdkState getCurrentState() {
        return currentState;
    }

    public boolean isReady() {
        return currentState == SdkState.READY;
    }

    public void resetForTesting() {
        PWLog.noise(TAG, "resetForTesting()");
        synchronized (lock) {
            currentState = SdkState.INITIALIZING;
            taskQueue.clear();
        }
    }
}
