package com.pushwoosh.liveupdates;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.liveupdates.internal.DefaultProgressStyleProvider;
import com.pushwoosh.liveupdates.internal.LiveUpdateNotificationRenderer;
import com.pushwoosh.liveupdates.internal.LiveUpdatePushHandler;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;

import java.util.Collection;
import java.util.Collections;

/**
 * Auto-discovered {@link Plugin} that wires the Live Updates module into the SDK.
 * <p>
 * Discovered at startup via the manifest meta-data {@code com.pushwoosh.plugin.live_updates}.
 * On {@link #init()} it gates on API 36 (Live Updates are unavailable below Android 16), resolves
 * the {@link LiveUpdateProgressStyleProvider} the integrator declared in the manifest (falling
 * back to {@link DefaultProgressStyleProvider}), installs the SDK-owned
 * {@link LiveUpdateNotificationRenderer} into {@link PushwooshLiveUpdates}, and registers a
 * {@link LiveUpdatePushHandler} on the system message chain so live-update pushes are intercepted
 * before the default notification path.
 */
public class LiveUpdatesPlugin implements Plugin {

    private static final String TAG = "LiveUpdatesPlugin";
    private static final String META_STYLE_PROVIDER = "com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER";

    /**
     * Activates the module: gates on API 36+, then installs the renderer and registers the
     * live-update push handler. A no-op below API 36.
     */
    @Override
    public void init() {
        PWLog.info(TAG, "init() entry; SDK_INT=" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < 36) {
            PWLog.warn(TAG, "Live Updates require API 36+, plugin disabled");
            return;
        }

        LiveUpdateProgressStyleProvider provider = resolveStyleProvider();
        PushwooshLiveUpdates.install(new LiveUpdateNotificationRenderer(provider));

        MessageSystemHandleChainProvider.getMessageSystemChain()
                .addItem(new LiveUpdatePushHandler(PushwooshLiveUpdates::getActiveRenderer));
        PWLog.info(TAG, "init() done; handler registered");
    }

    /** The module stores no preferences, so it contributes no migration schemes. */
    @Override
    public Collection<? extends MigrationScheme> getPrefsMigrationSchemes(PrefsProvider prefsProvider) {
        return Collections.emptyList();
    }

    /**
     * Resolves the style provider declared via the {@code com.pushwoosh.LIVE_UPDATE_STYLE_PROVIDER}
     * manifest meta-data, instantiating it reflectively through its public no-arg constructor.
     * Falls back to {@link DefaultProgressStyleProvider} when the key is absent, the class is
     * missing, does not implement {@link LiveUpdateProgressStyleProvider}, or cannot be
     * instantiated — every failure mode degrades gracefully with a warning rather than throwing.
     */
    @RequiresApi(36)
    private LiveUpdateProgressStyleProvider resolveStyleProvider() {
        try {
            Context ctx = AndroidPlatformModule.getApplicationContext();
            if (ctx == null) return new DefaultProgressStyleProvider();
            Bundle meta = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            if (meta == null) return new DefaultProgressStyleProvider();

            String fqn = resolveClassName(meta.getString(META_STYLE_PROVIDER), ctx.getPackageName());
            if (fqn == null) return new DefaultProgressStyleProvider();

            Object instance = Class.forName(fqn).getDeclaredConstructor().newInstance();
            if (instance instanceof LiveUpdateProgressStyleProvider) {
                PWLog.info(TAG, "Custom LiveUpdateProgressStyleProvider installed: " + fqn);
                return (LiveUpdateProgressStyleProvider) instance;
            }
            PWLog.warn(TAG, fqn + " is not a LiveUpdateProgressStyleProvider, falling back to default");
        } catch (Throwable t) {
            PWLog.warn(TAG, "failed to instantiate custom style provider, using default: " + t.getMessage());
        }
        return new DefaultProgressStyleProvider();
    }

    /**
     * Resolves the leading-dot shorthand the same way {@code AndroidManifestConfig} does for every
     * other manifest class reference: {@code ".MyProvider"} expands to {@code <packageName>.MyProvider}.
     * Without this the {@code ManifestValidator} (which resolves the dot) accepts the value while the
     * runtime quietly falls back to the default. Returns null for an absent or blank value.
     */
    @VisibleForTesting
    @Nullable static String resolveClassName(@Nullable String raw, @NonNull String packageName) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.startsWith(".") ? packageName + trimmed : trimmed;
    }
}
