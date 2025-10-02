//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setEmailTags](set-email-tags.md)

# setEmailTags

[main]\
open fun [setEmailTags](set-email-tags.md)(emailTags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch.  Example: 

```kotlin

  pushwoosh.setEmailTags(Tags.intTag("intTag", 42), "my@email.com");

```

#### Parameters

main

| | |
|---|---|
| emailTags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |
| email | user email |

[main]\
open fun [setEmailTags](set-email-tags.md)(emailTags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), PushwooshException&gt;)

Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch.  Example: 

```kotlin

  List<String> emails = new ArrayList<>();
  emails.add("my@email.com");
  pushwoosh.setEmailTags(Tags.intTag("intTag", 42), emails, (result) -> {
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
| emailTags | [application tags bundle](../../com.pushwoosh.tags/-tags-bundle/index.md) |
| email | user email |
| callback | sendEmailTags operation callback |
