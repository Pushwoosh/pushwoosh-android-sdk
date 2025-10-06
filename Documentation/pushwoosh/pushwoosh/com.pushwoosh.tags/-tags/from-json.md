//[pushwoosh](../../../index.md)/[com.pushwoosh.tags](../index.md)/[Tags](index.md)/[fromJson](from-json.md)

# fromJson

[main]\
open fun [fromJson](from-json.md)(json: JSONObject): [TagsBundle](../-tags-bundle/index.md)

Creates a TagsBundle from a JSON object. 

 This method converts a JSON object with tag name-value pairs into a TagsBundle. It's particularly useful when receiving tag data from API responses, remote config, or when importing tag data from external sources. 

**Common Use Cases:**

- API responses - import user profile data from backend APIs
- Remote config - load tags from Firebase Remote Config or similar services
- Data migration - import tags from other analytics platforms
- Bulk operations - process tag data received in JSON format

 Example: ```kotlin

	  // Parse user profile from API response
	  private void updateUserProfileFromApi(String jsonResponse) {
	      try {
	          JSONObject profileJson = new JSONObject(jsonResponse);
	          JSONObject tagsJson = profileJson.getJSONObject("user_tags");
	
	          // Example JSON:
	          // {
	          //   "Name": "John Doe",
	          //   "Age": 28,
	          //   "City": "New York",
	          //   "Subscription_Tier": "premium",
	          //   "Email_Verified": true
	          // }
	
	          TagsBundle userTags = Tags.fromJson(tagsJson);
	          Pushwoosh.getInstance().setTags(userTags);
	      } catch (JSONException e) {
	          Log.e("App", "Failed to parse user tags", e);
	      }
	  }
	
	  // Load tags from Firebase Remote Config
	  private void loadTagsFromRemoteConfig() {
	      FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
	      String tagsJsonString = remoteConfig.getString("user_default_tags");
	
	      try {
	          JSONObject tagsJson = new JSONObject(tagsJsonString);
	          TagsBundle defaultTags = Tags.fromJson(tagsJson);
	          Pushwoosh.getInstance().setTags(defaultTags);
	      } catch (JSONException e) {
	          Log.e("App", "Failed to load remote config tags", e);
	      }
	  }
	
	  // Import tags from analytics service
	  private void importAnalyticsTags(JSONObject analyticsData) {
	      try {
	          // Convert analytics properties to Pushwoosh tags
	          JSONObject pushwooshTags = new JSONObject();
	          pushwooshTags.put("User_Segment", analyticsData.getString("segment"));
	          pushwooshTags.put("Lifetime_Value", analyticsData.getInt("ltv"));
	          pushwooshTags.put("Engagement_Score", analyticsData.getInt("engagement"));
	
	          TagsBundle tags = Tags.fromJson(pushwooshTags);
	          Pushwoosh.getInstance().setTags(tags);
	      } catch (JSONException e) {
	          Log.e("App", "Failed to import analytics tags", e);
	      }
	  }
	
	  // Combine JSON tags with additional tags
	  private void setUserTagsFromMultipleSources(JSONObject apiTags) {
	      TagsBundle combinedTags = new TagsBundle.Builder()
	          .putAll(apiTags)  // Add all tags from JSON
	          .putString("App_Version", BuildConfig.VERSION_NAME)  // Add app-specific tag
	          .putDate("Tags_Updated", new Date())  // Add timestamp
	          .build();
	
	      Pushwoosh.getInstance().setTags(combinedTags);
	  }
	
```

#### Return

TagsBundle containing all tags from the JSON object

#### Parameters

main

| | |
|---|---|
| json | JSONObject containing tag name-value pairs |

#### See also

| |
|---|
| [TagsBundle.Builder](../-tags-bundle/-builder/put-all.md) |
