package com.pushwoosh.featureflags;

import com.pushwoosh.exception.PushwooshException;

/**
 * Failure of a feature flags refresh. The cached flags are kept.
 */
public class FeatureFlagsException extends PushwooshException {
    private final int httpStatus;

    public FeatureFlagsException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * @return HTTP status of the failed request: {@code 0} when no HTTP response was received (network error) or the
     *         refresh failed locally without sending a request; may be {@code 200} when the HTTP exchange succeeded
     *         but the response could not be used (unparseable body or an error status in the response envelope)
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
