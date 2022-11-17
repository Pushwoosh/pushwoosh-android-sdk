package com.pushwoosh.internal.crypto;

import android.util.Base64;

import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class RsaEncryptor  {



    public String encrypt(final byte[] value, final PublicKey publicKey, String encryptionPadding) throws Exception {
        final Cipher cipher = Cipher.getInstance(CryptoKeyProperties.getRsaTransformation(encryptionPadding));
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytes = generateEncryptedResult(getKeySize(publicKey), value, cipher, encryptionPadding);
        return new String(Base64.encode(bytes, Base64.DEFAULT)).replaceAll("(\\r|\\n)", "");

    }

    private int getKeySize(final PublicKey publicKey) {
        if (publicKey instanceof RSAKey) {
            return ((RSAKey) publicKey).getModulus().bitLength();
        }

        throw new IllegalArgumentException("PublicKey should be instance of RsaKey");
    }

    private byte[] generateEncryptedResult(int keySize, final byte[] input, final Cipher cipher, final String encryptionPadding) throws IllegalBlockSizeException, BadPaddingException {
        int blockSize = RsaChainProvider.getEncryptionPadding(encryptionPadding, keySize, cipher);
        byte[] result = new byte[keySize / 8 * (input.length / blockSize + (input.length % blockSize > 0 ? 1 : 0))];
        int prevCount = 0;
        for (int i = 0; i < input.length; i += blockSize) {
            byte[] src = cipher.doFinal(Arrays.copyOfRange(input, i, Math.min(i + blockSize, input.length)));
            System.arraycopy(src, 0, result, prevCount, src.length);
            prevCount += src.length;
        }

        return result;
    }
}
