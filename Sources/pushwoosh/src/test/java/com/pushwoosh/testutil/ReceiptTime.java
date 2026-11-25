package com.pushwoosh.testutil;

/**
 * Fixed push receipt time shared by delivery timestamp tests.
 */
public final class ReceiptTime {
    public static final long RECEIVED_AT_MILLIS = 1795000000000L;
    public static final long RECEIVED_AT_SECONDS = 1795000000L;
    public static final String RECEIVED_AT_RFC3339 = "2026-11-18T11:06:40Z";
    public static final long MINUTE_LATER_MILLIS = RECEIVED_AT_MILLIS + 60_000L;
    public static final String MINUTE_LATER_RFC3339 = "2026-11-18T11:07:40Z";

    public static final long ON_MINUTE_SECONDS = 1795000020L;
    public static final String ON_MINUTE_RFC3339 = "2026-11-18T11:07:00Z";

    private ReceiptTime() {}
}
