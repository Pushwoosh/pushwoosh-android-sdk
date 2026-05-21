package com.pushwoosh.inapp.network.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InAppLayoutTest {

    // Verifies that an unknown code falls back to DIALOG.
    // Persistence contract used by InAppDbHelper:211 — legacy SQLite rows with null/empty/unknown
    // layout codes must map to DIALOG. A refactor that changes this default silently breaks
    // legacy in-app records on upgrade.
    @Test
    public void of_unknownCode_returnsDialog() {
        assertEquals(InAppLayout.DIALOG, InAppLayout.of("unknown_layout_xyz"));
    }
}
