//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setEmail](set-email.md)

# setEmail

[main]\
open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)

Register emails list associated to the current user.

#### Parameters

main

| | |
|---|---|
| emails | user's emails array list |

[main]\
open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), SetEmailException&gt;)

Register emails list associated to the current user.

#### Parameters

main

| | |
|---|---|
| emails | user's emails array list |
| callback | setEmail operation callback |

[main]\
open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Register email associated to the current user. Email should be a string and could not be null or empty.

#### Parameters

main

| | |
|---|---|
| email | user's email string |

[main]\
open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), SetEmailException&gt;)

Register email associated to the current user. Email should be a string and could not be null or empty.

#### Parameters

main

| | |
|---|---|
| email | user's email string |
| callback | setEmail operation callback |
