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


import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable, thread-safe collection of tags for user segmentation and personalization.
 * <p>
 * TagsBundle stores key-value pairs that represent user attributes, preferences, and behaviors.
 * Tags enable targeted push notifications, audience segmentation, and personalized content delivery
 * through the Pushwoosh platform. Once built, a TagsBundle instance is immutable and can be safely
 * shared across threads.
 * <p>
 * <strong>Building vs Reading Tags:</strong>
 * <ul>
 * <li>Use {@link Builder} when you need to create a custom tag collection for multiple operations</li>
 * <li>Use {@link Tags} utility methods for simple, one-off tag operations</li>
 * <li>Use {@link com.pushwoosh.Pushwoosh#sendTags(TagsBundle)} to sync tags with Pushwoosh servers</li>
 * </ul>
 * <p>
 * <strong>Supported Tag Types:</strong>
 * <ul>
 * <li>Integer - for demographics, counters, scores (age, level, points)</li>
 * <li>Long - for timestamps, large IDs (registration_timestamp, user_id)</li>
 * <li>Boolean - for flags, subscriptions (email_subscribed, premium_user)</li>
 * <li>String - for profile data, categories (name, gender, favorite_category)</li>
 * <li>List - for multi-value attributes (interests, purchased_products)</li>
 * <li>Date - for milestones, events (last_purchase, subscription_end)</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong> TagsBundle is immutable after creation. The Builder uses
 * {@link ConcurrentHashMap} to allow safe concurrent tag additions during construction.
 * <p>
 * <strong>Usage Example (E-commerce App):</strong>
 * <pre>{@code
 * // Building tags for a premium customer
 * TagsBundle tags = new TagsBundle.Builder()
 *     .putString("customer_tier", "premium")
 *     .putInt("lifetime_orders", 15)
 *     .putLong("customer_since", System.currentTimeMillis())
 *     .putBoolean("newsletter_subscribed", true)
 *     .putList("favorite_categories", Arrays.asList("electronics", "books"))
 *     .putDate("last_purchase", new Date())
 *     .build();
 *
 * // Sending tags to Pushwoosh
 * Pushwoosh.getInstance().sendTags(tags);
 *
 * // Reading tags from bundle
 * String tier = tags.getString("customer_tier"); // "premium"
 * int orders = tags.getInt("lifetime_orders", 0); // 15
 * List<String> categories = tags.getList("favorite_categories"); // ["electronics", "books"]
 *
 * // Converting to JSON for API calls
 * JSONObject json = tags.toJson();
 * }</pre>
 *
 * @see Tags
 * @see com.pushwoosh.Pushwoosh#sendTags(TagsBundle)
 * @see <a href="http://docs.pushwoosh.com/docs/segmentation-tags-and-filters">Segmentation guide</a>
 */
public class TagsBundle {
	/**
	 * Builder for constructing {@link TagsBundle} instances using the Builder pattern.
	 * <p>
	 * The Builder allows efficient, fluent construction of tag collections through method chaining.
	 * All tag additions are stored client-side until {@link #build()} is called to create an immutable
	 * TagsBundle. The actual synchronization with Pushwoosh servers happens when you call
	 * {@link com.pushwoosh.Pushwoosh#sendTags(TagsBundle)}.
	 * <p>
	 * <strong>Thread Safety:</strong> This builder uses {@link ConcurrentHashMap} internally,
	 * making it safe to add tags from multiple threads during construction.
	 * <p>
	 * <strong>Batching Best Practice:</strong> When setting multiple tags, always use a single
	 * Builder instance and one {@link com.pushwoosh.Pushwoosh#sendTags(TagsBundle)} call instead
	 * of multiple individual tag operations. This reduces network requests and improves performance.
	 * <p>
	 * <strong>Usage Example (Fitness App):</strong>
	 * <pre>{@code
	 * TagsBundle userProfile = new TagsBundle.Builder()
	 *     // Demographics
	 *     .putInt("age", 28)
	 *     .putString("gender", "female")
	 *     .putString("fitness_level", "intermediate")
	 *
	 *     // Activity tracking
	 *     .putInt("workouts_completed", 45)
	 *     .putDate("last_workout", new Date())
	 *     .putList("favorite_activities", Arrays.asList("yoga", "running", "cycling"))
	 *
	 *     // Subscription status
	 *     .putBoolean("premium_member", true)
	 *     .putLong("subscription_end", System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
	 *
	 *     // Preferences
	 *     .putBoolean("push_workouts", true)
	 *     .putBoolean("push_achievements", true)
	 *     .build();
	 *
	 * // Send all tags in one request
	 * Pushwoosh.getInstance().sendTags(userProfile);
	 * }</pre>
	 *
	 * @see TagsBundle
	 * @see Tags
	 */
	public static class Builder {
		private final Map<String, Object> tags = new ConcurrentHashMap<>();

		/**
		 * Adds a tag with an integer value.
		 * <p>
		 * Use for demographics (age, zip code), counters (items in cart, articles read),
		 * scores (loyalty points, game level), or other numeric attributes that fit within
		 * the integer range (-2,147,483,648 to 2,147,483,647).
		 * <p>
		 * <strong>Example (E-commerce App):</strong>
		 * <pre>{@code
		 * new TagsBundle.Builder()
		 *     .putInt("age", 35)
		 *     .putInt("cart_items", 3)
		 *     .putInt("loyalty_points", 1250)
		 *     .putInt("items_viewed_today", 12)
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "age", "loyalty_points", "cart_items")
		 * @param value integer value to store
		 * @return this Builder instance for method chaining
		 */
		public Builder putInt(String key, int value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds a tag with a long value.
		 * <p>
		 * Use for timestamps (registration date, last activity), large numeric IDs (user ID, transaction ID),
		 * or any numeric value that exceeds the integer range. Long values can store numbers from
		 * -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807.
		 * <p>
		 * <strong>Example (News App):</strong>
		 * <pre>{@code
		 * long now = System.currentTimeMillis();
		 * new TagsBundle.Builder()
		 *     .putLong("user_id", 9876543210L)
		 *     .putLong("registered_at", now)
		 *     .putLong("last_article_read", now - 3600000) // 1 hour ago
		 *     .putLong("subscription_id", 1234567890123L)
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "user_id", "registered_at", "last_login")
		 * @param value long value to store
		 * @return this Builder instance for method chaining
		 */
		public Builder putLong(String key, long value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Increments an integer tag by the specified value without fetching the current value first.
		 * <p>
		 * This operation is performed server-side, making it efficient for counters that need to be
		 * updated without knowing their current value. If the tag doesn't exist, it will be created
		 * and set to the increment value. Use negative values to decrement.
		 * <p>
		 * <strong>When to Use:</strong>
		 * <ul>
		 * <li>Tracking cumulative actions (app opens, purchases made, articles read)</li>
		 * <li>Maintaining counters without client-side state management</li>
		 * <li>Avoiding race conditions when multiple devices update the same counter</li>
		 * </ul>
		 * <p>
		 * <strong>Example (Gaming App):</strong>
		 * <pre>{@code
		 * // User completed a level and earned points
		 * new TagsBundle.Builder()
		 *     .incrementInt("levels_completed", 1)
		 *     .incrementInt("total_score", 500)
		 *     .incrementInt("coins_earned", 100)
		 *     .incrementInt("deaths", -1) // negative value to decrement
		 *     .build();
		 *
		 * // Track daily engagement
		 * new TagsBundle.Builder()
		 *     .incrementInt("app_opens_today", 1)
		 *     .incrementInt("total_sessions", 1)
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "total_purchases", "app_opens", "points_earned")
		 * @param value value to increment by (positive to increase, negative to decrease)
		 * @return this Builder instance for method chaining
		 */
		public Builder incrementInt(String key, int value) {
			Map<String, Object> inc = new HashMap<>();
			inc.put("operation", "increment");
			inc.put("value", value);
			tags.put(key, inc);
			return this;
		}

		/**
		 * Appends values to an existing list tag without replacing the entire list.
		 * <p>
		 * This operation is performed server-side. The specified values are added to the end of the
		 * existing list tag. If the tag doesn't exist, it will be created with the provided values.
		 * Duplicate values are allowed unless you handle deduplication on your end.
		 * <p>
		 * <strong>When to Use:</strong>
		 * <ul>
		 * <li>Adding new items to multi-value attributes (new interests, viewed products)</li>
		 * <li>Building historical lists (purchased categories, visited sections)</li>
		 * <li>Avoiding the need to fetch current list before updating</li>
		 * </ul>
		 * <p>
		 * <strong>Example (E-commerce App):</strong>
		 * <pre>{@code
		 * // User browsed new product categories
		 * new TagsBundle.Builder()
		 *     .appendList("browsed_categories", Arrays.asList("shoes", "accessories"))
		 *     .appendList("viewed_brands", Arrays.asList("Nike", "Adidas"))
		 *     .build();
		 *
		 * // User completed a purchase
		 * new TagsBundle.Builder()
		 *     .appendList("purchased_products", Arrays.asList("PROD-12345", "PROD-67890"))
		 *     .appendList("purchase_categories", Arrays.asList("electronics"))
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "interests", "purchased_categories", "viewed_products")
		 * @param value list of string values to append to the existing list
		 * @return this Builder instance for method chaining
		 */
		public Builder appendList(String key, List<String> value) {
			Map<String, Object> append = new HashMap<>();
			append.put("operation", "append");
			append.put("value", value);
			tags.put(key, append);
			return this;
		}

		/**
		 * Removes specific values from an existing list tag without replacing the entire list.
		 * <p>
		 * This operation is performed server-side. The specified values are removed from the existing
		 * list tag. If a value appears multiple times in the list, all occurrences are removed. If the
		 * tag doesn't exist or none of the values are found, the operation has no effect.
		 * <p>
		 * <strong>When to Use:</strong>
		 * <ul>
		 * <li>Removing items from multi-value attributes (unsubscribe from topics, remove interests)</li>
		 * <li>Cleaning up historical data (remove old preferences, outdated categories)</li>
		 * <li>Avoiding the need to fetch and replace the entire list</li>
		 * </ul>
		 * <p>
		 * <strong>Example (News App):</strong>
		 * <pre>{@code
		 * // User unsubscribed from specific news categories
		 * new TagsBundle.Builder()
		 *     .removeFromList("subscribed_topics", Arrays.asList("politics", "sports"))
		 *     .removeFromList("favorite_authors", Arrays.asList("john_doe"))
		 *     .build();
		 *
		 * // Clean up user preferences
		 * new TagsBundle.Builder()
		 *     .removeFromList("blocked_categories", Arrays.asList("entertainment"))
		 *     .removeFromList("saved_for_later", Arrays.asList("article-123", "article-456"))
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "interests", "subscribed_topics", "blocked_users")
		 * @param value list of string values to remove from the existing list
		 * @return this Builder instance for method chaining
		 */
		public Builder removeFromList(String key, List<String> value) {
			Map<String, Object> remove  = new HashMap<>();
			remove.put("operation", "remove");
			remove.put("value", value);
			tags.put(key, remove);
			return this;
		}

		/**
		 * Adds a tag with a boolean value.
		 * <p>
		 * Use for binary flags, subscription states, feature toggles, or any yes/no attribute.
		 * Boolean tags are ideal for segmenting users based on true/false conditions.
		 * <p>
		 * <strong>Example (Subscription Service):</strong>
		 * <pre>{@code
		 * new TagsBundle.Builder()
		 *     // Subscription status
		 *     .putBoolean("premium_member", true)
		 *     .putBoolean("trial_active", false)
		 *     .putBoolean("auto_renew_enabled", true)
		 *
		 *     // Communication preferences
		 *     .putBoolean("email_notifications", true)
		 *     .putBoolean("push_notifications", true)
		 *     .putBoolean("sms_notifications", false)
		 *
		 *     // Feature flags
		 *     .putBoolean("beta_features_enabled", true)
		 *     .putBoolean("gdpr_consent", true)
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "premium_member", "email_subscribed", "onboarding_completed")
		 * @param value boolean value (true or false)
		 * @return this Builder instance for method chaining
		 */
		public Builder putBoolean(String key, boolean value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds a tag with a string value.
		 * <p>
		 * Use for text-based attributes such as names, categories, preferences, identifiers, or any
		 * non-numeric data. String tags are the most versatile type and support segmentation by exact
		 * match, contains, starts with, and other text-based filters.
		 * <p>
		 * <strong>Example (User Profile):</strong>
		 * <pre>{@code
		 * new TagsBundle.Builder()
		 *     // Demographics
		 *     .putString("name", "John Doe")
		 *     .putString("gender", "male")
		 *     .putString("country", "USA")
		 *     .putString("language", "en")
		 *
		 *     // Preferences
		 *     .putString("favorite_category", "electronics")
		 *     .putString("preferred_currency", "USD")
		 *     .putString("timezone", "America/New_York")
		 *
		 *     // Custom identifiers
		 *     .putString("customer_tier", "gold")
		 *     .putString("referral_code", "FRIEND2024")
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "name", "category", "language", "customer_tier")
		 * @param value string value to store (can be null, but consider using {@link #putStringIfNotEmpty(String, String)})
		 * @return this Builder instance for method chaining
		 */
		public Builder putString(String key, String value) {
			try {
				tags.put(key, value);
			} catch (Exception e) {
				PWLog.error("Failed to put String tag in TagsBundle:" + e);
			}
			return this;
		}

		/**
		 * Adds a tag with a string value only if the value is not null or empty.
		 * <p>
		 * This is a convenience method that validates the string value before adding it to the bundle.
		 * If the value is null or empty (zero-length or whitespace-only), the tag is not added.
		 * Use this when you want to avoid setting tags with empty or meaningless values.
		 * <p>
		 * <strong>Example (Form Validation):</strong>
		 * <pre>{@code
		 * // Safely add optional profile fields from user input
		 * String phoneNumber = editTextPhone.getText().toString().trim();
		 * String company = editTextCompany.getText().toString().trim();
		 * String referralCode = editTextReferral.getText().toString().trim();
		 *
		 * new TagsBundle.Builder()
		 *     .putString("email", email) // Required field, always set
		 *     .putStringIfNotEmpty("phone", phoneNumber) // Optional, only if provided
		 *     .putStringIfNotEmpty("company", company) // Optional, only if provided
		 *     .putStringIfNotEmpty("referral_code", referralCode) // Optional, only if provided
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "phone", "company", "optional_field")
		 * @param value string value to store (will only be added if not null or empty)
		 * @return this Builder instance for method chaining
		 */
		public Builder putStringIfNotEmpty(String key, String value) {
			if (!TextUtils.isEmpty(value)) {
				tags.put(key, value);
			}
			return this;
		}

		/**
		 * Adds a tag with a list of string values, replacing any existing list.
		 * <p>
		 * Use for multi-value attributes such as interests, categories, product IDs, or any attribute
		 * where a user can have multiple selections. This method replaces the entire list - to add or
		 * remove individual items from an existing list, use {@link #appendList(String, List)} or
		 * {@link #removeFromList(String, List)} instead.
		 * <p>
		 * <strong>Example (Content Preferences):</strong>
		 * <pre>{@code
		 * new TagsBundle.Builder()
		 *     // User interests
		 *     .putList("interests", Arrays.asList("technology", "sports", "travel"))
		 *     .putList("favorite_sports", Arrays.asList("football", "basketball", "tennis"))
		 *
		 *     // Product interactions
		 *     .putList("wishlist_ids", Arrays.asList("PROD-001", "PROD-042", "PROD-156"))
		 *     .putList("recently_viewed", Arrays.asList("CAT-electronics", "CAT-books"))
		 *
		 *     // Content subscriptions
		 *     .putList("newsletter_topics", Arrays.asList("daily_digest", "weekly_deals", "new_arrivals"))
		 *     .build();
		 * }</pre>
		 *
		 * @param key   tag name (e.g., "interests", "categories", "product_ids")
		 * @param value list of string values (replaces existing list completely)
		 * @return this Builder instance for method chaining
		 */
		public Builder putList(String key, List<String> value) {
			tags.put(key, value);
			return this;
		}

		/**
		 * Adds a tag with a date value, formatted as "yyyy-MM-dd HH:mm".
		 * <p>
		 * Use for tracking important dates, milestones, or time-based events. The date is automatically
		 * converted to the format "yyyy-MM-dd HH:mm" before storage. Date tags enable time-based
		 * segmentation (e.g., users who purchased in the last 30 days, subscription expiring soon).
		 * <p>
		 * <strong>Example (Subscription & Events):</strong>
		 * <pre>{@code
		 * Calendar cal = Calendar.getInstance();
		 *
		 * new TagsBundle.Builder()
		 *     // Important dates
		 *     .putDate("last_purchase", new Date())
		 *     .putDate("account_created", new Date())
		 *     .putDate("last_login", new Date())
		 *
		 *     // Subscription milestones
		 *     .putDate("trial_started", new Date())
		 *     .putDate("subscription_end", cal.getTime()) // Future date
		 *     .putDate("last_payment", new Date())
		 *
		 *     // Activity tracking
		 *     .putDate("last_app_open", new Date())
		 *     .putDate("onboarding_completed", new Date())
		 *     .build();
		 * }</pre>
		 * <p>
		 * <strong>Note:</strong> The date format is "yyyy-MM-dd HH:mm" (e.g., "2024-01-15 14:30").
		 * For timestamp-based tracking with millisecond precision, consider using {@link #putLong(String, long)}
		 * with {@link System#currentTimeMillis()}.
		 *
		 * @param key   tag name (e.g., "last_purchase", "subscription_end", "trial_started")
		 * @param value Date object to store (will be formatted as "yyyy-MM-dd HH:mm")
		 * @return this Builder instance for method chaining
		 */
		public Builder putDate(String key, Date value) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			tags.put(key, dateFormat.format(value));
			return this;
		}

		/**
		 * Removes a tag from the user's profile on Pushwoosh servers.
		 * <p>
		 * This operation marks the tag for deletion. When the TagsBundle is sent to Pushwoosh,
		 * the specified tag will be completely removed from the user's profile. Use this when you
		 * need to clean up outdated tags or reset user attributes.
		 * <p>
		 * <strong>Example (Privacy & Data Cleanup):</strong>
		 * <pre>{@code
		 * // User downgraded from premium to free
		 * new TagsBundle.Builder()
		 *     .putBoolean("premium_member", false)
		 *     .remove("premium_tier") // Remove premium-specific tag
		 *     .remove("subscription_end") // No longer relevant
		 *     .remove("auto_renew_enabled")
		 *     .build();
		 *
		 * // User opted out of personalization
		 * new TagsBundle.Builder()
		 *     .remove("interests")
		 *     .remove("favorite_categories")
		 *     .remove("browsing_history")
		 *     .putBoolean("personalization_enabled", false)
		 *     .build();
		 * }</pre>
		 *
		 * @param key tag name to remove (e.g., "old_tag", "deprecated_field")
		 * @return this Builder instance for method chaining
		 */
		public Builder remove(String key) {
			tags.put(key, NULL_PLACEHOLDER);
			return this;
		}

		/**
		 * Imports all tags from a JSON object, adding them to the builder.
		 * <p>
		 * This method extracts all key-value pairs from the provided JSON object and adds them
		 * as tags. This is useful for bulk importing tags from API responses, local storage,
		 * or configuration files. Existing tags with the same keys will be overwritten.
		 * <p>
		 * <strong>Example (Importing from API Response):</strong>
		 * <pre>{@code
		 * // Received user profile from backend API
		 * JSONObject userProfile = apiResponse.getJSONObject("profile");
		 * // {
		 * //   "name": "Jane Smith",
		 * //   "age": 32,
		 * //   "premium": true,
		 * //   "interests": ["music", "travel", "photography"]
		 * // }
		 *
		 * TagsBundle tags = new TagsBundle.Builder()
		 *     .putAll(userProfile) // Import all fields from JSON
		 *     .putString("sync_source", "api") // Add additional tags
		 *     .putLong("last_sync", System.currentTimeMillis())
		 *     .build();
		 *
		 * Pushwoosh.getInstance().sendTags(tags);
		 * }</pre>
		 * <p>
		 * <strong>Note:</strong> All JSON value types (strings, numbers, booleans, arrays, nested objects)
		 * are supported and automatically converted to appropriate tag types.
		 *
		 * @param json JSON object containing tag name-value pairs to import
		 * @return this Builder instance for method chaining
		 */
		public Builder putAll(JSONObject json) {
			Iterator<String> keys = json.keys();
			//use synchronized keyword in attempt to fix rare ConcurrentModificationException in
			// java.util.LinkedHashMap$LinkedKeyIterator.next on devices with Samsung chips (https://kanban.corp.pushwoosh.com/issue/SDK-306/)
			synchronized (keys) {
				while (keys.hasNext()) {
					String key = keys.next();
					this.tags.put(key, json.opt(key));
				}
			}

			return this;
		}

		/**
		 * Builds and returns an immutable {@link TagsBundle} instance with all added tags.
		 * <p>
		 * This method finalizes the tag collection and creates an immutable TagsBundle that can be
		 * safely passed to {@link com.pushwoosh.Pushwoosh#sendTags(TagsBundle)} or other methods.
		 * After calling build(), the Builder can still be reused to create additional TagsBundle
		 * instances, but changes to the Builder will not affect previously built instances.
		 * <p>
		 * <strong>Usage Pattern:</strong>
		 * <pre>{@code
		 * // Build a TagsBundle
		 * TagsBundle tags = new TagsBundle.Builder()
		 *     .putString("name", "John")
		 *     .putInt("age", 30)
		 *     .putBoolean("premium", true)
		 *     .build();
		 *
		 * // Send to Pushwoosh
		 * Pushwoosh.getInstance().sendTags(tags);
		 *
		 * // Read tags from the bundle
		 * String name = tags.getString("name"); // "John"
		 * int age = tags.getInt("age", 0); // 30
		 * }</pre>
		 *
		 * @return immutable TagsBundle instance containing all added tags
		 */
		public TagsBundle build() {
			return new TagsBundle(this);
		}

		public HashMap<String, Object> getTagsHashMap() {
			HashMap<String, Object> finalMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : tags.entrySet()) {
				finalMap.put(entry.getKey(),
						entry.getValue() == NULL_PLACEHOLDER ? null : entry.getValue());
			}
			return finalMap;
		}
	}

	//null placeholder is used to allow passing null to remove() method, while still utilizing
	//concurrent hashmap to avoid race condition when modifying TagsBundle builder
	private static final Object NULL_PLACEHOLDER = new Object();
	private final Map<String, Object> tags;

	private TagsBundle(Builder builder) {
		tags = builder.getTagsHashMap();
	}

	/**
	 * Retrieves an integer tag value by name, with a fallback default value.
	 * <p>
	 * Returns the integer value associated with the given key. If the tag doesn't exist or
	 * cannot be converted to an integer, returns the provided default value. This method safely
	 * handles type conversion from any Number type.
	 * <p>
	 * <strong>Example (Reading User Profile):</strong>
	 * <pre>{@code
	 * TagsBundle tags = getUserTags(); // Get from somewhere
	 *
	 * int age = tags.getInt("age", 0); // Returns age or 0 if not set
	 * int loyaltyPoints = tags.getInt("loyalty_points", 0);
	 * int cartItems = tags.getInt("cart_items", 0);
	 *
	 * // Use in business logic
	 * if (age >= 18 && loyaltyPoints > 1000) {
	 *     showPremiumOffer();
	 * }
	 * }</pre>
	 *
	 * @param key          tag name (e.g., "age", "loyalty_points", "level")
	 * @param defaultValue value to return if tag doesn't exist or cannot be converted to int
	 * @return tag value as integer, or defaultValue if tag is not found or not a number
	 */
	public int getInt(String key, int defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		return defaultValue;
	}

	/**
	 * Retrieves a long tag value by name, with a fallback default value.
	 * <p>
	 * Returns the long value associated with the given key. If the tag doesn't exist or
	 * cannot be converted to a long, returns the provided default value. This method is
	 * commonly used for timestamps and large numeric IDs.
	 * <p>
	 * <strong>Example (Subscription Management):</strong>
	 * <pre>{@code
	 * TagsBundle tags = getUserTags();
	 *
	 * long userId = tags.getLong("user_id", 0L);
	 * long registeredAt = tags.getLong("registered_at", 0L);
	 * long subscriptionEnd = tags.getLong("subscription_end", 0L);
	 *
	 * // Check if subscription is expiring soon
	 * long now = System.currentTimeMillis();
	 * long daysUntilExpiry = (subscriptionEnd - now) / (24 * 60 * 60 * 1000);
	 *
	 * if (daysUntilExpiry <= 7 && daysUntilExpiry > 0) {
	 *     showRenewalReminder();
	 * }
	 * }</pre>
	 *
	 * @param key          tag name (e.g., "user_id", "registered_at", "subscription_end")
	 * @param defaultValue value to return if tag doesn't exist or cannot be converted to long
	 * @return tag value as long, or defaultValue if tag is not found or not a number
	 */
	public long getLong(String key, long defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}

		return defaultValue;
	}

	/**
	 * Retrieves a boolean tag value by name, with a fallback default value.
	 * <p>
	 * Returns the boolean value associated with the given key. If the tag doesn't exist or
	 * is not a boolean type, returns the provided default value. Use this for feature flags,
	 * subscription status, or any true/false attribute.
	 * <p>
	 * <strong>Example (Feature Access Control):</strong>
	 * <pre>{@code
	 * TagsBundle tags = getUserTags();
	 *
	 * boolean isPremium = tags.getBoolean("premium_member", false);
	 * boolean pushEnabled = tags.getBoolean("push_notifications", true);
	 * boolean emailSubscribed = tags.getBoolean("email_subscribed", false);
	 * boolean betaAccess = tags.getBoolean("beta_features_enabled", false);
	 *
	 * // Control feature access
	 * if (isPremium) {
	 *     unlockPremiumFeatures();
	 * }
	 *
	 * // Respect notification preferences
	 * if (pushEnabled) {
	 *     scheduleNotification();
	 * }
	 * }</pre>
	 *
	 * @param key          tag name (e.g., "premium_member", "push_enabled", "trial_active")
	 * @param defaultValue value to return if tag doesn't exist or is not a boolean
	 * @return tag value as boolean, or defaultValue if tag is not found or not a boolean
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Object value = tags.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		return defaultValue;
	}

	/**
	 * Retrieves a string tag value by name, or null if not found.
	 * <p>
	 * Returns the string value associated with the given key. If the tag doesn't exist or
	 * is not a string type, returns null. Always check for null before using the returned value.
	 * <p>
	 * <strong>Example (User Profile Display):</strong>
	 * <pre>{@code
	 * TagsBundle tags = getUserTags();
	 *
	 * String name = tags.getString("name");
	 * String email = tags.getString("email");
	 * String customerTier = tags.getString("customer_tier");
	 * String favoriteCategory = tags.getString("favorite_category");
	 *
	 * // Safe null handling
	 * if (name != null) {
	 *     textViewName.setText(name);
	 * } else {
	 *     textViewName.setText("Guest User");
	 * }
	 *
	 * // Display tier with default
	 * String tier = customerTier != null ? customerTier : "standard";
	 * textViewTier.setText("Tier: " + tier);
	 * }</pre>
	 *
	 * @param key tag name (e.g., "name", "email", "category", "language")
	 * @return tag value as String, or null if tag is not found or not a string
	 */
	@Nullable
	public String getString(String key) {
		Object value = tags.get(key);
		if (value instanceof String) {
			return (String) value;
		}

		return null;
	}

	/**
	 * Retrieves a list tag value by name, or null if not found.
	 * <p>
	 * Returns the list of strings associated with the given key. If the tag doesn't exist or
	 * is not a list type, returns null. This method handles both List and JSONArray types,
	 * automatically converting JSONArray to List&lt;String&gt;. Non-string elements in the list
	 * are ignored during conversion. Always check for null before iterating.
	 * <p>
	 * <strong>Example (Content Personalization):</strong>
	 * <pre>{@code
	 * TagsBundle tags = getUserTags();
	 *
	 * List<String> interests = tags.getList("interests");
	 * List<String> purchasedCategories = tags.getList("purchased_categories");
	 * List<String> wishlist = tags.getList("wishlist_ids");
	 *
	 * // Safe iteration with null check
	 * if (interests != null && !interests.isEmpty()) {
	 *     for (String interest : interests) {
	 *         recommendContentByInterest(interest);
	 *     }
	 * }
	 *
	 * // Check for specific value
	 * if (purchasedCategories != null && purchasedCategories.contains("electronics")) {
	 *     showElectronicsDeals();
	 * }
	 * }</pre>
	 *
	 * @param key tag name (e.g., "interests", "categories", "product_ids")
	 * @return list of strings, or null if tag is not found or not a list
	 */
	@Nullable
	public List<String> getList(String key) {
		Object value = tags.get(key);
		if (value instanceof List) {
			return (List<String>) value;
		}

		List<String> result = new ArrayList<>();
		if (value instanceof JSONArray) {
			for (int i = 0; i < ((JSONArray) value).length(); i++) {
				try {
					result.add(((JSONArray) value).getString(i));
				} catch (JSONException ignore) {
					//getList return only Strings so that we can ignore all other elements
				}
			}

			return result;
		}

		return null;
	}

	/**
	 * Converts the TagsBundle to its JSON representation.
	 * <p>
	 * Returns a JSONObject containing all tags in the bundle. This method is primarily used
	 * internally by the Pushwoosh SDK when sending tags to the server, but can also be useful
	 * for logging, debugging, or integrating with other APIs that accept JSON.
	 * <p>
	 * <strong>Example (API Integration & Debugging):</strong>
	 * <pre>{@code
	 * TagsBundle tags = new TagsBundle.Builder()
	 *     .putString("name", "Alice")
	 *     .putInt("age", 28)
	 *     .putBoolean("premium", true)
	 *     .putList("interests", Arrays.asList("sports", "music"))
	 *     .build();
	 *
	 * // Convert to JSON for API call
	 * JSONObject json = tags.toJson();
	 * // Result: {"name":"Alice","age":28,"premium":true,"interests":["sports","music"]}
	 *
	 * // Send to custom backend
	 * sendToBackend(json.toString());
	 *
	 * // Log for debugging
	 * Log.d("Tags", "User tags: " + json.toString());
	 * }</pre>
	 *
	 * @return non-null JSONObject containing all tags in the bundle
	 */
	@NonNull
	public JSONObject toJson() {
		return JsonUtils.mapToJson(tags);
	}

	/**
	 * Returns the internal map representation of tags.
	 * <p>
	 * This method is intended for internal SDK use and advanced scenarios. It provides direct
	 * access to the underlying tag map structure. The returned map is immutable (backed by a
	 * HashMap created during build), so modifications will throw UnsupportedOperationException.
	 * <p>
	 * For standard tag access, prefer the type-safe getter methods: {@link #getString(String)},
	 * {@link #getInt(String, int)}, {@link #getBoolean(String, boolean)}, {@link #getList(String)},
	 * {@link #getLong(String, long)}.
	 *
	 * @return immutable map of tag names to their values
	 */
	public Map<String, Object> getMap(){ return tags; }
}
