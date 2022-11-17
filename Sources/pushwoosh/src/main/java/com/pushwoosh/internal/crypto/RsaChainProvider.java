package com.pushwoosh.internal.crypto;

import javax.crypto.Cipher;

public class RsaChainProvider {

    public static int getEncryptionPadding(final String encryptionPadding, int keySize, Cipher cilpher) {
        if (cilpher.getBlockSize() != 0) {
            return cilpher.getBlockSize();
        }

        switch (encryptionPadding) {
            case CryptoKeyProperties.ENCRYPTION_PADDING_RSA_OAEP:
                return keySize / 8 - 66;
            case CryptoKeyProperties.ENCRYPTION_PADDING_RSA_PKCS1:
                return keySize / 8 - 11;
            default:
                return keySize / 8;
        }
    }
}
