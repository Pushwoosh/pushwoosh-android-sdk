package com.pushwoosh.internal.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HttpResponseTest {

    // Verifies that HTTP status 399 (just below the error range) is not classified as an error.
    @Test
    public void isErrorCode_399_returnsFalse() {
        assertFalse(HttpResponse.isErrorCode(399));
    }

    // Verifies that HTTP status 400 (lower error boundary) is classified as an error.
    @Test
    public void isErrorCode_400_returnsTrue() {
        assertTrue(HttpResponse.isErrorCode(400));
    }

    // Verifies that HTTP status 599 (upper error boundary, inclusive) is classified as an error.
    @Test
    public void isErrorCode_599_returnsTrue() {
        assertTrue(HttpResponse.isErrorCode(599));
    }

    // Verifies that HTTP status 600 (just above the error range) is not classified as an error.
    @Test
    public void isErrorCode_600_returnsFalse() {
        assertFalse(HttpResponse.isErrorCode(600));
    }
}
