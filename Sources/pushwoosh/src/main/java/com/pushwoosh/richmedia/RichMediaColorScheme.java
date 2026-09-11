package com.pushwoosh.richmedia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Color scheme the SDK reports to Rich Media content. It drives the
 * {@code prefers-color-scheme} CSS media query inside the Rich Media WebView.
 * <p>
 * Configured via {@link RichMediaManager#setRichMediaColorScheme(RichMediaColorScheme)}
 * or the {@code com.pushwoosh.rich_media_color_scheme} meta-data tag in AndroidManifest.xml.
 */
public enum RichMediaColorScheme {
    /**
     * Follows the application theme: dark when the current activity theme declares
     * {@code android:isLightTheme="false"}; without a visible activity, the manifest
     * application theme is used. Default.
     */
    APP,
    /** Follows the system dark mode setting, ignoring the application theme. */
    SYSTEM,
    /** Rich Media content is always rendered with the light scheme. */
    LIGHT,
    /** Rich Media content is always rendered with the dark scheme. */
    DARK;

    /**
     * Parses a color scheme name, case-insensitive.
     *
     * @param input scheme name: "app", "system", "light" or "dark"
     * @return matching scheme, or {@link #APP} when the input is unknown or null
     */
    @NonNull public static RichMediaColorScheme fromString(@Nullable String input) {
        for (RichMediaColorScheme scheme : values()) {
            if (scheme.name().equalsIgnoreCase(input)) {
                return scheme;
            }
        }
        return APP;
    }
}
