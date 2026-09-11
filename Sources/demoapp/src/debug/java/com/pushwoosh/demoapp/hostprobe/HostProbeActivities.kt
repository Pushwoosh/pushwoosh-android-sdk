package com.pushwoosh.demoapp.hostprobe

import android.app.Activity

/**
 * Empty activities whose only distinguishing feature is the theme assigned in the debug manifest
 * (src/debug/AndroidManifest.xml). They exercise the host-activity rule of SDK-961 against the real
 * framework.
 */
class OpaqueLightActivity : Activity()

class OpaqueDarkActivity : Activity()

class TranslucentActivity : Activity()

class DialogDarkActivity : Activity()

class NoAttrThemeActivity : Activity()

class TranslucentLightActivity : Activity()

class FloatingLightActivity : Activity()

/** Inherits a translucent theme but overrides the attribute back to opaque. */
class OpaqueOverrideActivity : Activity()

/** Dark palette, but the theme claims isLightTheme=true. */
class DarkPaletteLightFlagActivity : Activity()

/**
 * Declared in a separate process without the SDK's InitProvider, so it never reaches this process's
 * lifecycle callbacks.
 */
class RemoteProcessActivity : Activity()

/** Finishes itself before it is ever resumed. */
class FinishInOnCreateActivity : Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}

/** Opaque light activity living in its own task. */
class OwnTaskActivity : Activity()

/** Handles uiMode changes itself, so the system never recreates it on a theme switch. */
class ConfigChangesActivity : Activity()

/** isLightTheme comes from a bool resource reference rather than a literal. */
class ReferenceFlagActivity : Activity()
