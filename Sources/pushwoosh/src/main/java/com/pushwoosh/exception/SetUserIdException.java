package com.pushwoosh.exception;

/**
 * Exception thrown when setting user ID fails.
 * <p>
 * Returned via {@link com.pushwoosh.function.Callback} when {@link com.pushwoosh.Pushwoosh#setUserId(String, com.pushwoosh.function.Callback)} fails.
 * Common causes: network errors, invalid user ID format.
 *
 * @see com.pushwoosh.Pushwoosh#setUserId(String, com.pushwoosh.function.Callback)
 */
public class SetUserIdException extends PushwooshException {
    public SetUserIdException(String message) {
        super(message);
    }
}
