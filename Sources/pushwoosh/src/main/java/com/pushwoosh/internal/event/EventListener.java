package com.pushwoosh.internal.event;

import androidx.annotation.MainThread;

@FunctionalInterface
public interface EventListener<T extends Event> {
	/**
	 * DataEvent handler. Is executed on main thread.
	 *
	 */
	@MainThread
	void onReceive(T event);
}
