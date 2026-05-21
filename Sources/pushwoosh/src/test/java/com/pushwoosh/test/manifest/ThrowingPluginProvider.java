package com.pushwoosh.test.manifest;

import com.pushwoosh.internal.PluginProvider;

public class ThrowingPluginProvider implements PluginProvider {
    public ThrowingPluginProvider() {
        throw new RuntimeException("ThrowingPluginProvider ctor fails on purpose");
    }

    @Override
    public String getPluginType() {
        return "fake";
    }

    @Override
    public int richMediaStartDelay() {
        return DEFAULT_RICH_MEDIA_START_DELAY;
    }
}
