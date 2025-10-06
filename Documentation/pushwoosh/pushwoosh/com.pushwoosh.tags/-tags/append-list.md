//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[appendList](append-list.md)

# appendList

[main]\
open fun [appendList](append-list.md)(key: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), list: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle that appends values to an existing list tag. 

 This method adds new items to an existing list tag without replacing the entire list. Items are added only if they don't already exist in the list (no duplicates). The operation is performed server-side, so you don't need to retrieve the current list first. 

**Common Use Cases:**

- Shopping behavior - add to viewed products, recently browsed categories
- Content preferences - add to favorite topics, followed channels, interests
- User engagement - add to completed modules, unlocked achievements
- Product tracking - add to wishlist, cart history, purchase history
- Dynamic segmentation - add to user segments, interest groups

**Important:** The tag must already exist as a list tag. Duplicate values are automatically ignored - only unique values will be added to the list.  Example: 

```kotlin

	  // E-commerce: Track browsing and shopping activity
	  private void onProductViewed(String productId, String category) {
	      // Add to recently viewed products
	      List<String> viewedProduct = Arrays.asList(productId);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Recently_Viewed_Products", viewedProduct));
	
	      // Add to browsed categories (no duplicates)
	      List<String> categories = Arrays.asList(category);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Browsed_Categories", categories));
	  }
	
	  private void onAddToWishlist(List<String> productIds) {
	      Pushwoosh.getInstance().setTags(Tags.appendList("Wishlist", productIds));
	  }
	
	  // News app: Track content preferences
	  private void onTopicFollowed(String topic) {
	      List<String> newTopic = Arrays.asList(topic);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Followed_Topics", newTopic));
	  }
	
	  private void onArticleBookmarked(String articleId, String category) {
	      List<String> bookmark = Arrays.asList(articleId);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Bookmarked_Articles", bookmark));
	
	      // Also track interest in this category
	      List<String> interest = Arrays.asList(category);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Interests", interest));
	  }
	
	  // Streaming app: Track viewing preferences
	  private void onShowAddedToWatchlist(String showId, List<String> genres) {
	      List<String> show = Arrays.asList(showId);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Watchlist", show));
	
	      // Add genres to favorite genres
	      Pushwoosh.getInstance().setTags(Tags.appendList("Favorite_Genres", genres));
	  }
	
	  // Fitness app: Track workout preferences
	  private void onWorkoutTypeCompleted(String workoutType) {
	      List<String> workout = Arrays.asList(workoutType);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Workout_Types", workout));
	  }
	
	  // Education app: Track learning progress
	  private void onCourseCompleted(String courseId, String subject) {
	      List<String> course = Arrays.asList(courseId);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Courses", course));
	
	      List<String> subjectInterest = Arrays.asList(subject);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Subject_Interests", subjectInterest));
	  }
	
	  // Travel app: Build travel profile
	  private void onDestinationSaved(String destination, String country) {
	      List<String> dest = Arrays.asList(destination);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Saved_Destinations", dest));
	
	      List<String> countryList = Arrays.asList(country);
	      Pushwoosh.getInstance().setTags(Tags.appendList("Countries_Of_Interest", countryList));
	  }
	
```

#### Return

TagsBundle containing the append list operation

#### Parameters

main

| | |
|---|---|
| key | list tag name (e.g., &quot;Recently_Viewed_Products&quot;, &quot;Interests&quot;, &quot;Wishlist&quot;) |
| list | values to append to the existing list (duplicates are ignored) |

#### See also

| |
|---|
| [removeFromList(String, List)](remove-from-list.md) |
| [listTag(String, List)](list-tag.md) |
| [TagsBundle.Builder](../-tags-bundle/-builder/append-list.md) |
