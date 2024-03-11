package com.pushwoosh.exception;

public class NotificationIdNotFoundException extends PushwooshException{
    public NotificationIdNotFoundException(String description) {
        super(description);
    }
}
