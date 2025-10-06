//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[listTag](list-tag.md)

# listTag

[main]\
open fun [listTag](list-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single list tag. 

 List tags store multiple string values for a single key, perfect for tracking collections like interests, favorites, categories, or any multi-value attributes. Lists enable precise audience segmentation based on multiple criteria. 

**Common Use Cases:**

- User interests - topics, categories, hobbies, preferences
- Product tracking - viewed products, wishlist items, cart contents
- Content preferences - favorite genres, followed topics, subscribed channels
- Feature usage - enabled features, completed modules, unlocked achievements
- Multi-select attributes - selected options, chosen preferences

 Example: ```kotlin

	  // E-commerce: Track shopping interests and activity
	  List<String> interests = Arrays.asList("electronics", "sports", "fashion");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Interests", interests));
	
	  List<String> viewedProducts = Arrays.asList("product_12345", "product_67890", "product_11111");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Recently_Viewed", viewedProducts));
	
	  List<String> favoriteBrands = Arrays.asList("Nike", "Adidas", "Under Armour");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Brands", favoriteBrands));
	
	  // News app: Content preferences
	  List<String> topics = Arrays.asList("technology", "business", "sports", "entertainment");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Subscribed_Topics", topics));
	
	  List<String> authors = Arrays.asList("john_doe", "jane_smith", "tech_writer");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Followed_Authors", authors));
	
	  // Streaming app: Media preferences
	  List<String> genres = Arrays.asList("action", "comedy", "documentary", "sci-fi");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Genres", genres));
	
	  List<String> watchlist = Arrays.asList("movie_001", "series_042", "movie_123");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Watchlist", watchlist));
	
	  // Travel app: Preferences
	  List<String> destinations = Arrays.asList("Paris", "Tokyo", "New York", "London");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Destinations", destinations));
	
	  List<String> activities = Arrays.asList("hiking", "museums", "food_tours", "beaches");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Preferred_Activities", activities));
	
	  // Fitness app: Workout preferences
	  List<String> workoutTypes = Arrays.asList("cardio", "strength", "yoga", "hiit");
	  Pushwoosh.getInstance().setTags(Tags.listTag("Workout_Preferences", workoutTypes));
	
```

#### Return

TagsBundle containing the single list tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Interests&quot;, &quot;Favorite_Categories&quot;, &quot;Recently_Viewed&quot;) |
| value | list of string values |

#### See also

| |
|---|
| [appendList(String, List)](append-list.md) |
| [removeFromList(String, List)](remove-from-list.md) |
| [TagsBundle.Builder](../-tags-bundle/-builder/put-list.md) |
