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

import androidx.annotation.Nullable;

import com.pushwoosh.exception.PushwooshException;

/**
 * Encapsulates the result of an asynchronous operation, providing a type-safe way to handle both success and failure cases.
 * <p>
 * This class follows the Result pattern (also known as Either or Try pattern) to represent operations that can succeed
 * with data or fail with an exception. It ensures null-safety by guaranteeing that exactly one of the two fields
 * (data or exception) will be non-null, never both or neither.
 * <p>
 * <b>Key characteristics:</b>
 * <ul>
 * <li>Type-safe: Success data type (T) and error type (E) are explicitly defined</li>
 * <li>Null-safe: Either data or exception is non-null, but never both</li>
 * <li>Immutable: Once created, the result cannot be changed</li>
 * <li>Thread-safe: Can be safely passed between threads</li>
 * </ul>
 * <p>
 * <b>Usage pattern:</b> Always check {@link #isSuccess()} first to determine if the operation succeeded,
 * then access either {@link #getData()} for successful results or {@link #getException()} for errors.
 * <br><br>
 * <b>Basic success/failure handling:</b>
 * <pre>
 * {@code
 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *       if (result.isSuccess()) {
 *           // Safe to access data
 *           RegisterForPushNotificationsResultData data = result.getData();
 *           String token = data.getToken();
 *           Log.d("App", "Registered successfully. Token: " + token);
 *       } else {
 *           // Safe to access exception
 *           RegisterForPushNotificationsException exception = result.getException();
 *           Log.e("App", "Registration failed: " + exception.getMessage());
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Null-safe data handling (Void operations):</b>
 * <pre>
 * {@code
 *   // Some operations like setTags() return Void on success
 *   Pushwoosh.getInstance().setTags(tags, (result) -> {
 *       if (result.isSuccess()) {
 *           // result.getData() is null for Void operations
 *           Log.d("App", "Tags set successfully");
 *       } else {
 *           PushwooshException exception = result.getException();
 *           Log.e("App", "Failed to set tags: " + exception.getMessage());
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Detailed error handling with exception types:</b>
 * <pre>
 * {@code
 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *       if (result.isSuccess()) {
 *           String token = result.getData().getToken();
 *           saveTokenToPreferences(token);
 *       } else {
 *           Exception exception = result.getException();
 *
 *           // Log full exception with stack trace
 *           Log.e("App", "Registration failed", exception);
 *
 *           // Handle specific error types
 *           if (exception instanceof NetworkException) {
 *               showToast("Network error. Please check your connection.");
 *           } else if (exception.getMessage().contains("Play Services")) {
 *               showPlayServicesUpdateDialog();
 *           } else {
 *               showToast("Registration failed: " + exception.getMessage());
 *           }
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Working with nullable data (tags retrieval):</b>
 * <pre>
 * {@code
 *   Pushwoosh.getInstance().getTags((result) -> {
 *       if (result.isSuccess()) {
 *           TagsBundle tags = result.getData(); // Never null on success
 *
 *           // Safe access with default values
 *           String name = tags.getString("Name", "Guest");
 *           int age = tags.getInt("Age", 0);
 *           List<String> interests = tags.getList("Interests", Collections.emptyList());
 *
 *           Log.d("App", "User: " + name + ", Age: " + age);
 *           Log.d("App", "Interests: " + interests);
 *       } else {
 *           Log.e("App", "Failed to retrieve tags", result.getException());
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Conditional logic based on success/failure:</b>
 * <pre>
 * {@code
 *   Pushwoosh.getInstance().setUserId("user_12345", (result) -> {
 *       if (result.isSuccess()) {
 *           // Proceed with app initialization
 *           loadUserProfile();
 *           syncUserData();
 *           enablePushFeatures();
 *       } else {
 *           // Fallback to anonymous mode
 *           Log.w("App", "User ID not set, using anonymous mode", result.getException());
 *           enableAnonymousMode();
 *       }
 *   });
 * }
 * </pre>
 * <br>
 * <b>Logging best practices:</b>
 * <pre>
 * {@code
 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
 *       if (result.isSuccess()) {
 *           RegisterForPushNotificationsResultData data = result.getData();
 *           Log.d("App", "Push registration successful");
 *           Log.d("App", "  Token: " + data.getToken());
 *           Log.d("App", "  Notifications enabled: " + data.isEnabled());
 *
 *           // Send to analytics
 *           Analytics.logEvent("push_registered", "token", data.getToken());
 *       } else {
 *           Exception exception = result.getException();
 *           // Log with exception for full stack trace
 *           Log.e("App", "Push registration failed: " + exception.getMessage(), exception);
 *
 *           // Send to crash reporting
 *           Crashlytics.recordException(exception);
 *       }
 *   });
 * }
 * </pre>
 *
 * @param <T> the type of data returned on successful operation completion
 * @param <E> the type of exception returned on operation failure, must extend {@link PushwooshException}
 * @see Callback
 * @see PushwooshException
 */
public class Result<T, E extends PushwooshException> {
	private final T data;
	private final E exception;

	private Result(T data, E exception) {
		this.data = data;
		this.exception = exception;
	}

	/**
	 * Creates a successful result containing the specified data.
	 * <p>
	 * This factory method constructs a Result instance representing a successful operation.
	 * The created result will have {@link #isSuccess()} return true, {@link #getData()} return
	 * the provided data, and {@link #getException()} return null.
	 * <p>
	 * <b>Note:</b> This is an internal SDK method. Application developers typically don't need to
	 * create Result instances directly as they are provided by SDK callback methods.
	 *
	 * @param data the data to wrap in a successful result (may be null for Void operations)
	 * @param <T>  the type of success data
	 * @param <E>  the type of exception (not used in successful results)
	 * @return a Result instance representing a successful operation with the given data
	 */
	public static <T, E extends PushwooshException> Result<T, E> fromData(T data) {
		return new Result<>(data, null);
	}

	/**
	 * Creates a failed result containing the specified exception.
	 * <p>
	 * This factory method constructs a Result instance representing a failed operation.
	 * The created result will have {@link #isSuccess()} return false, {@link #getData()} return null,
	 * and {@link #getException()} return the provided exception.
	 * <p>
	 * <b>Note:</b> This is an internal SDK method. Application developers typically don't need to
	 * create Result instances directly as they are provided by SDK callback methods.
	 *
	 * @param exception the exception describing the failure (must not be null)
	 * @param <T>       the type of success data (not used in failed results)
	 * @param <E>       the type of exception
	 * @return a Result instance representing a failed operation with the given exception
	 */
	public static <T, E extends PushwooshException> Result<T, E> fromException(E exception) {
		return new Result<>(null, exception);
	}

	/**
	 * Creates a result with both data and exception fields.
	 * <p>
	 * This factory method constructs a Result instance that may contain either data, exception, or both.
	 * It provides flexibility for edge cases where both values might be present.
	 * <p>
	 * <b>Note:</b> This is an internal SDK method. Application developers typically don't need to
	 * create Result instances directly as they are provided by SDK callback methods.
	 *
	 * @param data      the success data (may be null)
	 * @param exception the failure exception (may be null)
	 * @param <T>       the type of success data
	 * @param <E>       the type of exception
	 * @return a Result instance with the given data and exception
	 */
	public static <T, E extends PushwooshException> Result<T, E> from(T data, E exception) {
		return new Result<>(data, exception);
	}

	/**
	 * Checks whether the asynchronous operation completed successfully.
	 * <p>
	 * This method determines success by checking if the exception field is null. A null exception
	 * indicates that the operation completed without errors.
	 * <p>
	 * <b>Usage pattern:</b> Always call this method first before accessing {@link #getData()} or
	 * {@link #getException()} to determine which accessor to use.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   if (result.isSuccess()) {
	 *       // Operation succeeded - safe to get data
	 *       String token = result.getData().getToken();
	 *   } else {
	 *       // Operation failed - safe to get exception
	 *       Exception error = result.getException();
	 *       Log.e("App", "Error: " + error.getMessage());
	 *   }
	 * }
	 * </pre>
	 *
	 * @return true if the operation completed successfully (exception is null), false otherwise
	 */
	public boolean isSuccess() {
		return exception == null;
	}

	/**
	 * Returns the success data from the operation.
	 * <p>
	 * This method provides access to the data returned by a successful asynchronous operation.
	 * The return value is guaranteed to be non-null when {@link #isSuccess()} returns true,
	 * except for operations that return Void where the data will be null even on success.
	 * <p>
	 * <b>Null safety:</b>
	 * <ul>
	 * <li>Non-null when operation succeeded and returned data (e.g., push token, tags)</li>
	 * <li>Null when operation failed ({@link #isSuccess()} returns false)</li>
	 * <li>Null for successful Void operations (e.g., setTags, setUserId)</li>
	 * </ul>
	 * <p>
	 * <b>Important:</b> Always check {@link #isSuccess()} before calling this method to ensure
	 * proper null handling and to distinguish between failed operations and successful Void operations.
	 * <br><br>
	 * <b>Example with data-returning operation:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
	 *       if (result.isSuccess()) {
	 *           RegisterForPushNotificationsResultData data = result.getData(); // Non-null
	 *           String token = data.getToken();
	 *           Log.d("App", "Token: " + token);
	 *       }
	 *   });
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with Void operation:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().setTags(tags, (result) -> {
	 *       if (result.isSuccess()) {
	 *           // result.getData() is null for Void operations
	 *           Log.d("App", "Tags set successfully");
	 *       }
	 *   });
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with getTags operation:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().getTags((result) -> {
	 *       if (result.isSuccess()) {
	 *           TagsBundle tags = result.getData(); // Non-null on success
	 *           String name = tags.getString("Name", "Unknown");
	 *           int age = tags.getInt("Age", 0);
	 *           Log.d("App", "Name: " + name + ", Age: " + age);
	 *       }
	 *   });
	 * }
	 * </pre>
	 *
	 * @return the operation result data if successful, null if the operation failed or for successful Void operations
	 */
	@Nullable
	public T getData() {
		return data;
	}

	/**
	 * Returns the exception that caused the operation to fail.
	 * <p>
	 * This method provides access to the exception thrown during an asynchronous operation.
	 * The return value is guaranteed to be non-null when {@link #isSuccess()} returns false,
	 * and null when the operation succeeded.
	 * <p>
	 * <b>Null safety:</b>
	 * <ul>
	 * <li>Non-null when operation failed ({@link #isSuccess()} returns false)</li>
	 * <li>Null when operation succeeded ({@link #isSuccess()} returns true)</li>
	 * </ul>
	 * <p>
	 * The exception provides detailed information about the failure, including an error message
	 * and potentially a stack trace. Different SDK operations return specific exception types
	 * that extend {@link PushwooshException}.
	 * <p>
	 * <b>Important:</b> Always check {@link #isSuccess()} before calling this method to ensure
	 * you're only accessing exceptions for failed operations.
	 * <br><br>
	 * <b>Example with basic error logging:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
	 *       if (!result.isSuccess()) {
	 *           Exception exception = result.getException(); // Non-null on failure
	 *           Log.e("App", "Registration failed: " + exception.getMessage());
	 *       }
	 *   });
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with full stack trace logging:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().setTags(tags, (result) -> {
	 *       if (!result.isSuccess()) {
	 *           PushwooshException exception = result.getException();
	 *           // Log with full stack trace for debugging
	 *           Log.e("App", "Failed to set tags", exception);
	 *
	 *           // Or use exception methods
	 *           Log.e("App", "Error message: " + exception.getMessage());
	 *           Log.e("App", "Error cause: " + exception.getCause());
	 *       }
	 *   });
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with exception type checking:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().registerForPushNotifications((result) -> {
	 *       if (!result.isSuccess()) {
	 *           Exception exception = result.getException();
	 *
	 *           // Check for specific exception types
	 *           if (exception instanceof NetworkException) {
	 *               Log.e("App", "Network error - check internet connection");
	 *               showRetryDialog();
	 *           } else if (exception.getMessage().contains("Play Services")) {
	 *               Log.e("App", "Google Play Services error");
	 *               promptUpdatePlayServices();
	 *           } else {
	 *               Log.e("App", "Unknown error: " + exception.getMessage());
	 *               showGenericErrorDialog();
	 *           }
	 *       }
	 *   });
	 * }
	 * </pre>
	 * <br>
	 * <b>Example with user feedback:</b>
	 * <pre>
	 * {@code
	 *   Pushwoosh.getInstance().setUserId("user_123", (result) -> {
	 *       if (!result.isSuccess()) {
	 *           SetUserIdException exception = result.getException();
	 *
	 *           // Log for debugging
	 *           Log.e("App", "Failed to set user ID", exception);
	 *
	 *           // Show user-friendly message
	 *           String message = "Unable to set user ID: " + exception.getMessage();
	 *           Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	 *
	 *           // Report to crash analytics
	 *           Crashlytics.recordException(exception);
	 *       }
	 *   });
	 * }
	 * </pre>
	 *
	 * @return the exception that caused the operation to fail, or null if the operation succeeded
	 */
	@Nullable
	public E getException() {
		return exception;
	}
}
