# Module pushwoosh-inbox-ui

Provides ready-made UI components for displaying the Pushwoosh message inbox inside your app,
so you can surface a message center without building list screens from scratch. Add this
dependency when you want a Fragment or a standalone Activity that renders the inbox messages
delivered by Pushwoosh and handles read/delete interactions out of the box. The primary entry
point for obtaining the inbox UI is [PushwooshInboxUi].

# Package com.pushwoosh.inbox.ui

Entry point and visual configuration for the inbox UI.

[PushwooshInboxUi] is the main entry point: it creates the inbox Fragment and lets you
register an [OnInboxMessageClickListener] that is called whenever the user taps a message.

[PushwooshInboxStyle] is a global style object that controls every visual aspect of the inbox
list — colors, fonts, text sizes, item animation, empty-state and error-state images, toolbar
visibility, and the date formatter — before the Fragment is shown.

[OnInboxMessageClickListener] is a single-method interface you implement to receive a callback
with the tapped [InboxMessage] when the user selects an item in the list.

# Package com.pushwoosh.inbox.ui.presentation.view.activity

Standalone Activity that hosts the inbox as a full screen.

[InboxActivity] is an `AppCompatActivity` that embeds the inbox Fragment and applies the
colors from [PushwooshInboxStyle] to the status bar, navigation bar, and content background,
providing a self-contained inbox screen you can launch directly without building a host Activity.

# Package com.pushwoosh.inbox.ui.model.customizing.formatter

Customization contract for inbox message timestamps.

[InboxDateFormatter] is an interface that converts a `Date` into a display string; assign a
custom implementation to [PushwooshInboxStyle.dateFormatter] to replace the default
`MMM dd` format with any locale or pattern your app requires.
