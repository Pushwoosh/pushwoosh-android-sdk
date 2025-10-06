//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[toJson](to-json.md)

# toJson

[main]\
open fun [toJson](to-json.md)(): JSONObject

Converts the TagsBundle to its JSON representation. 

 Returns a JSONObject containing all tags in the bundle. This method is primarily used internally by the Pushwoosh SDK when sending tags to the server, but can also be useful for logging, debugging, or integrating with other APIs that accept JSON. 

Example (API Integration &Debugging):

```kotlin

	TagsBundle tags = new TagsBundle.Builder()
	    .putString("name", "Alice")
	    .putInt("age", 28)
	    .putBoolean("premium", true)
	    .putList("interests", Arrays.asList("sports", "music"))
	    .build();
	
	// Convert to JSON for API call
	JSONObject json = tags.toJson();
	// Result: {"name":"Alice","age":28,"premium":true,"interests":["sports","music"]}
	
	// Send to custom backend
	sendToBackend(json.toString());
	
	// Log for debugging
	Log.d("Tags", "User tags: " + json.toString());
	
```

#### Return

non-null JSONObject containing all tags in the bundle
