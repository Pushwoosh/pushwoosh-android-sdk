//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[mergeUserId](merge-user-id.md)

# mergeUserId

[main]\
open fun [mergeUserId](merge-user-id.md)(oldUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), newUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), doMerge: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), MergeUserException&gt;)

Move all event statistics from oldUserId to newUserId if doMerge is true. If doMerge is false all events for oldUserId are removed.

#### Parameters

main

| | |
|---|---|
| oldUserId | source user identifier |
| newUserId | destination user identifier |
| doMerge | merge/remove events for source user identifier |
| callback | method completion callback |
