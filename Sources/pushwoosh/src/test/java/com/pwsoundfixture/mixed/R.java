package com.pwsoundfixture.mixed;

/**
 * Test fixture standing in for an R class that holds one name present in our resource table and two that are not
 * usable — a foreign name and an unreadable one. Lets a test assert that unusable fields are skipped one by one
 * without losing the rest of the enumeration, no matter what order {@code getFields()} happens to return.
 */
public final class R {
    public static final class raw {
        public static final int mixed_known_sound = 0x7f0e0005;

        public static final int mixed_foreign_sound = 0x7f0e0006;

        public static final int mixed_broken_sound = 0x7f0e0007;

        private raw() {}
    }

    private R() {}
}
