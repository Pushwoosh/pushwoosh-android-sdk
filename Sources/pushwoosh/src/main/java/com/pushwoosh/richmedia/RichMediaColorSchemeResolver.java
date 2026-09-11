package com.pushwoosh.richmedia;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.R;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Resolves the color scheme Rich Media reports to WebView content. For internal use.
 */
public class RichMediaColorSchemeResolver {
    private static final String TAG = "RichMediaColorSchemeResolver";

    private RichMediaColorSchemeResolver() {}

    @NonNull public static Context wrapForCurrentScheme(@NonNull Context base) {
        boolean dark = isCurrentSchemeDark(base);
        return new ContextThemeWrapper(
                base, dark ? R.style.PwRichMediaColorScheme_Dark : R.style.PwRichMediaColorScheme_Light);
    }

    /**
     * True when the configured {@link RichMediaColorScheme} resolves to dark for {@code context}.
     * Shared by the Rich Media WebView wrapper and the native in-app dark overlay (SDK-971).
     * For internal use.
     */
    public static boolean isCurrentSchemeDark(@NonNull Context context) {
        Activity host;
        if (context instanceof Activity && PushwooshPlatform.isHostCandidate((Activity) context)) {
            host = (Activity) context;
        } else {
            PushwooshPlatform platform = PushwooshPlatform.getInstance();
            host = platform == null ? null : platform.getHostActivity();
        }
        Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
        RichMediaColorScheme scheme = RichMediaManager.getRichMediaColorScheme();
        boolean dark = isDark(scheme, host, app);
        PWLog.debug(
                TAG,
                "colorScheme=" + scheme + " host="
                        + (host == null ? "null" : host.getClass().getSimpleName()) + " dark=" + dark);
        return dark;
    }

    static boolean isDark(@NonNull RichMediaColorScheme scheme, @Nullable Activity host, @NonNull Context app) {
        switch (scheme) {
            case LIGHT:
                return false;
            case DARK:
                return true;
            case SYSTEM:
                int uiMode = app.getResources().getConfiguration().uiMode;
                return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            case APP:
            default:
                Resources.Theme theme = appTheme(host, app);
                if (theme == null) {
                    return false;
                }
                TypedValue value = new TypedValue();
                if (!theme.resolveAttribute(android.R.attr.isLightTheme, value, true)) {
                    return false;
                }
                return value.data == 0;
        }
    }

    @Nullable private static Resources.Theme appTheme(@Nullable Activity host, @NonNull Context app) {
        if (host != null) {
            return host.getTheme();
        }
        int themeRes = app.getApplicationInfo().theme;
        if (themeRes == 0) {
            return null;
        }
        Resources.Theme theme = app.getResources().newTheme();
        theme.applyStyle(themeRes, true);
        return theme;
    }
}
