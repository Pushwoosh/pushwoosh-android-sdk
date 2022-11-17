package com.pushwoosh.internal.crypto;

public final class CryptoKeyProperties {
    /**
     * KEY_ALGORITHM_RSA PKCS#1 v1.5 padding scheme for encryption.
     */
    public static final String ENCRYPTION_PADDING_RSA_PKCS1 = "PKCS1Padding";

    /**
     * KEY_ALGORITHM_RSA Optimal Asymmetric Encryption Padding (OAEP) scheme.
     */
    public static final String ENCRYPTION_PADDING_RSA_OAEP = "OAEPPadding";

    public static final String KEY_ALGORITHM_RSA = "RSA";

    static final String RSA_PKCS1_TRANSFORMATION = KEY_ALGORITHM_RSA + "/ECB/" + ENCRYPTION_PADDING_RSA_PKCS1;

    static final String RSA_OAEP_TRANSFORMATION = KEY_ALGORITHM_RSA + "/ECB/OAEPWITHSHA-256ANDMGF1PADDING";

    public static String getRsaTransformation(final String encryptionPadding) {
        switch (encryptionPadding) {
            case ENCRYPTION_PADDING_RSA_PKCS1:
                return RSA_PKCS1_TRANSFORMATION;
            case ENCRYPTION_PADDING_RSA_OAEP:
                return RSA_OAEP_TRANSFORMATION;
            default:
                throw new IllegalArgumentException("Incorrect padding: " + encryptionPadding);
        }
    }

}

