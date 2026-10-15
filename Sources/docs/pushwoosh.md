# Module pushwoosh

Core module of the Pushwoosh SDK. Handles push notification registration, user segmentation
via tags, in-app messaging, and rich media. Start here — all other modules depend on this one.

# Package com.pushwoosh

Main entry point of the SDK. Use [Pushwoosh] to register the device for push notifications,
manage user identity (User ID, email), set tags, and schedule local notifications.

# Package com.pushwoosh.notification

Push notification handling and customization. [PushMessage] carries all data about a received
notification. Extend [NotificationServiceExtension] to intercept notifications before display,
suppress them in foreground, or override launch behavior. Use [NotificationFactory] to build
fully custom notification layouts.

### Action buttons

The server can attach up to three action buttons to a push through the `pw_buttons` field. Each
button has an `id`, a `label` and an `action`:

| `action` | What a tap does |
|---|---|
| `open` | Brings the app forward exactly like a tap on the notification body |
| `url` / `deeplink` | Opens the button's `url` the way the push link does: an allowed external host goes to the system, a link your app resolves is delivered inside the app, otherwise the app is launched |
| `custom` | Delivers the tap to your code in the background without opening any screen |

The SDK renders the buttons itself — a custom [NotificationFactory] must not render
[PushMessage.buttons] again — and it owns the tap: the notification is dismissed, an open
event carrying the button id is reported, and [NotificationServiceExtension.onMessageOpened] is
called with a message whose [PushMessage.tappedButtonId] returns the tapped button's id. There
is no separate callback. Rich Media, the push link `l` and the `launch` key apply to a tap on the
notification body only.

A `custom` tap runs in a broadcast receiver: there is no activity, the budget is about ten
seconds, and Android 12+ forbids starting an activity from there.

The legacy Android-only `pw_actions` contract (`Action`, `PushMessage.getActions()`) is deprecated.
A push that carries `pw_buttons` never renders `pw_actions`.

# Package com.pushwoosh.inapp

In-app messaging. Use [InAppManager] to post events that trigger in-app message campaigns
configured in the Pushwoosh Control Panel.

# Package com.pushwoosh.inapp.view.config

Configuration classes for customizing how in-app messages are presented in the WebView.
Use [ModalRichmediaConfig] to set position, size, animation, and swipe behavior.

# Package com.pushwoosh.inapp.view.config.enums

Enums for [ModalRichmediaConfig] options: presentation and dismiss animation types
([ModalRichMediaPresentAnimationType], [ModalRichMediaDismissAnimationType]),
window position, width, and swipe gesture.

# Package com.pushwoosh.richmedia

Rich media pages (HTML pages delivered via push or in-app). Use [RichMediaManager] to control
when and how rich media is shown, or implement [RichMediaPresentingDelegate] to handle
presentation yourself. Customize appearance with [RichMediaStyle].

# Package com.pushwoosh.richmedia.animation

Animation presets for rich media presentation. Apply [RichMediaAnimation] subclasses
(slide, cross-fade) to [RichMediaStyle] to control enter and exit transitions.

# Package com.pushwoosh.tags

User attribute management for audience segmentation. Use [Tags] factory methods to create
single-tag instances quickly, or build a multi-attribute [TagsBundle] with [TagsBundle.Builder]
and send it via `Pushwoosh.getInstance().sendTags()`.

# Package com.pushwoosh.function

Async callback interfaces used across the SDK. [Callback] is invoked on the main thread
and receives a [Result] that wraps either a success value or an exception.

# Package com.pushwoosh.exception

Typed exceptions thrown by SDK operations. All extend [PushwooshException], so you can
catch them individually for specific error handling or handle the base class for a catch-all.
