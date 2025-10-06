//[pushwoosh-inbox](../../../index.md)/[com.pushwoosh.inbox](../index.md)/[PushwooshInbox](index.md)/[loadCachedMessages](load-cached-messages.md)

# loadCachedMessages

[main]\
open fun [loadCachedMessages](load-cached-messages.md)(inboxMessage: [InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md), limit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Collection](https://developer.android.com/reference/kotlin/java/util/Collection.html)&lt;[InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md)&gt;

Get the collection of the [com.pushwoosh.inbox.data.InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md) that the user received. This method obtains messages synchronously from local database

#### Parameters

main

| | |
|---|---|
| inboxMessage | - This parameter provides pagination. Pass the last [com.pushwoosh.inbox.data.InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md) that is on your current page as a parameter to get previous messages.To get the latest messages or in case the pagination is not necessary, pass null as a parameter. |
| limit | - amount of messages to get. Pass -1 to get all the messages |

[main]\
open fun [loadCachedMessages](load-cached-messages.md)(callback: Callback&lt;Collection&lt;InboxMessage&gt;, InboxMessagesException&gt;, inboxMessage: [InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md), limit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Get the collection of the [com.pushwoosh.inbox.data.InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md) that the user received. This method obtains messages asynchronously from local database

#### Parameters

main

| | |
|---|---|
| callback | - if successful, return the collection of the InboxMessages. Otherwise, return error |
| inboxMessage | - This parameter provides pagination. Pass the last [com.pushwoosh.inbox.data.InboxMessage](../../com.pushwoosh.inbox.data/-inbox-message/index.md) that is on your current page as a parameter to get previous messages.To get the latest messages or in case the pagination is not necessary, pass null as a parameter. |
| limit | - amount of messages to get. Pass -1 to get all the messages |
