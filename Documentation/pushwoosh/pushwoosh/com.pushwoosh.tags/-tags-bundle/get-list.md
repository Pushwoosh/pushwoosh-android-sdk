//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[TagsBundle](index.md)/[getList](get-list.md)

# getList

[main]\
open fun [getList](get-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;

Retrieves a list tag value by name, or null if not found. 

 Returns the list of strings associated with the given key. If the tag doesn't exist or is not a list type, returns null. This method handles both List and JSONArray types, automatically converting JSONArray to List<String>. Non-string elements in the list are ignored during conversion. Always check for null before iterating. 

Example (Content Personalization):

```kotlin

	TagsBundle tags = getUserTags();
	
	List<String> interests = tags.getList("interests");
	List<String> purchasedCategories = tags.getList("purchased_categories");
	List<String> wishlist = tags.getList("wishlist_ids");
	
	// Safe iteration with null check
	if (interests != null && !interests.isEmpty()) {
	    for (String interest : interests) {
	        recommendContentByInterest(interest);
	    }
	}
	
	// Check for specific value
	if (purchasedCategories != null && purchasedCategories.contains("electronics")) {
	    showElectronicsDeals();
	}
	
```

#### Return

list of strings, or null if tag is not found or not a list

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;interests&quot;, &quot;categories&quot;, &quot;product_ids&quot;) |
