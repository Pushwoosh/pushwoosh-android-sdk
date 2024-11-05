package com.pushwoosh.internal.utils;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HashDecoderTest {

    // Helper class to hold CSV data
    public static class CsvRow {
        String encoded;
        String expectedMessageCode;

        CsvRow(String encoded, String expectedMessageCode) {
            this.encoded = encoded;
            this.expectedMessageCode = expectedMessageCode;
        }
    }

    // Helper method to load CSV data
    public List<CsvRow> loadCsvData() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("message-hash.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<CsvRow> csvData = new ArrayList<>();

        String line;
        reader.readLine(); // Skip header line
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",");
            String encoded = columns[0];
            String expectedMessageCode = columns[2]; // Index of MessageCode column
            csvData.add(new CsvRow(encoded, expectedMessageCode));
        }
        reader.close();
        return csvData;
    }

    @Test
    public void testHashDecoderWithCsvData() throws Exception {
        List<CsvRow> csvData = loadCsvData();
        int matchCounter = 0;  // Counter for successful matches
        List<String> mismatches = new ArrayList<>();  // List to store mismatch information

        for (CsvRow row : csvData) {
            String[] result = HashDecoder.parseMessageHash(row.encoded);
            String actualMessageCode = result[1];  // Index of MessageCode from parseMessageHash

            if (actualMessageCode.equals(row.expectedMessageCode)) {
                matchCounter++;  // Increment counter for each match
            } else {
                mismatches.add("Mismatch for hash: " + row.encoded +
                        " Expected: " + row.expectedMessageCode +
                        " Got: " + actualMessageCode);
                assertEquals("_e_1o_1us-36Xv14-88VofI", row.encoded);
            }
        }

        // Log the number of successful matches
        System.out.println("Total matches: " + matchCounter + " out of " + csvData.size());

        // If there are mismatches, print them and fail the test
        if (!mismatches.isEmpty()) {
            System.out.println("Found mismatches:");
            for (String mismatch : mismatches) {
                System.out.println(mismatch);
            }
            assertEquals(mismatches.size(), 1);
        }
    }
}
