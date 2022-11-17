package com.pushwoosh.internal.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;
import java.util.List;

/**
 * This class used for {@link com.pushwoosh.repository.SendTagsProcessor}. And this class is fixed
 * bug on Unity where each tag is sent by separate request. This class is accumulated requests and
 * sending it in one single request
 */
public class Accumulator<T> {
	public interface Completion<W> {
		void onAccumulated(List<W> data);
	}

	private final List<T> accumulatedData = new LinkedList<>();
	private final int delayMs;
	private final Completion<T> completion;
	private final Handler handler;

	public Accumulator(Completion<T> completion, int timeoutMs) {
		this.delayMs = timeoutMs;
		this.completion = completion;
		this.handler = new Handler(Looper.getMainLooper());
	}

	public void accumulate(T data) {
		synchronized (accumulatedData) {
			if (accumulatedData.isEmpty()) {
				handler.postDelayed(() -> {
					List<T> processingData = new LinkedList<>();
					synchronized (accumulatedData) {
						processingData.addAll(accumulatedData);
						accumulatedData.clear();
					}

					completion.onAccumulated(processingData);
				}, delayMs);
			}
			accumulatedData.add(data);
		}
	}
}
