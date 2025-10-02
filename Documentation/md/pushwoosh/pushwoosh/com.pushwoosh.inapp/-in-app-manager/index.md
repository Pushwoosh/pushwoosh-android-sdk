//[pushwoosh](../../../index.md)/[com.pushwoosh.inapp](../index.md)/[InAppManager](index.md)

# InAppManager

open class [InAppManager](index.md)

InAppManager is responsible for In-App messaging functionality.

#### See also

| |
|---|
| &lt;a href=&quot;https://docs.pushwoosh.com/platform-docs/automation/behavior-based-messaging/in-app-messaging&quot;&gt;In-App Messaging&lt;/a&gt; |

## Properties

| Name | Summary |
|---|---|
| [instance](instance.md) | [main]<br>open val [instance](instance.md): [InAppManager](index.md) |

## Functions

| Name | Summary |
|---|---|
| [addJavascriptInterface](add-javascript-interface.md) | [main]<br>open fun [addJavascriptInterface](add-javascript-interface.md)(object: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Add JavaScript interface for In-Apps extension. |
| [postEvent](post-event.md) | [main]<br>open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), attributes: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))<br>[postEvent](post-event.md)<br>[main]<br>open fun [postEvent](post-event.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), attributes: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PostEventException&gt;)<br>Post events for In-App Messages. |
| [postEventInternal](post-event-internal.md) | [main]<br>open fun [postEventInternal](post-event-internal.md)(event: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), attributes: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md)) |
| [registerJavascriptInterface](register-javascript-interface.md) | [main]<br>open fun [registerJavascriptInterface](register-javascript-interface.md)(className: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Same as [addJavascriptInterface](add-javascript-interface.md) but uses class name instead of object |
| [reloadInApps](reload-in-apps.md) | [main]<br>open fun [reloadInApps](reload-in-apps.md)()<br>open fun [reloadInApps](reload-in-apps.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), ReloadInAppsException&gt;) |
| [removeJavascriptInterface](remove-javascript-interface.md) | [main]<br>open fun [removeJavascriptInterface](remove-javascript-interface.md)(name: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Removes object registered with [addJavascriptInterface](add-javascript-interface.md) |
