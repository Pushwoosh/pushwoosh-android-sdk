package com.pwsoundfixture.deep;

/**
 * Test fixture standing in for an AGP-generated R class that sits deeper than a two-segment parent of the
 * applicationId. Lets a test assert that the parent walk probes the deepest parent first, whichever source it
 * came from — a shallow parent is the likelier library namespace.
 */
public final class R {
    public static final class raw {
        public static final int deep_package_sound = 0x7f0e0008;

        private raw() {}
    }

    private R() {}
}
