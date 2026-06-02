package com.pushwoosh.test.manifest;

import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;

import java.util.Collection;
import java.util.Collections;

public class FakeNoCtorPlugin implements Plugin {
    @SuppressWarnings("unused")
    public FakeNoCtorPlugin(int unused) {}

    @Override
    public void init() {}

    @Override
    public Collection<? extends MigrationScheme> getPrefsMigrationSchemes(PrefsProvider prefsProvider) {
        return Collections.emptyList();
    }
}
