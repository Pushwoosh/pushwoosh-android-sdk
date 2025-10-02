//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setTags](set-tags.md)

# setTags

[main]\
open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))

Associates device with given tags. If setTags request fails tags will be resent on the next application launch.  Example: 

```kotlin

  pushwoosh.setTags(Tags.intTag("intTag", 42));

```

#### Parameters

main

| | |
|---|---|
| tags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |

[main]\
open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PushwooshException&gt;)

Associates device with given tags. If setTags request fails tags will be resent on the next application launch.  Example: 

```kotlin

  pushwoosh.setTags(Tags.intTag("intTag", 42), (result) -> {
      if (result.isSuccess()) {
          // tags sucessfully sent
      }
      else {
          // failed to send tags
      }
  });

```

#### Parameters

main

| | |
|---|---|
| tags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |
| callback | sendTags operation callback |
