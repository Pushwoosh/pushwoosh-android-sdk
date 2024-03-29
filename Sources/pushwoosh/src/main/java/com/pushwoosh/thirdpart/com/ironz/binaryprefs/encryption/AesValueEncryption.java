/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.encryption;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation of {@link ValueEncryption} class which uses AES for <code>byte[]</code> data encryption.
 */
public final class AesValueEncryption implements ValueEncryption {

	private static final String SHORT_KEYS_MESSAGE = "Secret and initial vector must be 16 bytes";

	private static final String AES = "AES";
	private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5PADDING";
	private static final int KEY_LENGTH = 16;
	private final byte[] secretKeyBytes;
	private final byte[] initialVector;

	/**
	 * Creates byte encryption instance which performs AES vice versa encryption operation.
	 *
	 * @param secretKeyBytes 16 bytes secret key
	 * @param initialVector  16 bytes initial vector
	 */
	public AesValueEncryption(byte[] secretKeyBytes, byte[] initialVector) {
		checkLength(secretKeyBytes, initialVector);
		this.secretKeyBytes = secretKeyBytes;
		this.initialVector = initialVector;
	}

	private void checkLength(byte[] secretKeyBytes, byte[] initialVector) {
		if (secretKeyBytes.length != KEY_LENGTH || initialVector.length != KEY_LENGTH) {
			throw new EncryptionException(SHORT_KEYS_MESSAGE);
		}
	}

	@Override
	public byte[] encrypt(byte[] bytes) {
		return encryptInternal(bytes);
	}

	@Override
	public byte[] decrypt(byte[] bytes) {
		return decryptInternal(bytes);
	}

	private byte[] encryptInternal(byte[] bytes) {
		try {
			Cipher encryptCipher = createEncryptCipher();
			return encryptCipher.doFinal(bytes);
		} catch (Exception e) {
			throw new EncryptionException(e);
		}
	}

	private byte[] decryptInternal(byte[] bytes) {
		try {
			Cipher decryptCipher = createDecryptCipher();
			return decryptCipher.doFinal(bytes);
		} catch (Exception e) {
			throw new EncryptionException(e);
		}
	}

	private Cipher createEncryptCipher() throws Exception {
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, AES);
		IvParameterSpec iv = new IvParameterSpec(initialVector);
		Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
		return cipher;
	}

	private Cipher createDecryptCipher() throws Exception {
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, AES);
		IvParameterSpec iv = new IvParameterSpec(initialVector);
		Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
		return cipher;
	}
}