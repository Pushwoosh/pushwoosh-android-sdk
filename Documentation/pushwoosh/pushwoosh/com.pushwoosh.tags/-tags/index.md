//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)

# Tags

class [Tags](index.md)

Utility class for creating single-tag [TagsBundle](../-tags-bundle/index.md) instances quickly. 

 Tags are key-value pairs that store user attributes and behavior data on the device. They enable audience segmentation for targeted push campaigns, personalized messaging, and behavioral tracking. Use tags to store user profile data, preferences, activity metrics, and any other information needed for campaign targeting. 

**When to Use Tags vs TagsBundle.Builder:**

- Use [Tags](index.md) static methods when setting a single tag quickly
- Use [TagsBundle.Builder](../-tags-bundle/-builder/index.md) when setting multiple tags in one operation (more efficient)

**Tag Types and Use Cases:**

- **Integer** - numeric values like age, purchase count, loyalty points, app version
- **Long** - large numbers like timestamps, user IDs, milliseconds since epoch
- **Boolean** - yes/no values like subscription status, email verified, premium user
- **String** - text values like name, city, subscription tier, language preference
- **List** - multiple values like favorite categories, interests, viewed product IDs
- **Date** - timestamps like last login, registration date, last purchase date

**Tag Operations:**

- **Set** - use [intTag](int-tag.md), [stringTag](string-tag.md), etc. to set or update tag values
- **Increment** - use [incrementInt](increment-int.md) to add to existing numeric values
- **Append** - use [appendList](append-list.md) to add items to existing lists
- **Remove** - use [removeFromList](remove-from-list.md) to remove items from lists or [removeTag](remove-tag.md) to delete tags

**Best Practices:**

- Use descriptive tag names in PascalCase or snake_case (e.g., &quot;Subscription_Tier&quot;, &quot;last_purchase_date&quot;)
- Keep tag names consistent across your app
- Use predefined values for categorical tags (e.g., &quot;free&quot;, &quot;basic&quot;, &quot;premium&quot; for subscription tiers)
- Combine multiple tag updates using [TagsBundle.Builder](../-tags-bundle/-builder/index.md) to reduce network calls
- Remove unused tags with [removeTag](remove-tag.md) to keep your data clean

**Complete Usage Example:**

```kotlin

  // E-commerce app: Track user profile and shopping behavior

  // 1. Set user profile on login
  TagsBundle userProfile = new TagsBundle.Builder()
      .putString("Name", "John Smith")
      .putInt("Age", 32)
      .putString("City", "New York")
      .putString("Subscription_Tier", "premium") // "free", "basic", "premium"
      .putBoolean("Email_Verified", true)
      .putDate("Registration_Date", new Date())
      .build();
  Pushwoosh.getInstance().setTags(userProfile);

  // 2. Track shopping behavior with single tags (quick updates)
  Pushwoosh.getInstance().setTags(Tags.stringTag("Last_Viewed_Category", "electronics"));
  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Purchase", new Date()));

  // 3. Increment purchase counter after successful order
  Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Purchases", 1));
  Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", 50));

  // 4. Add to favorite categories list
  List<String> newFavorites = Arrays.asList("electronics", "sports");
  Pushwoosh.getInstance().setTags(Tags.appendList("Favorite_Categories", newFavorites));

  // 5. Remove from wishlist when purchased
  List<String> purchasedItems = Arrays.asList("product_12345");
  Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", purchasedItems));

  // 6. News app: Track reading preferences
  Pushwoosh.getInstance().setTags(Tags.listTag("Interests", Arrays.asList("technology", "business", "sports")));
  Pushwoosh.getInstance().setTags(Tags.intTag("Articles_Read_Today", 5));
  Pushwoosh.getInstance().setTags(Tags.booleanTag("Breaking_News_Enabled", true));

  // 7. Fitness app: Track user activity
  Pushwoosh.getInstance().setTags(Tags.intTag("Workouts_This_Week", 4));
  Pushwoosh.getInstance().setTags(Tags.stringTag("Fitness_Level", "intermediate"));
  Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Workout", new Date()));

  // 8. Clean up: Remove deprecated tags
  Pushwoosh.getInstance().setTags(Tags.removeTag("Old_Field_Name"));

```

#### See also

| |
|---|
| [TagsBundle](../-tags-bundle/index.md) |
| [TagsBundle.Builder](../-tags-bundle/-builder/index.md) |
| [Pushwoosh](../../com.pushwoosh/-pushwoosh/set-tags.md) |

## Functions

| Name | Summary |
|---|---|
| [appendList](append-list.md) | [main]<br>open fun [appendList](append-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), list: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle that appends values to an existing list tag. |
| [booleanTag](boolean-tag.md) | [main]<br>open fun [booleanTag](boolean-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single boolean tag. |
| [dateTag](date-tag.md) | [main]<br>open fun [dateTag](date-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Date](https://developer.android.com/reference/kotlin/java/util/Date.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single date tag. |
| [empty](empty.md) | [main]<br>open fun [empty](empty.md)(): [TagsBundle](../-tags-bundle/index.md)<br>Returns an empty TagsBundle singleton instance. |
| [fromJson](from-json.md) | [main]<br>open fun [fromJson](from-json.md)(json: JSONObject): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle from a JSON object. |
| [incrementInt](increment-int.md) | [main]<br>open fun [incrementInt](increment-int.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), delta: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle that increments an integer tag value. |
| [intTag](int-tag.md) | [main]<br>open fun [intTag](int-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single integer tag. |
| [listTag](list-tag.md) | [main]<br>open fun [listTag](list-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single list tag. |
| [longTag](long-tag.md) | [main]<br>open fun [longTag](long-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single long integer tag. |
| [removeFromList](remove-from-list.md) | [main]<br>open fun [removeFromList](remove-from-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), list: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle that removes values from an existing list tag. |
| [removeTag](remove-tag.md) | [main]<br>open fun [removeTag](remove-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle that removes a tag from the device. |
| [stringTag](string-tag.md) | [main]<br>open fun [stringTag](string-tag.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), value: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [TagsBundle](../-tags-bundle/index.md)<br>Creates a TagsBundle with a single string tag. |
