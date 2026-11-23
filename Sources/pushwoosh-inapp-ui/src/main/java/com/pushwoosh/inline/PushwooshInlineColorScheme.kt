package com.pushwoosh.inline

/**
 * Which theme inline banners are drawn in.
 */
enum class PushwooshInlineColorScheme {
    /** Follow the theme of the view hosting the slot. */
    APP,

    /** Follow the system night mode. */
    SYSTEM,

    /** Always light. */
    LIGHT,

    /** Always dark. */
    DARK,
}
