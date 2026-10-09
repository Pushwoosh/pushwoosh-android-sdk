package com.pushwoosh.featureflags;

/**
 * Notified on the main thread after a refresh changed at least one flag
 * ({@code enabled}, {@code rule_id} or {@code properties}), or added or removed a flag.
 */
public interface FeatureFlagsUpdateListener {
    void onFeatureFlagsUpdated();
}
