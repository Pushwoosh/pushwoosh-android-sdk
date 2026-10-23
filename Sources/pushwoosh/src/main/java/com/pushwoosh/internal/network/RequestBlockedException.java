package com.pushwoosh.internal.network;

/**
 * A request that was refused before it reached the transport: no address could be resolved from the
 * configured roots, or a local gate ("remove all device data", paused server communication, a missing
 * or unusable reverse proxy) turned it away.
 *
 * <p>Nothing left the device, so a caller that records "when did we last talk to the backend" — and
 * throttles on that record — must not count it as a send.
 */
public class RequestBlockedException extends NetworkException {
    public RequestBlockedException(String description) {
        super(description);
    }
}
