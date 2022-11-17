package com.pushwoosh.testingapp;

import com.squareup.otto.Bus;

/**
 * Created by etkachenko on 1/19/17.
 */

public class BusStation {
	private static Bus bus = new Bus();

	public static synchronized Bus getBus() {
		if (bus == null) {
			bus = new Bus();
		}
		return bus;
	}
}
