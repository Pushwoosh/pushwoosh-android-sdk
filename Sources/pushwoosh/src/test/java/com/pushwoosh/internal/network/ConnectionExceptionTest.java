package com.pushwoosh.internal.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConnectionExceptionTest {

    // Verifies that a failure with both statuses left at 0 is transient — the sentinel for a dropped connection.
    @Test
    public void isTransient_bothStatusesZero_returnsTrue() {
        ConnectionException exception = new ConnectionException("Connection failed", 0, 0);

        assertTrue("both statuses 0 means connection failure", exception.isTransient());
    }

    // Verifies that every HTTP status in the temporary-failure table is reported as transient.
    @Test
    public void isTransient_retriableHttpStatuses_returnTrue() {
        int[] retriableCodes = {408, 429, 500, 502, 503, 504};

        for (int code : retriableCodes) {
            ConnectionException exception = new ConnectionException("Server error", code, 0);

            assertTrue("HTTP " + code + " should be transient", exception.isTransient());
        }
    }

    // Verifies that client-side rejections are terminal — the server answered, a retry would answer the same.
    @Test
    public void isTransient_clientErrors_returnFalse() {
        int[] terminalCodes = {400, 401, 403, 404};

        for (int code : terminalCodes) {
            ConnectionException exception = new ConnectionException("Client error", code, 0);

            assertFalse("HTTP " + code + " should be terminal", exception.isTransient());
        }
    }

    // Verifies that a retriable HTTP status wins over a non-zero pushwoosh status.
    @Test
    public void isTransient_retriableHttpStatusWithPushwooshStatus_returnsTrue() {
        ConnectionException exception = new ConnectionException("Service Unavailable", 503, 42);

        assertTrue("503 with pushwoosh status 42 is still transient", exception.isTransient());
    }

    // Verifies that a pushwoosh-level error without an HTTP status is terminal.
    // The input is synthetic on purpose: PushwooshRequestManager assigns the HTTP status before it ever
    // parses the envelope, so in production a non-zero pushwoosh status always arrives with a non-zero
    // HTTP one. The case exists to pin both operands of the sentinel conjunction — without it the
    // mutation `pushwooshStatusCode == 0 || statusCode == 0` survives.
    @Test
    public void isTransient_pushwooshStatusWithoutHttpStatus_returnsFalse() {
        ConnectionException exception = new ConnectionException("Pushwoosh error", 0, 42);

        assertFalse("pushwoosh error without HTTP status is terminal", exception.isTransient());
    }
}
