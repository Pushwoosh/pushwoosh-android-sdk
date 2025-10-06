//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[stringTag](string-tag.md)

# stringTag

[main]\
open fun [stringTag](string-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle with a single string tag. 

 String tags are the most versatile tag type, used for storing text values like names, locations, categories, language preferences, and any categorical or textual data. 

**Common Use Cases:**

- User profile - name, email, username, gender, occupation
- Location - city, country, region, timezone
- Preferences - language, theme, notification frequency
- Categorization - subscription tier, user segment, customer type
- Product tracking - last viewed product, favorite brand, preferred category

 Example: ```kotlin

	  // E-commerce: User profile and preferences
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Name", "Sarah Johnson"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("City", "San Francisco"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Subscription_Tier", "premium")); // "free", "basic", "premium"
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Favorite_Brand", "Nike"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Last_Viewed_Product", "product_12345"));
	
	  // News app: Content preferences
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Language", "en")); // ISO language codes
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Favorite_Section", "technology"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Content_Level", "expert")); // "beginner", "intermediate", "expert"
	
	  // Banking app: Account information
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Account_Type", "premium_checking"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Customer_Segment", "high_value"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Branch", "downtown_sf"));
	
	  // Travel app: User preferences
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Home_Airport", "SFO"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Airline", "United"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Traveler_Type", "business")); // "leisure", "business", "family"
	
	  // Fitness app: User profile
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Fitness_Goal", "weight_loss")); // "weight_loss", "muscle_gain", "endurance"
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Experience_Level", "intermediate"));
	  Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Workout_Time", "morning"));
	
```

#### Return

TagsBundle containing the single string tag

#### Parameters

main

| | |
|---|---|
| key | tag name (e.g., &quot;Name&quot;, &quot;City&quot;, &quot;Subscription_Tier&quot;, &quot;Preferred_Language&quot;) |
| value | string tag value |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/put-string.md) |
