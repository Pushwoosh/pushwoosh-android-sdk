//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[build](build.md)

# build

[main]\
open fun [build](build.md)(): [TagsBundle](../index.md)

Builds and returns an immutable [TagsBundle](../index.md) instance with all added tags. 

 This method finalizes the tag collection and creates an immutable TagsBundle that can be safely passed to sendTags or other methods. After calling build(), the Builder can still be reused to create additional TagsBundle instances, but changes to the Builder will not affect previously built instances. 

Usage Pattern:

```kotlin

		// Build a TagsBundle
		TagsBundle tags = new TagsBundle.Builder()
		    .putString("name", "John")
		    .putInt("age", 30)
		    .putBoolean("premium", true)
		    .build();
		
		// Send to Pushwoosh
		Pushwoosh.getInstance().sendTags(tags);
		
		// Read tags from the bundle
		String name = tags.getString("name"); // "John"
		int age = tags.getInt("age", 0); // 30
		
```

#### Return

immutable TagsBundle instance containing all added tags
