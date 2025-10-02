//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setUser](set-user.md)

# setUser

[main]\
open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)

Set User identifier and register emails associated to the user. UserId could be Facebook ID or any other user ID. This allows data and events to be matched across multiple user devices.

#### Parameters

main

| | |
|---|---|
| userId | user identifier |
| emails | user's emails array list |

[main]\
open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), SetUserException&gt;)

Set User identifier and register emails associated to the user. UserId could be Facebook ID or any other user ID. This allows data and events to be matched across multiple user devices.

#### Parameters

main

| | |
|---|---|
| userId | user identifier |
| emails | user's emails array list |
| callback | setUser operation callback |
