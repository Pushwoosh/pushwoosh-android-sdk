//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[putAll](put-all.md)

# putAll

[main]\
open fun [putAll](put-all.md)(json: JSONObject): [TagsBundle.Builder](index.md)

Imports all tags from a JSON object, adding them to the builder. 

 This method extracts all key-value pairs from the provided JSON object and adds them as tags. This is useful for bulk importing tags from API responses, local storage, or configuration files. Existing tags with the same keys will be overwritten. 

Example (Importing from API Response):

```kotlin

		// Received user profile from backend API
		JSONObject userProfile = apiResponse.getJSONObject("profile");
		// {
		//   "name": "Jane Smith",
		//   "age": 32,
		//   "premium": true,
		//   "interests": ["music", "travel", "photography"]
		// }
		
		TagsBundle tags = new TagsBundle.Builder()
		    .putAll(userProfile) // Import all fields from JSON
		    .putString("sync_source", "api") // Add additional tags
		    .putLong("last_sync", System.currentTimeMillis())
		    .build();
		
		Pushwoosh.getInstance().sendTags(tags);
		
```

Note: All JSON value types (strings, numbers, booleans, arrays, nested objects) are supported and automatically converted to appropriate tag types.

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| json | JSON object containing tag name-value pairs to import |
