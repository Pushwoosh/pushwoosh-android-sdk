package com.pushwoosh.inapp.network;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class InAppExecutorServiceHelper {
    public static ExecutorService createExecutorService() {
        return new ExecutorService() {
            @Override public <T> Future<T> submit(Callable<T> task) {
                try {
                    T v = task.call();
                    return CompletableFuture.completedFuture(v);
                } catch (Exception e) {
                    CompletableFuture<T> f = new CompletableFuture<>();
                    f.completeExceptionally(e);
                    return f;
                }
            }
            @Override public Future<?> submit(Runnable task) {
                task.run();
                return CompletableFuture.completedFuture(null);
            }
            @Override public <T> Future<T> submit(Runnable task, T result) {
                task.run();
                return CompletableFuture.completedFuture(result);
            }
            @Override public void execute(Runnable command) { command.run(); }
            @Override public void shutdown() {}
            @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
            @Override public boolean isShutdown() { return false; }
            @Override public boolean isTerminated() { return false; }
            @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
            @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
            @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
            @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
            @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        };
    }
}
