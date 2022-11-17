package com.pushwoosh.internal.event;

import java.util.LinkedList;
import java.util.List;

public abstract class Emitter<T extends Event> {
	@SuppressWarnings("WeakerAccess")
	protected EventListener<T> listener;

	public void bind(EventListener<T> listener) {
		this.listener = listener;
	}

	public void unbind() {
		this.listener = null;
	}

	@SuppressWarnings("WeakerAccess")
	protected void emit(T dataEvent) {
		if (listener != null) {
			listener.onReceive(dataEvent);
		}
	}

	public static <T extends Event> Emitter<T> when(Emitter<T> source, Emitter<?> condition) {
		return new Emitter<T>() {
			boolean conditional = false;
			List<T> buffer = new LinkedList<>();

			@Override
			public void bind(EventListener<T> listener) {
				super.bind(listener);

				source.bind(event -> {
					if (conditional) {
						emit(event);
					} else {
						buffer.add(event);
					}
				});

				condition.bind(event -> {
					conditional = true;
					for (T dataEvent1 : buffer) {
						emit(dataEvent1);
					}
				});
			}
		};
	}

	public static <T extends Event> Emitter<T> forEvent(Class<T> event) {
		return new Emitter<T>() {
			@Override
			public void bind(EventListener<T> listener) {
				super.bind(listener);

				EventBus.subscribe(event, listener);
			}

			@Override
			public void unbind() {
				EventBus.unsubscribe(event, listener);

				super.unbind();
			}
		};
	}
}
