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
