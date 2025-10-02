//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getTags](get-tags.md)

# getTags

[main]\
open fun [getTags](get-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), GetTagsException&gt;)

Gets tags associated with current device  Example: 

```kotlin

  pushwoosh.getTags((result) -> {
      if (result.isSuccess()) {
           // tags successfully received
           int intTag = result.getInt("intTag");
      }
      else {
          // failed to receive tags
      }
  });

```

#### Parameters

main

| | |
|---|---|
| callback | callback handler |
