//[pushwoosh](../../../../index.md)/[com.pushwoosh.tags](../../index.md)/[TagsBundle](../index.md)/[Builder](index.md)/[appendList](append-list.md)

# appendList

[main]\
open fun [appendList](append-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle.Builder](index.md)

Appends values to an existing list tag without replacing the entire list. 

 This operation is performed server-side. The specified values are added to the end of the existing list tag. If the tag doesn't exist, it will be created with the provided values. Duplicate values are allowed unless you handle deduplication on your end. 

When to Use:

- Adding new items to multi-value attributes (new interests, viewed products)
- Building historical lists (purchased categories, visited sections)
- Avoiding the need to fetch current list before updating

Example (E-commerce App):

```kotlin

		// User browsed new product categories
		new TagsBundle.Builder()
		    .appendList("browsed_categories", Arrays.asList("shoes", "accessories"))
		    .appendList("viewed_brands", Arrays.asList("Nike", "Adidas"))
		    .build();
		
		// User completed a purchase
		new TagsBundle.Builder()
		    .appendList("purchased_products", Arrays.asList("PROD-12345", "PROD-67890"))
		    .appendList("purchase_categories", Arrays.asList("electronics"))
		    .build();
		
```

#### Return

this Builder instance for method chaining

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;interests&quot;, &quot;purchased_categories&quot;, &quot;viewed_products&quot;) |
| value | list of string values to append to the existing list |
