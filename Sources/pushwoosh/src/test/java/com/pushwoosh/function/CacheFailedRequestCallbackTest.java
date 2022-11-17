package com.pushwoosh.function;

import com.pushwoosh.internal.network.ConnectionException;

import org.junit.Assert;
import org.junit.Test;

public class CacheFailedRequestCallbackTest {

    @Test
    public void needToRetryTest() {
        CacheFailedRequestCallback callback = new CacheFailedRequestCallback(null, null);

        // pushwoosh status 200
        ConnectionException exception = new ConnectionException(null, 200, 200);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 300
        exception = new ConnectionException(null, 200, 300);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 400
        exception = new ConnectionException(null, 200, 400);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 429
        exception = new ConnectionException(null, 200, 429);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 500
        exception = new ConnectionException(null, 200, 500);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 600
        exception = new ConnectionException(null, 200, 600);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 0
        exception = new ConnectionException(null, 200, 0);
        Assert.assertFalse(callback.needToRetry(exception));

        // pushwoosh status 0 network status 0 (no internet)
        exception = new ConnectionException(null, 0, 0);
        Assert.assertTrue(callback.needToRetry(exception));

        // pushwoosh status -100
        exception = new ConnectionException(null, 200, -100);
        Assert.assertFalse(callback.needToRetry(exception));
    }
}
