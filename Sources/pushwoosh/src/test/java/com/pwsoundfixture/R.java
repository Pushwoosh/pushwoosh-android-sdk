package com.pwsoundfixture;

/**
 * Test fixture standing in for an AGP-generated R class. The nested class compiles to
 * {@code com.pwsoundfixture.R$raw}, so {@code Class.forName} resolves it exactly like the real one.
 */
public final class R {
    public static final class raw {
        public static final int parent_package_sound = 0x7f0e0001;

        private raw() {}
    }

    private R() {}
}
