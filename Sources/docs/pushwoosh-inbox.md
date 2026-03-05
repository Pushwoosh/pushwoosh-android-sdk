# Module pushwoosh-inbox

Provides a persistent message inbox that stores push notifications delivered by Pushwoosh,
letting users revisit them after dismissing the system tray alert. Add this dependency when
you want to surface a message center inside your app where users can read, act on, and delete
received pushes. The primary entry point for loading and managing inbox messages is [PushwooshInbox].

# Package com.pushwoosh.inbox

Entry point for all inbox operations.

[PushwooshInbox] loads the full message list from the network or the local cache, exposes
paginated access, and provides methods to mark messages as read, trigger their action, or
delete them. It also lets you register count observers that are notified whenever the number
of unread or unacted-on messages changes.

# Package com.pushwoosh.inbox.data

Data types that represent a single inbox entry.

[InboxMessage] is the read-only interface for a message — it exposes the title, body, image
URL, send date, and action metadata, as well as flags indicating whether the message has been
read or its action has been performed.

[InboxMessageType] enumerates the four kinds of action that a message can carry: plain (no
action), Rich Media page, remote URL, or deep link.

# Package com.pushwoosh.inbox.event

EventBus event fired when the local inbox state changes.

[InboxMessagesUpdatedEvent] carries three collections — newly added messages, updated messages,
and the codes of deleted messages — so subscribers can incrementally refresh any inbox UI
without reloading the full list.

# Package com.pushwoosh.inbox.exception

Exception type surfaced through inbox callbacks.

[InboxMessagesException] is thrown or delivered to error callbacks when a network or storage
failure prevents an inbox operation from completing.
