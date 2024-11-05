package com.pushwoosh.internal.utils;

import java.util.HashMap;
import java.util.Map;

public class HashDecoder {

    private static final String HASH_DELIMITER = "_";
    private static final String MESSAGE_CODE_INNER_SPLITTER = "-";
    private static final int HASHING_BASE = 62;

    private static final int MESSAGE_CODE_FIRST_PART_LETTERS_COUNT = 4;
    private static final int MESSAGE_CODE_OTHER_PART_LETTERS_COUNT = 8;

    private static final Map<Character, Integer> alphabetRevert = new HashMap<>();
    private static final String[] alphabet = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    static {
        for (int i = 0; i < alphabet.length; i++) {
            alphabetRevert.put(alphabet[i].charAt(0), i);
        }
    }

    // Decodes the hash into message ID, message code, and campaign ID
    public static String[] parseMessageHash(String hash) {
        String[] parts = hash.split(HASH_DELIMITER);
        if (parts.length > 3) {
            long campaignID = alphabetDecode(parts[1]);
            long messageID = alphabetDecode(parts[2]);
            String messageCode = decodeMessageCode(parts[3]);
            return new String[]{String.valueOf(messageID), messageCode, String.valueOf(campaignID)};
        }
        return new String[]{"0", "", "0"};
    }

    // Decodes the message code, handling parts split by '-'
    public static String decodeMessageCode(String messageCode) {
        String[] parts = messageCode.split(MESSAGE_CODE_INNER_SPLITTER);
        if (parts.length == 1) {
            return messageCode;
        }

        for (int i = 0; i < parts.length; i++) {
            long decodedPart = alphabetDecode(parts[i]);
            String decodedHexPart = Long.toHexString(decodedPart).toUpperCase();
            boolean isFirstPart = (i == 0);
            parts[i] = prependZerosIfNeeded(isFirstPart, decodedHexPart);
        }

        return String.join(MESSAGE_CODE_INNER_SPLITTER, parts);
    }

    // Converts base-62 encoded hash string to long
    public static long alphabetDecode(String hash) {
        if (hash == null || hash.isEmpty()) {
            return 0;
        }

        long value = 0;
        for (int i = 0; i < hash.length(); i++) {
            value = value * HASHING_BASE + alphabetRevert.get(hash.charAt(i));
        }
        return value;
    }

    // Adds leading zeros if necessary to the hex string
    private static String prependZerosIfNeeded(boolean isFirstPart, String str) {
        int strLen = str.length();
        int normalPartLen = MESSAGE_CODE_OTHER_PART_LETTERS_COUNT;
        if (isFirstPart) {
            normalPartLen = MESSAGE_CODE_FIRST_PART_LETTERS_COUNT;
        }

        if (strLen >= normalPartLen) {
            return str;
        }

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < normalPartLen - strLen; i++) {
            prefix.append("0");
        }

        return prefix.toString() + str;
    }
}

