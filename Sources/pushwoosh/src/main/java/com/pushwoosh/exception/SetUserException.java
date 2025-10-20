package com.pushwoosh.exception;

/**
 * Exception thrown when setting user attributes fails.
 * <p>
 * Returned via {@link com.pushwoosh.function.Callback} when {@link com.pushwoosh.Pushwoosh#setUser(String, com.pushwoosh.function.Callback)} fails.
 * Common causes: network errors, invalid user ID.
 *
 * @see com.pushwoosh.Pushwoosh#setUser(String, com.pushwoosh.function.Callback)
 */
public class SetUserException extends PushwooshException {
    public SetUserException(String description) {
        super(description);
    }
}
