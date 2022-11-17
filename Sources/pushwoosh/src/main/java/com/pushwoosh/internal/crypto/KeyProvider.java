package com.pushwoosh.internal.crypto;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

final class KeyProvider {

    static PublicKey getPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] byteKey = Base64.decode(key.getBytes(), Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }
}
