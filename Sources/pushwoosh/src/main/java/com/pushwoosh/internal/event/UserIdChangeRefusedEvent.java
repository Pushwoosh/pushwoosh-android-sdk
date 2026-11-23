package com.pushwoosh.internal.event;

/** Sent after a registerUser failure made the SDK revert the user id to its previous value. */
public class UserIdChangeRefusedEvent implements Event {

    public UserIdChangeRefusedEvent() {}
}
