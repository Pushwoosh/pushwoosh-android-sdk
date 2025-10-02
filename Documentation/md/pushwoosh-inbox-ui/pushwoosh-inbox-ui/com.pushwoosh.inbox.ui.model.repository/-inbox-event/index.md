//[pushwoosh-inbox-ui](../../../index.md)/[com.pushwoosh.inbox.ui.model.repository](../index.md)/[InboxEvent](index.md)

# InboxEvent

sealed class [InboxEvent](index.md)

#### Inheritors

| |
|---|
| [OnCreate](-on-create/index.md) |
| [Loading](-loading/index.md) |
| [FinishLoading](-finish-loading/index.md) |
| [FailedLoading](-failed-loading/index.md) |
| [SuccessLoadingCache](-success-loading-cache/index.md) |
| [SuccessLoading](-success-loading/index.md) |
| [InboxEmpty](-inbox-empty/index.md) |
| [InboxMessagesUpdated](-inbox-messages-updated/index.md) |
| [RestoreState](-restore-state/index.md) |

## Types

| Name | Summary |
|---|---|
| [FailedLoading](-failed-loading/index.md) | [androidJvm]<br>class [FailedLoading](-failed-loading/index.md)(val error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) : [InboxEvent](index.md) |
| [FinishLoading](-finish-loading/index.md) | [androidJvm]<br>class [FinishLoading](-finish-loading/index.md) : [InboxEvent](index.md) |
| [InboxEmpty](-inbox-empty/index.md) | [androidJvm]<br>class [InboxEmpty](-inbox-empty/index.md) : [InboxEvent](index.md) |
| [InboxMessagesUpdated](-inbox-messages-updated/index.md) | [androidJvm]<br>class [InboxMessagesUpdated](-inbox-messages-updated/index.md)(val addedInboxMessages: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;, val updatedInboxMessages: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;, val deleted: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;) : [InboxEvent](index.md) |
| [Loading](-loading/index.md) | [androidJvm]<br>class [Loading](-loading/index.md) : [InboxEvent](index.md) |
| [OnCreate](-on-create/index.md) | [androidJvm]<br>class [OnCreate](-on-create/index.md) : [InboxEvent](index.md) |
| [RestoreState](-restore-state/index.md) | [androidJvm]<br>class [RestoreState](-restore-state/index.md) : [InboxEvent](index.md) |
| [SuccessLoading](-success-loading/index.md) | [androidJvm]<br>class [SuccessLoading](-success-loading/index.md)(val inboxMessages: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;) : [InboxEvent](index.md) |
| [SuccessLoadingCache](-success-loading-cache/index.md) | [androidJvm]<br>class [SuccessLoadingCache](-success-loading-cache/index.md)(val inboxMessages: [Collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;InboxMessage&gt;) : [InboxEvent](index.md) |
