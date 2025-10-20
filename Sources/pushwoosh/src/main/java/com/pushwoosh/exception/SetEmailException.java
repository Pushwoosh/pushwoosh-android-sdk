package com.pushwoosh.exception;

/**
 * Exception thrown when setting user email fails.
 * <p>
 * Returned via {@link com.pushwoosh.function.Callback} when {@link com.pushwoosh.Pushwoosh#setEmail(String, com.pushwoosh.function.Callback)} fails.
 * Common causes: network errors, invalid email format.
 *
 * @see com.pushwoosh.Pushwoosh#setEmail(String, com.pushwoosh.function.Callback)
 */
public class SetEmailException extends PushwooshException {
    public SetEmailException(String message) {
        super(message);
    }
}
