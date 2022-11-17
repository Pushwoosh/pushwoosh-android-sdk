package com.pushwoosh.testingapp;

/**
 * Created by etkachenko on 1/20/17.
 */

public class RegisterRecieversMessage {
	private Boolean registerRecievers;

	public RegisterRecieversMessage(Boolean registerRecievers) {
		this.registerRecievers = registerRecievers;
	}

	public final Boolean getMessage() {
		return registerRecievers;
	}
}


