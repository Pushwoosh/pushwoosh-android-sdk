package com.pushwoosh.internal.event;


public class DataEvent<T> implements Event {
	private final T data;

	public DataEvent(T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}
}
