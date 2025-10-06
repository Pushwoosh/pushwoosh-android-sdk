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

package com.pushwoosh.function;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.pushwoosh.exception.PushwooshException;

/**
 * The main callback interface for handling results of asynchronous operations in Pushwoosh SDK.
 * <p>
 * All asynchronous SDK operations (registration, tags management, user operations) use this callback
 * interface to return results. The callback is always invoked on the main (UI) thread, making it safe
 * to update UI elements directly without additional thread synchronization.
 * <p>
 * <b>When to use callbacks:</b>
 * <ul>
 * <li>When you need to know if the operation succeeded or failed</li>
 * <li>When you need to handle operation results (e.g., push token, tags data)</li>
 * <li>When you want to show user feedback or update UI based on results</li>
 * <li>When implementing retry logic for failed operations</li>
 * </ul>
 * <p>
 * Most SDK methods offer both callback and fire-and-forget versions. Use the callback version when
 * you need to handle results; use the version without callback for fire-and-forget operations.
 * <br><br>
 * <b>Basic usage example:</b>
 * <pre>
 * {@code
 *   // Register for push notifications with callback
 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *       if (result.isSuccess()) {
 *           // Operation succeeded
 *           String token = result.getData().getToken();
 *           Log.d("App", "Push token: " + token);
 *       } else {
 *           // Operation failed
 *           Exception error = result.getException();
 *           Log.e("App", "Registration failed: " + error.getMessage());
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Example with user tags:</b>
 * <pre>
 * {@code
 *   // Set tags with error handling
 *   TagsBundle tags = new TagsBundle.Builder()
 *       .putString("Name", "John Doe")
 *       .putInt("Age", 25)
 *       .build();
 *
 *   Pushwoosh.getInstance().setTags(tags, (result) -> {
 *       if (result.isSuccess()) {
 *           Log.d("App", "Tags updated successfully");
 *           Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
 *       } else {
 *           Log.e("App", "Failed to update tags", result.getException());
 *           Toast.makeText(this, "Update failed. Please try again", Toast.LENGTH_SHORT).show();
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Example with user ID management:</b>
 * <pre>
 * {@code
 *   // Set user ID with callback
 *   Pushwoosh.getInstance().setUserId("user_12345", (result) -> {
 *       if (result.isSuccess()) {
 *           Log.d("App", "User ID set successfully");
 *           // Proceed with app initialization
 *           initializeUserSession();
 *       } else {
 *           Log.e("App", "Failed to set user ID", result.getException());
 *           // Retry or show error dialog
 *           showRetryDialog();
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Example with retry logic:</b>
 * <pre>
 * {@code
 *   private void registerWithRetry(int attemptNumber) {
 *       Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *           if (result.isSuccess()) {
 *               Log.d("App", "Registration successful");
 *           } else {
 *               if (attemptNumber < MAX_RETRIES) {
 *                   Log.w("App", "Registration failed, retrying... Attempt " + (attemptNumber + 1));
 *                   new Handler().postDelayed(() -> registerWithRetry(attemptNumber + 1), 5000);
 *               } else {
 *                   Log.e("App", "Registration failed after " + MAX_RETRIES + " attempts");
 *                   showErrorDialog("Unable to register for notifications");
 *               }
 *           }
 *       });
 *   }
 * }
 * </pre>
 * <br>
 * <b>Example with loading indicator:</b>
 * <pre>
 * {@code
 *   // Show loading indicator during operation
 *   showLoadingIndicator();
 *
 *   Pushwoosh.getInstance().getTags((result) -> {
 *       hideLoadingIndicator();
 *
 *       if (result.isSuccess()) {
 *           TagsBundle tags = result.getData();
 *           String userName = tags.getString("Name", "Unknown");
 *           int userAge = tags.getInt("Age", 0);
 *           updateUserProfile(userName, userAge);
 *       } else {
 *           Log.e("App", "Failed to load tags", result.getException());
 *           showError("Unable to load user profile");
 *       }
 *   });
 * }
 * </pre>
 *
 * @param <T> the type of data returned on successful operation completion
 * @param <E> the type of exception returned on operation failure, must extend {@link PushwooshException}
 * @see Result
 * @see PushwooshException
 */
public interface Callback<T, E extends PushwooshException> {
	/**
	 * Handles the result of an asynchronous operation.
	 * <p>
	 * This method is invoked on the main (UI) thread when an asynchronous SDK operation completes,
	 * regardless of whether it succeeded or failed. The {@link Result} parameter encapsulates either
	 * the successful result data or an exception describing the failure.
	 * <p>
	 * <b>Thread safety:</b> This method is always called on the main thread, making it safe to update
	 * UI components directly without posting to a handler or using runOnUiThread().
	 * <p>
	 * <b>Usage pattern:</b>
	 * <ul>
	 * <li>Check {@link Result#isSuccess()} to determine if operation succeeded</li>
	 * <li>Use {@link Result#getData()} to access successful result (may be null for Void operations)</li>
	 * <li>Use {@link Result#getException()} to access error information on failure</li>
	 * </ul>
	 * <br>
	 * <b>Example with push registration:</b>
	 * <pre>
	 * {@code
	 *   @Override
	 *   public void process(@NonNull Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result) {
	 *       if (result.isSuccess()) {
	 *           RegisterForPushNotificationsResultData data = result.getData();
	 *           String pushToken = data.getToken();
	 *           boolean notificationsEnabled = data.isEnabled();
	 *
	 *           Log.d("App", "Push token: " + pushToken);
	 *           Log.d("App", "Notifications enabled: " + notificationsEnabled);
	 *
	 *           // Update UI
	 *           statusTextView.setText("Push notifications enabled");
	 *       } else {
	 *           RegisterForPushNotificationsException exception = result.getException();
	 *           Log.e("App", "Registration failed: " + exception.getMessage(), exception);
	 *
	 *           // Show error to user
	 *           AlertDialog.Builder builder = new AlertDialog.Builder(this);
	 *           builder.setTitle("Registration Failed")
	 *               .setMessage("Unable to register for push notifications: " + exception.getMessage())
	 *               .setPositiveButton("Retry", (dialog, which) -> retryRegistration())
	 *               .setNegativeButton("Cancel", null)
	 *               .show();
	 *       }
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with tags management:</b>
	 * <pre>
	 * {@code
	 *   @Override
	 *   public void process(@NonNull Result<Void, PushwooshException> result) {
	 *       if (result.isSuccess()) {
	 *           // result.getData() is null for Void operations
	 *           Log.d("App", "Tags set successfully");
	 *           Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
	 *       } else {
	 *           PushwooshException exception = result.getException();
	 *           Log.e("App", "Failed to set tags: " + exception.getMessage(), exception);
	 *
	 *           // Determine error type and respond accordingly
	 *           if (exception.getMessage().contains("network")) {
	 *               Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_LONG).show();
	 *           } else {
	 *               Toast.makeText(this, "Update failed. Please try again", Toast.LENGTH_SHORT).show();
	 *           }
	 *       }
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with getting tags:</b>
	 * <pre>
	 * {@code
	 *   @Override
	 *   public void process(@NonNull Result<TagsBundle, GetTagsException> result) {
	 *       if (result.isSuccess()) {
	 *           TagsBundle tags = result.getData();
	 *
	 *           // Safely access tag values with defaults
	 *           String userName = tags.getString("Name", "Guest");
	 *           int userAge = tags.getInt("Age", 0);
	 *           String subscription = tags.getString("Subscription", "free");
	 *
	 *           Log.d("App", "User: " + userName + ", Age: " + userAge + ", Plan: " + subscription);
	 *
	 *           // Update UI with user data
	 *           nameTextView.setText(userName);
	 *           subscriptionBadge.setText(subscription.toUpperCase());
	 *       } else {
	 *           GetTagsException exception = result.getException();
	 *           Log.e("App", "Failed to get tags: " + exception.getMessage(), exception);
	 *           // Show error or use cached data
	 *           loadCachedUserData();
	 *       }
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with user ID operations:</b>
	 * <pre>
	 * {@code
	 *   @Override
	 *   public void process(@NonNull Result<Void, SetUserIdException> result) {
	 *       if (result.isSuccess()) {
	 *           Log.d("App", "User ID set successfully");
	 *           // Continue with user initialization
	 *           loadUserProfile();
	 *           syncUserData();
	 *       } else {
	 *           SetUserIdException exception = result.getException();
	 *           Log.e("App", "Failed to set user ID: " + exception.getMessage(), exception);
	 *
	 *           // Critical error - user tracking won't work
	 *           AlertDialog.Builder builder = new AlertDialog.Builder(this);
	 *           builder.setTitle("Setup Error")
	 *               .setMessage("Failed to initialize user tracking. Some features may not work correctly.")
	 *               .setPositiveButton("Retry", (dialog, which) -> retryUserIdSetup())
	 *               .setNegativeButton("Continue Anyway", (dialog, which) -> continueWithoutTracking())
	 *               .setCancelable(false)
	 *               .show();
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param result the result of the asynchronous operation, containing either success data or failure exception
	 */
	@MainThread
	void process(@NonNull Result<T, E> result);
}
