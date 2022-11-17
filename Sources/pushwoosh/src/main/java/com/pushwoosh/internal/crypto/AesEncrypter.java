package com.pushwoosh.internal.crypto;

import android.util.Base64;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncrypter {

    public String encrypt(byte[] key, byte[] ivBytes, String plainString) throws GeneralSecurityException {
        final byte[] encryptedBytes = plainString.getBytes();

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));

        final byte[] resultBytes = cipher.doFinal(encryptedBytes);
        byte[] reuslt = concatenateByteIVAndData(ivBytes, resultBytes);
        return new String(Base64.encode(reuslt, Base64.DEFAULT));
    }

    byte[] concatenateByteIVAndData(byte[] ivBytes, byte[] data) {
        byte[] result = new byte[ivBytes.length + data.length];
        System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
        System.arraycopy(data, 0, result, ivBytes.length, data.length);
        return result;
    }
}
