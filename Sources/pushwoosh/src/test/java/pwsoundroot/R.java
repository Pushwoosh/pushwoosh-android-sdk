package pwsoundroot;

/**
 * Test fixture in a single-segment package. The parent-package walk must stop before reaching it,
 * so any test that finds {@code too_shallow_sound} has caught a broken floor.
 */
public final class R {
    public static final class raw {
        public static final int too_shallow_sound = 0x7f0e0004;

        private raw() {}
    }

    private R() {}
}
