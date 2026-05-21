package com.pushwoosh.test.manifest;

import com.pushwoosh.internal.PluginProvider;

public class FakePluginProvider implements PluginProvider {
    public FakePluginProvider() {}

    @Override
    public String getPluginType() {
        return "fake";
    }

    @Override
    public int richMediaStartDelay() {
        return DEFAULT_RICH_MEDIA_START_DELAY;
    }
}
