//[pushwoosh](../../../index.md)/[com.pushwoosh.inapp](../index.md)/[InAppManager](index.md)/[postEvent](post-event.md)

# postEvent

[main]\
open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), attributes: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

[postEvent](post-event.md)

[main]\
open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), attributes: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PostEventException&gt;)

Post events for In-App Messages. This can trigger In-App message HTML as specified in Pushwoosh Control Panel.

#### Parameters

main

| | |
|---|---|
| event | name of the event |
| attributes | additional event attributes |
| callback | method completion callback |
