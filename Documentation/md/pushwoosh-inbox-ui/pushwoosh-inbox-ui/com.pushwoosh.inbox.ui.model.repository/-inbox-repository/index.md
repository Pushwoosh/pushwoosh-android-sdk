//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui.model.repository](../index.md)/[InboxRepository](index.md)

# InboxRepository

[androidJvm]\
object [InboxRepository](index.md)

## Functions

| Name | Summary |
|---|---|
| [addCallback](add-callback.md) | [androidJvm]<br>fun [addCallback](add-callback.md)(callback: ([InboxEvent](../-inbox-event/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) |
| [loadCachedInbox](load-cached-inbox.md) | [androidJvm]<br>fun [loadCachedInbox](load-cached-inbox.md)(inboxMessage: InboxMessage?, limit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt; |
| [loadCachedInboxAsync](load-cached-inbox-async.md) | [androidJvm]<br>fun [loadCachedInboxAsync](load-cached-inbox-async.md)(inboxMessage: InboxMessage?, limit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [loadInbox](load-inbox.md) | [androidJvm]<br>fun [loadInbox](load-inbox.md)(forceRequest: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), inboxMessage: InboxMessage?, limit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [removeCallback](remove-callback.md) | [androidJvm]<br>fun [removeCallback](remove-callback.md)(callback: ([InboxEvent](../-inbox-event/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) |
| [removeItem](remove-item.md) | [androidJvm]<br>fun [removeItem](remove-item.md)(inboxMessage: InboxMessage) |
| [subscribeToEvent](subscribe-to-event.md) | [androidJvm]<br>fun [subscribeToEvent](subscribe-to-event.md)(): Subscription&lt;InboxMessagesUpdatedEvent&gt; |
