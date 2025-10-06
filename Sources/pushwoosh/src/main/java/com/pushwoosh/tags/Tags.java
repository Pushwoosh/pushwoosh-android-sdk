/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.tags;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Utility class for creating single-tag {@link TagsBundle} instances quickly.
 * <p>
 * Tags are key-value pairs that store user attributes and behavior data on the device.
 * They enable audience segmentation for targeted push campaigns, personalized messaging,
 * and behavioral tracking. Use tags to store user profile data, preferences, activity metrics,
 * and any other information needed for campaign targeting.
 * <p>
 * <b>When to Use Tags vs TagsBundle.Builder:</b>
 * <ul>
 * <li>Use {@link Tags} static methods when setting a single tag quickly</li>
 * <li>Use {@link TagsBundle.Builder} when setting multiple tags in one operation (more efficient)</li>
 * </ul>
 * <p>
 * <b>Tag Types and Use Cases:</b>
 * <ul>
 * <li><b>Integer</b> - numeric values like age, purchase count, loyalty points, app version</li>
 * <li><b>Long</b> - large numbers like timestamps, user IDs, milliseconds since epoch</li>
 * <li><b>Boolean</b> - yes/no values like subscription status, email verified, premium user</li>
 * <li><b>String</b> - text values like name, city, subscription tier, language preference</li>
 * <li><b>List</b> - multiple values like favorite categories, interests, viewed product IDs</li>
 * <li><b>Date</b> - timestamps like last login, registration date, last purchase date</li>
 * </ul>
 * <p>
 * <b>Tag Operations:</b>
 * <ul>
 * <li><b>Set</b> - use {@link #intTag}, {@link #stringTag}, etc. to set or update tag values</li>
 * <li><b>Increment</b> - use {@link #incrementInt} to add to existing numeric values</li>
 * <li><b>Append</b> - use {@link #appendList} to add items to existing lists</li>
 * <li><b>Remove</b> - use {@link #removeFromList} to remove items from lists or {@link #removeTag} to delete tags</li>
 * </ul>
 * <p>
 * <b>Best Practices:</b>
 * <ul>
 * <li>Use descriptive tag names in PascalCase or snake_case (e.g., "Subscription_Tier", "last_purchase_date")</li>
 * <li>Keep tag names consistent across your app</li>
 * <li>Use predefined values for categorical tags (e.g., "free", "basic", "premium" for subscription tiers)</li>
 * <li>Combine multiple tag updates using {@link TagsBundle.Builder} to reduce network calls</li>
 * <li>Remove unused tags with {@link #removeTag} to keep your data clean</li>
 * </ul>
 * <p>
 * <b>Complete Usage Example:</b>
 * <pre>
 * {@code
 *   // E-commerce app: Track user profile and shopping behavior
 *
 *   // 1. Set user profile on login
 *   TagsBundle userProfile = new TagsBundle.Builder()
 *       .putString("Name", "John Smith")
 *       .putInt("Age", 32)
 *       .putString("City", "New York")
 *       .putString("Subscription_Tier", "premium") // "free", "basic", "premium"
 *       .putBoolean("Email_Verified", true)
 *       .putDate("Registration_Date", new Date())
 *       .build();
 *   Pushwoosh.getInstance().setTags(userProfile);
 *
 *   // 2. Track shopping behavior with single tags (quick updates)
 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Last_Viewed_Category", "electronics"));
 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Purchase", new Date()));
 *
 *   // 3. Increment purchase counter after successful order
 *   Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Purchases", 1));
 *   Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", 50));
 *
 *   // 4. Add to favorite categories list
 *   List<String> newFavorites = Arrays.asList("electronics", "sports");
 *   Pushwoosh.getInstance().setTags(Tags.appendList("Favorite_Categories", newFavorites));
 *
 *   // 5. Remove from wishlist when purchased
 *   List<String> purchasedItems = Arrays.asList("product_12345");
 *   Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", purchasedItems));
 *
 *   // 6. News app: Track reading preferences
 *   Pushwoosh.getInstance().setTags(Tags.listTag("Interests", Arrays.asList("technology", "business", "sports")));
 *   Pushwoosh.getInstance().setTags(Tags.intTag("Articles_Read_Today", 5));
 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Breaking_News_Enabled", true));
 *
 *   // 7. Fitness app: Track user activity
 *   Pushwoosh.getInstance().setTags(Tags.intTag("Workouts_This_Week", 4));
 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Fitness_Level", "intermediate"));
 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Workout", new Date()));
 *
 *   // 8. Clean up: Remove deprecated tags
 *   Pushwoosh.getInstance().setTags(Tags.removeTag("Old_Field_Name"));
 * }
 * </pre>
 *
 * @see TagsBundle
 * @see TagsBundle.Builder
 * @see com.pushwoosh.Pushwoosh#setTags(TagsBundle)
 */
public final class Tags {

	private static class Holder {
		private static final TagsBundle EMPTY_BUNDLE = new TagsBundle.Builder().build();
	}

	/**
	 * Creates a TagsBundle with a single integer tag.
	 * <p>
	 * Integer tags are ideal for storing numeric values like age, counters, version numbers,
	 * scores, or any whole number attribute. Use this for quick single-tag updates.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>User demographics - age, family size, number of children</li>
	 * <li>Counters - total purchases, items in cart, app sessions</li>
	 * <li>Loyalty metrics - reward points, tier level, achievement count</li>
	 * <li>App versioning - app version code, feature flags</li>
	 * <li>Engagement metrics - articles read, videos watched, products viewed</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track user demographics and activity
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Age", 28));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Items_In_Cart", 3));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Total_Purchases", 12));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Loyalty_Points", 450));
	 *
	 *   // News app: Track reading behavior
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Articles_Read_Today", 7));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Reading_Streak_Days", 15));
	 *
	 *   // Gaming app: Track player progress
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Player_Level", 42));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Total_Score", 98750));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Lives_Remaining", 3));
	 *
	 *   // Fitness app: Track workout metrics
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Workouts_This_Month", 18));
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("Calories_Burned_Today", 520));
	 *
	 *   // App version tracking
	 *   Pushwoosh.getInstance().setTags(Tags.intTag("App_Version_Code", BuildConfig.VERSION_CODE));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Age", "Total_Purchases", "Loyalty_Points")
	 * @param value integer tag value
	 * @return TagsBundle containing the single integer tag
	 * @see #incrementInt(String, int)
	 * @see TagsBundle.Builder#putInt(String, int)
	 */
	public static TagsBundle intTag(String key, int value) {
		return new TagsBundle.Builder()
				.putInt(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle with a single long integer tag.
	 * <p>
	 * Long tags are used for storing large numeric values that exceed the integer range,
	 * such as timestamps in milliseconds, large user IDs, or big numeric identifiers.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>Timestamps - Unix timestamps in milliseconds, event times</li>
	 * <li>Large identifiers - user IDs, order numbers, transaction IDs</li>
	 * <li>Big numeric values - total revenue in cents, large counters</li>
	 * <li>Database IDs - primary keys from backend systems</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Store Unix timestamp in milliseconds
	 *   long lastLoginTime = System.currentTimeMillis();
	 *   Pushwoosh.getInstance().setTags(Tags.longTag("Last_Login_Timestamp", lastLoginTime));
	 *
	 *   // Store large user ID from backend
	 *   long userId = 9876543210123L;
	 *   Pushwoosh.getInstance().setTags(Tags.longTag("Backend_User_ID", userId));
	 *
	 *   // E-commerce: Store order number
	 *   long orderNumber = 2024031500000123L;
	 *   Pushwoosh.getInstance().setTags(Tags.longTag("Last_Order_Number", orderNumber));
	 *
	 *   // Banking app: Store transaction ID
	 *   long transactionId = 8765432109876543L;
	 *   Pushwoosh.getInstance().setTags(Tags.longTag("Last_Transaction_ID", transactionId));
	 *
	 *   // Track total lifetime revenue in cents
	 *   long lifetimeRevenueCents = 12450000L; // $124,500.00
	 *   Pushwoosh.getInstance().setTags(Tags.longTag("Lifetime_Revenue_Cents", lifetimeRevenueCents));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Last_Login_Timestamp", "Backend_User_ID", "Last_Order_Number")
	 * @param value long integer tag value
	 * @return TagsBundle containing the single long tag
	 * @see TagsBundle.Builder#putLong(String, long)
	 */
	public static TagsBundle longTag(String key, long value) {
		return new TagsBundle.Builder()
				.putLong(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle with a single boolean tag.
	 * <p>
	 * Boolean tags are ideal for storing yes/no, true/false, or on/off values. They are
	 * commonly used for user preferences, feature flags, verification statuses, and subscription states.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>Subscription status - is premium user, has active subscription, trial active</li>
	 * <li>Verification status - email verified, phone verified, identity confirmed</li>
	 * <li>User preferences - notifications enabled, marketing consent, dark mode enabled</li>
	 * <li>Feature flags - beta features enabled, new UI enabled</li>
	 * <li>Activity status - onboarding completed, profile complete, first purchase made</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track subscription and verification status
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Is_Premium_User", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Email_Verified", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("SMS_Notifications_Enabled", false));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("First_Purchase_Made", true));
	 *
	 *   // News app: Track user preferences
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Breaking_News_Enabled", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Daily_Digest_Enabled", false));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Push_Notifications_Allowed", true));
	 *
	 *   // Banking app: Security and compliance
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Two_Factor_Auth_Enabled", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Biometric_Login_Enabled", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Marketing_Consent_Given", false));
	 *
	 *   // Fitness app: Track onboarding and features
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Onboarding_Completed", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Workout_Reminders_Enabled", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Premium_Features_Unlocked", false));
	 *
	 *   // General: Feature flags and A/B testing
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("New_UI_Enabled", true));
	 *   Pushwoosh.getInstance().setTags(Tags.booleanTag("Beta_Features_Enabled", false));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Is_Premium_User", "Email_Verified", "Push_Notifications_Allowed")
	 * @param value boolean tag value (true or false)
	 * @return TagsBundle containing the single boolean tag
	 * @see TagsBundle.Builder#putBoolean(String, boolean)
	 */
	public static TagsBundle booleanTag(String key, boolean value) {
		return new TagsBundle.Builder()
				.putBoolean(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle with a single string tag.
	 * <p>
	 * String tags are the most versatile tag type, used for storing text values like names,
	 * locations, categories, language preferences, and any categorical or textual data.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>User profile - name, email, username, gender, occupation</li>
	 * <li>Location - city, country, region, timezone</li>
	 * <li>Preferences - language, theme, notification frequency</li>
	 * <li>Categorization - subscription tier, user segment, customer type</li>
	 * <li>Product tracking - last viewed product, favorite brand, preferred category</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: User profile and preferences
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Name", "Sarah Johnson"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("City", "San Francisco"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Subscription_Tier", "premium")); // "free", "basic", "premium"
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Favorite_Brand", "Nike"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Last_Viewed_Product", "product_12345"));
	 *
	 *   // News app: Content preferences
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Language", "en")); // ISO language codes
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Favorite_Section", "technology"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Content_Level", "expert")); // "beginner", "intermediate", "expert"
	 *
	 *   // Banking app: Account information
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Account_Type", "premium_checking"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Customer_Segment", "high_value"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Branch", "downtown_sf"));
	 *
	 *   // Travel app: User preferences
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Home_Airport", "SFO"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Airline", "United"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Traveler_Type", "business")); // "leisure", "business", "family"
	 *
	 *   // Fitness app: User profile
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Fitness_Goal", "weight_loss")); // "weight_loss", "muscle_gain", "endurance"
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Experience_Level", "intermediate"));
	 *   Pushwoosh.getInstance().setTags(Tags.stringTag("Preferred_Workout_Time", "morning"));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Name", "City", "Subscription_Tier", "Preferred_Language")
	 * @param value string tag value
	 * @return TagsBundle containing the single string tag
	 * @see TagsBundle.Builder#putString(String, String)
	 */
	public static TagsBundle stringTag(String key, String value) {
		return new TagsBundle.Builder()
				.putString(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle with a single list tag.
	 * <p>
	 * List tags store multiple string values for a single key, perfect for tracking collections
	 * like interests, favorites, categories, or any multi-value attributes. Lists enable precise
	 * audience segmentation based on multiple criteria.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>User interests - topics, categories, hobbies, preferences</li>
	 * <li>Product tracking - viewed products, wishlist items, cart contents</li>
	 * <li>Content preferences - favorite genres, followed topics, subscribed channels</li>
	 * <li>Feature usage - enabled features, completed modules, unlocked achievements</li>
	 * <li>Multi-select attributes - selected options, chosen preferences</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track shopping interests and activity
	 *   List<String> interests = Arrays.asList("electronics", "sports", "fashion");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Interests", interests));
	 *
	 *   List<String> viewedProducts = Arrays.asList("product_12345", "product_67890", "product_11111");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Recently_Viewed", viewedProducts));
	 *
	 *   List<String> favoriteBrands = Arrays.asList("Nike", "Adidas", "Under Armour");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Brands", favoriteBrands));
	 *
	 *   // News app: Content preferences
	 *   List<String> topics = Arrays.asList("technology", "business", "sports", "entertainment");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Subscribed_Topics", topics));
	 *
	 *   List<String> authors = Arrays.asList("john_doe", "jane_smith", "tech_writer");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Followed_Authors", authors));
	 *
	 *   // Streaming app: Media preferences
	 *   List<String> genres = Arrays.asList("action", "comedy", "documentary", "sci-fi");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Genres", genres));
	 *
	 *   List<String> watchlist = Arrays.asList("movie_001", "series_042", "movie_123");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Watchlist", watchlist));
	 *
	 *   // Travel app: Preferences
	 *   List<String> destinations = Arrays.asList("Paris", "Tokyo", "New York", "London");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Favorite_Destinations", destinations));
	 *
	 *   List<String> activities = Arrays.asList("hiking", "museums", "food_tours", "beaches");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Preferred_Activities", activities));
	 *
	 *   // Fitness app: Workout preferences
	 *   List<String> workoutTypes = Arrays.asList("cardio", "strength", "yoga", "hiit");
	 *   Pushwoosh.getInstance().setTags(Tags.listTag("Workout_Preferences", workoutTypes));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Interests", "Favorite_Categories", "Recently_Viewed")
	 * @param value list of string values
	 * @return TagsBundle containing the single list tag
	 * @see #appendList(String, List)
	 * @see #removeFromList(String, List)
	 * @see TagsBundle.Builder#putList(String, List)
	 */
	public static TagsBundle listTag(String key, List<String> value) {
		return new TagsBundle.Builder()
				.putList(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle with a single date tag.
	 * <p>
	 * Date tags store timestamp values for tracking when events occurred. They are automatically
	 * formatted as "yyyy-MM-dd HH:mm" strings. Date tags are essential for time-based segmentation
	 * and re-engagement campaigns.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>User activity - last login, last purchase, last app open</li>
	 * <li>Account milestones - registration date, subscription start, first purchase</li>
	 * <li>Content interaction - last article read, last video watched</li>
	 * <li>Re-engagement - last session date, last notification opened</li>
	 * <li>Time-sensitive events - trial expiration, subscription renewal date</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track user activity and milestones
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Login", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Purchase", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Registration_Date", user.getCreatedAt()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("First_Purchase_Date", order.getCreatedAt()));
	 *
	 *   // Subscription management
	 *   Date subscriptionStart = new Date();
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Subscription_Start_Date", subscriptionStart));
	 *
	 *   Calendar calendar = Calendar.getInstance();
	 *   calendar.add(Calendar.MONTH, 1);
	 *   Date renewalDate = calendar.getTime();
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Subscription_Renewal_Date", renewalDate));
	 *
	 *   // News app: Content engagement tracking
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Article_Read", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Notification_Opened", new Date()));
	 *
	 *   // Banking app: Transaction and activity tracking
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Transaction_Date", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Bill_Payment", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Account_Created", accountDate));
	 *
	 *   // Fitness app: Workout and progress tracking
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Workout", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Goal_Start_Date", goalStartDate));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Weight_Log", new Date()));
	 *
	 *   // Re-engagement campaigns: Track inactivity
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_App_Open", new Date()));
	 *   Pushwoosh.getInstance().setTags(Tags.dateTag("Last_Feature_Used", new Date()));
	 * }
	 * </pre>
	 *
	 * @param key   tag name (e.g., "Last_Login", "Registration_Date", "Last_Purchase")
	 * @param value Date object to be formatted as "yyyy-MM-dd HH:mm"
	 * @return TagsBundle containing the single date tag
	 * @see TagsBundle.Builder#putDate(String, Date)
	 */
	public static TagsBundle dateTag(String key, Date value) {
		return new TagsBundle.Builder()
				.putDate(key, value)
				.build();
	}

	/**
	 * Creates a TagsBundle that removes a tag from the device.
	 * <p>
	 * This method is used to delete tags that are no longer needed or contain outdated information.
	 * Removing unused tags keeps your data clean and improves targeting accuracy. Common scenarios
	 * include user logout, feature deprecation, or data cleanup.
	 * <p>
	 * <b>When to Remove Tags:</b>
	 * <ul>
	 * <li>User logout - remove user-specific tags like name, email, subscription tier</li>
	 * <li>Feature deprecation - remove tags for discontinued features or old implementations</li>
	 * <li>Preference reset - remove user preferences when reverting to defaults</li>
	 * <li>Data cleanup - remove temporary or session-specific tags</li>
	 * <li>Privacy compliance - remove personal data when requested by user</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // User logout: Clean up user-specific data
	 *   private void onUserLogout() {
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Name"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Email"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Subscription_Tier"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Loyalty_Points"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Last_Purchase"));
	 *
	 *       // Also clear user ID
	 *       Pushwoosh.getInstance().setUserId("");
	 *   }
	 *
	 *   // Remove deprecated tags after app update
	 *   private void cleanupDeprecatedTags() {
	 *       // Old tag names being replaced with new ones
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("old_subscription_field"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("legacy_user_type"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("deprecated_preference"));
	 *   }
	 *
	 *   // E-commerce: Clear cart-related tags after checkout
	 *   private void onOrderCompleted() {
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Items_In_Cart"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Cart_Value"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Cart_Abandoned_Date"));
	 *   }
	 *
	 *   // Privacy: Remove user data on request
	 *   private void deleteUserData() {
	 *       // Remove all personal information tags
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Name"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Email"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Phone"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("City"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Age"));
	 *   }
	 *
	 *   // Reset user preferences to defaults
	 *   private void resetPreferences() {
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Notification_Frequency"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Preferred_Language"));
	 *       Pushwoosh.getInstance().setTags(Tags.removeTag("Theme_Preference"));
	 *   }
	 *
	 *   // Bulk cleanup using TagsBundle.Builder (more efficient)
	 *   private void bulkRemoveTags() {
	 *       TagsBundle cleanup = new TagsBundle.Builder()
	 *           .remove("Old_Field_1")
	 *           .remove("Old_Field_2")
	 *           .remove("Deprecated_Tag")
	 *           .remove("Unused_Preference")
	 *           .build();
	 *       Pushwoosh.getInstance().setTags(cleanup);
	 *   }
	 * }
	 * </pre>
	 *
	 * @param key tag name to remove (e.g., "Name", "Old_Field", "Deprecated_Tag")
	 * @return TagsBundle containing the tag removal operation
	 * @see TagsBundle.Builder#remove(String)
	 */
	public static TagsBundle removeTag(String key) {
		return new TagsBundle.Builder()
				.remove(key)
				.build();
	}

	/**
	 * Creates a TagsBundle from a JSON object.
	 * <p>
	 * This method converts a JSON object with tag name-value pairs into a TagsBundle.
	 * It's particularly useful when receiving tag data from API responses, remote config,
	 * or when importing tag data from external sources.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>API responses - import user profile data from backend APIs</li>
	 * <li>Remote config - load tags from Firebase Remote Config or similar services</li>
	 * <li>Data migration - import tags from other analytics platforms</li>
	 * <li>Bulk operations - process tag data received in JSON format</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Parse user profile from API response
	 *   private void updateUserProfileFromApi(String jsonResponse) {
	 *       try {
	 *           JSONObject profileJson = new JSONObject(jsonResponse);
	 *           JSONObject tagsJson = profileJson.getJSONObject("user_tags");
	 *
	 *           // Example JSON:
	 *           // {
	 *           //   "Name": "John Doe",
	 *           //   "Age": 28,
	 *           //   "City": "New York",
	 *           //   "Subscription_Tier": "premium",
	 *           //   "Email_Verified": true
	 *           // }
	 *
	 *           TagsBundle userTags = Tags.fromJson(tagsJson);
	 *           Pushwoosh.getInstance().setTags(userTags);
	 *       } catch (JSONException e) {
	 *           Log.e("App", "Failed to parse user tags", e);
	 *       }
	 *   }
	 *
	 *   // Load tags from Firebase Remote Config
	 *   private void loadTagsFromRemoteConfig() {
	 *       FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
	 *       String tagsJsonString = remoteConfig.getString("user_default_tags");
	 *
	 *       try {
	 *           JSONObject tagsJson = new JSONObject(tagsJsonString);
	 *           TagsBundle defaultTags = Tags.fromJson(tagsJson);
	 *           Pushwoosh.getInstance().setTags(defaultTags);
	 *       } catch (JSONException e) {
	 *           Log.e("App", "Failed to load remote config tags", e);
	 *       }
	 *   }
	 *
	 *   // Import tags from analytics service
	 *   private void importAnalyticsTags(JSONObject analyticsData) {
	 *       try {
	 *           // Convert analytics properties to Pushwoosh tags
	 *           JSONObject pushwooshTags = new JSONObject();
	 *           pushwooshTags.put("User_Segment", analyticsData.getString("segment"));
	 *           pushwooshTags.put("Lifetime_Value", analyticsData.getInt("ltv"));
	 *           pushwooshTags.put("Engagement_Score", analyticsData.getInt("engagement"));
	 *
	 *           TagsBundle tags = Tags.fromJson(pushwooshTags);
	 *           Pushwoosh.getInstance().setTags(tags);
	 *       } catch (JSONException e) {
	 *           Log.e("App", "Failed to import analytics tags", e);
	 *       }
	 *   }
	 *
	 *   // Combine JSON tags with additional tags
	 *   private void setUserTagsFromMultipleSources(JSONObject apiTags) {
	 *       TagsBundle combinedTags = new TagsBundle.Builder()
	 *           .putAll(apiTags)  // Add all tags from JSON
	 *           .putString("App_Version", BuildConfig.VERSION_NAME)  // Add app-specific tag
	 *           .putDate("Tags_Updated", new Date())  // Add timestamp
	 *           .build();
	 *
	 *       Pushwoosh.getInstance().setTags(combinedTags);
	 *   }
	 * }
	 * </pre>
	 *
	 * @param json JSONObject containing tag name-value pairs
	 * @return TagsBundle containing all tags from the JSON object
	 * @see TagsBundle.Builder#putAll(JSONObject)
	 */
	public static TagsBundle fromJson(JSONObject json) {
		return new TagsBundle.Builder()
				.putAll(json)
				.build();
	}

	/**
	 * Creates a TagsBundle that increments an integer tag value.
	 * <p>
	 * This method adds the specified delta value to the existing tag value on the server.
	 * It's perfect for tracking counters, scores, and accumulative metrics without needing
	 * to know the current value. The operation is atomic and handled server-side.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>Purchase tracking - increment total purchases, order count</li>
	 * <li>Loyalty programs - add points, increase reward balance</li>
	 * <li>Engagement metrics - increment article reads, videos watched, sessions</li>
	 * <li>Gaming - add to score, increase level, award coins</li>
	 * <li>Activity counters - track app opens, feature usage, interactions</li>
	 * </ul>
	 * <p>
	 * <b>Important:</b> Use positive values to increment and negative values to decrement.
	 * The tag must already exist with an integer value, or it will be created with the delta value.
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track purchase activity
	 *   private void onPurchaseCompleted(Order order) {
	 *       // Increment total purchase count
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Purchases", 1));
	 *
	 *       // Add loyalty points based on order value
	 *       int pointsEarned = (int) (order.getTotal() * 10); // 10 points per dollar
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", pointsEarned));
	 *
	 *       // Increment items purchased count
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Items_Purchased", order.getItemCount()));
	 *   }
	 *
	 *   // News app: Track reading activity
	 *   private void onArticleRead() {
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Articles_Read_Total", 1));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Articles_Read_Today", 1));
	 *   }
	 *
	 *   // Gaming: Update player stats
	 *   private void onLevelCompleted(int scoreEarned, int coinsCollected) {
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Score", scoreEarned));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Coins", coinsCollected));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Levels_Completed", 1));
	 *   }
	 *
	 *   // Fitness app: Track workout metrics
	 *   private void onWorkoutCompleted(int caloriesBurned) {
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Workouts", 1));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Workouts_This_Week", 1));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Total_Calories_Burned", caloriesBurned));
	 *   }
	 *
	 *   // App engagement: Track feature usage
	 *   private void onFeatureUsed(String featureName) {
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("App_Opens", 1));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Feature_" + featureName + "_Used", 1));
	 *   }
	 *
	 *   // Decrement example: Using points or lives
	 *   private void onPointsRedeemed(int pointsUsed) {
	 *       // Use negative value to decrement
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Loyalty_Points", -pointsUsed));
	 *   }
	 *
	 *   // Session tracking
	 *   private void onAppLaunched() {
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Session_Count", 1));
	 *       Pushwoosh.getInstance().setTags(Tags.incrementInt("Sessions_This_Month", 1));
	 *   }
	 * }
	 * </pre>
	 *
	 * @param key   tag name to increment (e.g., "Total_Purchases", "Loyalty_Points", "Session_Count")
	 * @param delta value to add (positive to increment, negative to decrement)
	 * @return TagsBundle containing the increment operation
	 * @see TagsBundle.Builder#incrementInt(String, int)
	 */
	public static TagsBundle incrementInt(String key, int delta) {
		return new TagsBundle.Builder()
				.incrementInt(key, delta)
				.build();
	}

	/**
	 * Creates a TagsBundle that appends values to an existing list tag.
	 * <p>
	 * This method adds new items to an existing list tag without replacing the entire list.
	 * Items are added only if they don't already exist in the list (no duplicates).
	 * The operation is performed server-side, so you don't need to retrieve the current list first.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>Shopping behavior - add to viewed products, recently browsed categories</li>
	 * <li>Content preferences - add to favorite topics, followed channels, interests</li>
	 * <li>User engagement - add to completed modules, unlocked achievements</li>
	 * <li>Product tracking - add to wishlist, cart history, purchase history</li>
	 * <li>Dynamic segmentation - add to user segments, interest groups</li>
	 * </ul>
	 * <p>
	 * <b>Important:</b> The tag must already exist as a list tag. Duplicate values are automatically
	 * ignored - only unique values will be added to the list.
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Track browsing and shopping activity
	 *   private void onProductViewed(String productId, String category) {
	 *       // Add to recently viewed products
	 *       List<String> viewedProduct = Arrays.asList(productId);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Recently_Viewed_Products", viewedProduct));
	 *
	 *       // Add to browsed categories (no duplicates)
	 *       List<String> categories = Arrays.asList(category);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Browsed_Categories", categories));
	 *   }
	 *
	 *   private void onAddToWishlist(List<String> productIds) {
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Wishlist", productIds));
	 *   }
	 *
	 *   // News app: Track content preferences
	 *   private void onTopicFollowed(String topic) {
	 *       List<String> newTopic = Arrays.asList(topic);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Followed_Topics", newTopic));
	 *   }
	 *
	 *   private void onArticleBookmarked(String articleId, String category) {
	 *       List<String> bookmark = Arrays.asList(articleId);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Bookmarked_Articles", bookmark));
	 *
	 *       // Also track interest in this category
	 *       List<String> interest = Arrays.asList(category);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Interests", interest));
	 *   }
	 *
	 *   // Streaming app: Track viewing preferences
	 *   private void onShowAddedToWatchlist(String showId, List<String> genres) {
	 *       List<String> show = Arrays.asList(showId);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Watchlist", show));
	 *
	 *       // Add genres to favorite genres
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Favorite_Genres", genres));
	 *   }
	 *
	 *   // Fitness app: Track workout preferences
	 *   private void onWorkoutTypeCompleted(String workoutType) {
	 *       List<String> workout = Arrays.asList(workoutType);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Workout_Types", workout));
	 *   }
	 *
	 *   // Education app: Track learning progress
	 *   private void onCourseCompleted(String courseId, String subject) {
	 *       List<String> course = Arrays.asList(courseId);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Courses", course));
	 *
	 *       List<String> subjectInterest = Arrays.asList(subject);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Subject_Interests", subjectInterest));
	 *   }
	 *
	 *   // Travel app: Build travel profile
	 *   private void onDestinationSaved(String destination, String country) {
	 *       List<String> dest = Arrays.asList(destination);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Saved_Destinations", dest));
	 *
	 *       List<String> countryList = Arrays.asList(country);
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Countries_Of_Interest", countryList));
	 *   }
	 * }
	 * </pre>
	 *
	 * @param key  list tag name (e.g., "Recently_Viewed_Products", "Interests", "Wishlist")
	 * @param list values to append to the existing list (duplicates are ignored)
	 * @return TagsBundle containing the append list operation
	 * @see #removeFromList(String, List)
	 * @see #listTag(String, List)
	 * @see TagsBundle.Builder#appendList(String, List)
	 */
	public static TagsBundle appendList(String key, List<String> list) {
		return new TagsBundle.Builder()
				.appendList(key, list)
				.build();
	}

	/**
	 * Creates a TagsBundle that removes values from an existing list tag.
	 * <p>
	 * This method removes specified items from an existing list tag without replacing the entire list.
	 * The operation is performed server-side, so you don't need to retrieve the current list first.
	 * Only matching values will be removed; non-existent values are silently ignored.
	 * <p>
	 * <b>Common Use Cases:</b>
	 * <ul>
	 * <li>Shopping behavior - remove purchased items from wishlist, remove from cart</li>
	 * <li>Content management - unfollow topics, remove bookmarks, unsubscribe from channels</li>
	 * <li>User preferences - remove interests, deselect categories</li>
	 * <li>Cleanup operations - remove outdated items, clear temporary selections</li>
	 * <li>Opt-out scenarios - remove from notification categories, preference lists</li>
	 * </ul>
	 * <p>
	 * <b>Important:</b> The tag must already exist as a list tag. Only specified values that exist
	 * in the list will be removed.
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce: Remove items when purchased
	 *   private void onItemPurchased(List<String> purchasedProductIds) {
	 *       // Remove purchased items from wishlist
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", purchasedProductIds));
	 *
	 *       // Remove from cart
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Cart_Items", purchasedProductIds));
	 *   }
	 *
	 *   private void onWishlistItemRemoved(String productId) {
	 *       List<String> itemToRemove = Arrays.asList(productId);
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Wishlist", itemToRemove));
	 *   }
	 *
	 *   // News app: Unfollow topics and remove bookmarks
	 *   private void onTopicUnfollowed(String topic) {
	 *       List<String> topicToRemove = Arrays.asList(topic);
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Followed_Topics", topicToRemove));
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Interests", topicToRemove));
	 *   }
	 *
	 *   private void onBookmarkRemoved(List<String> articleIds) {
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Bookmarked_Articles", articleIds));
	 *   }
	 *
	 *   // Streaming app: Remove from watchlist
	 *   private void onShowWatched(String showId) {
	 *       List<String> watchedShow = Arrays.asList(showId);
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Watchlist", watchedShow));
	 *
	 *       // Add to completed list
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Shows", watchedShow));
	 *   }
	 *
	 *   // Notifications: Unsubscribe from categories
	 *   private void onCategoryUnsubscribed(List<String> categories) {
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Notification_Categories", categories));
	 *   }
	 *
	 *   // Fitness app: Remove completed workout from schedule
	 *   private void onWorkoutCompleted(String workoutId) {
	 *       List<String> completedWorkout = Arrays.asList(workoutId);
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Scheduled_Workouts", completedWorkout));
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Completed_Workouts", completedWorkout));
	 *   }
	 *
	 *   // Travel app: Remove visited destinations
	 *   private void onDestinationVisited(String destination) {
	 *       List<String> visited = Arrays.asList(destination);
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Bucket_List", visited));
	 *       Pushwoosh.getInstance().setTags(Tags.appendList("Visited_Destinations", visited));
	 *   }
	 *
	 *   // User preferences: Opt out of interests
	 *   private void onInterestRemoved(List<String> unwantedInterests) {
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Interests", unwantedInterests));
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Content_Preferences", unwantedInterests));
	 *   }
	 *
	 *   // Cleanup: Remove outdated items from recent views
	 *   private void cleanupOldViews(List<String> oldProductIds) {
	 *       Pushwoosh.getInstance().setTags(Tags.removeFromList("Recently_Viewed_Products", oldProductIds));
	 *   }
	 * }
	 * </pre>
	 *
	 * @param key  list tag name (e.g., "Wishlist", "Followed_Topics", "Notification_Categories")
	 * @param list values to remove from the existing list
	 * @return TagsBundle containing the remove from list operation
	 * @see #appendList(String, List)
	 * @see #listTag(String, List)
	 * @see TagsBundle.Builder#removeFromList(String, List)
	 */
	public static TagsBundle removeFromList(String key, List<String> list) {
		return new TagsBundle.Builder()
				.removeFromList(key, list)
				.build();
	}


	/**
	 * Returns an empty TagsBundle singleton instance.
	 * <p>
	 * This method provides a reusable empty TagsBundle that contains no tags. It's useful
	 * when you need to pass a TagsBundle parameter but have no tags to set, or when implementing
	 * conditional tag logic where an empty bundle represents "no changes".
	 * <p>
	 * <b>When to Use:</b>
	 * <ul>
	 * <li>Default values - provide empty bundle when no tags are available</li>
	 * <li>Conditional logic - return empty bundle when conditions aren't met</li>
	 * <li>Placeholder - use as a safe placeholder in method signatures</li>
	 * <li>Memory efficiency - reuse the singleton instead of creating new empty bundles</li>
	 * </ul>
	 * <br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Conditional tag setting based on user state
	 *   private TagsBundle getUserTags(User user) {
	 *       if (user == null || !user.isLoggedIn()) {
	 *           // No user data available, return empty bundle
	 *           return Tags.empty();
	 *       }
	 *
	 *       return new TagsBundle.Builder()
	 *           .putString("Name", user.getName())
	 *           .putString("Email", user.getEmail())
	 *           .build();
	 *   }
	 *
	 *   // Use with optional features
	 *   private void updateUserPreferences(UserPreferences prefs) {
	 *       TagsBundle tags = prefs.hasChanges()
	 *           ? buildPreferencesTags(prefs)
	 *           : Tags.empty();
	 *
	 *       if (tags != Tags.empty()) {
	 *           Pushwoosh.getInstance().setTags(tags);
	 *       }
	 *   }
	 *
	 *   // Safe method parameters
	 *   private void registerUser(User user, TagsBundle additionalTags) {
	 *       // If no additional tags provided, use empty bundle
	 *       if (additionalTags == null) {
	 *           additionalTags = Tags.empty();
	 *       }
	 *
	 *       TagsBundle userTags = new TagsBundle.Builder()
	 *           .putString("User_ID", user.getId())
	 *           .putAll(additionalTags.toJson())
	 *           .build();
	 *
	 *       Pushwoosh.getInstance().registerForPushNotificationsWithTags(userTags);
	 *   }
	 *
	 *   // Factory pattern for tag generation
	 *   private TagsBundle createTagsForEvent(Event event) {
	 *       switch (event.getType()) {
	 *           case LOGIN:
	 *               return new TagsBundle.Builder()
	 *                   .putDate("Last_Login", new Date())
	 *                   .build();
	 *           case LOGOUT:
	 *               // No tags needed for logout
	 *               return Tags.empty();
	 *           case PURCHASE:
	 *               return Tags.incrementInt("Total_Purchases", 1);
	 *           default:
	 *               return Tags.empty();
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @return Singleton empty TagsBundle instance
	 */
	public static TagsBundle empty() {
		return Holder.EMPTY_BUNDLE;
	}

	private Tags() { /* do nothing */ }
}
