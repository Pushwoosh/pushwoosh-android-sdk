package com.pushwoosh.inapp.inline;

import androidx.annotation.Nullable;

/** Single-slot holder for the {@link InlineContentHandler}; null means the inline module is not connected. */
public final class InlineContentHandlerProvider {
    private static volatile InlineContentHandler handler;

    private InlineContentHandlerProvider() {}

    public static void set(@Nullable InlineContentHandler value) {
        handler = value;
    }

    @Nullable public static InlineContentHandler get() {
        return handler;
    }
}
