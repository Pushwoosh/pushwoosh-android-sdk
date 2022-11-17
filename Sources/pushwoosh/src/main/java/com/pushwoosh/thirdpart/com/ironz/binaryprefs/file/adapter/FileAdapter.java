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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.adapter;

/**
 * Adapter abstraction which describes a file operation contract.
 * It's used for making unique implementation for concrete file operations.
 */
public interface FileAdapter {

	/**
	 * Returns all names for directory.
	 *
	 * @return file names with extension suffix
	 */
	String[] names();

	/**
	 * Returns byte array for concrete file by name or empty byte array if exception throws.
	 *
	 * @param name file name with extension
	 * @return byte array of file
	 */
	byte[] fetch(String name);

	/**
	 * Saves byte array to concrete file, if file exists it will be overwritten.
	 *
	 * @param name  file name with extension
	 * @param bytes byte array for saving
	 */
	void save(String name, byte[] bytes);

	/**
	 * Removes file by key.
	 *
	 * @param name file name with extension
	 */
	void remove(String name);
}