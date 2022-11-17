package com.pushwoosh.internal.event;

public class Subscription<T extends Event> {
	private final Class<T> eventClass;
	private final EventListener<T> eventListener;

	Subscription(final Class<T> eventClass, final EventListener<T> eventListener) {
		this.eventClass = eventClass;
		this.eventListener = eventListener;
	}

	public void unsubscribe() {
		EventBus.unsubscribe(eventClass, eventListener);
	}
}