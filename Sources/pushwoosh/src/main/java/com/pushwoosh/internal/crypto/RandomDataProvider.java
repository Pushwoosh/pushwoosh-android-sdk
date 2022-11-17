package com.pushwoosh.internal.crypto;

import androidx.annotation.NonNull;

import java.util.Random;

public class RandomDataProvider {

    private Random random;

    public RandomDataProvider(Random random) {
        this.random = random;
    }

    @NonNull
    public byte[] generate16Bit() {
        byte[] array = new byte[16];
        random.nextBytes(array);
        return array;
    }
}
