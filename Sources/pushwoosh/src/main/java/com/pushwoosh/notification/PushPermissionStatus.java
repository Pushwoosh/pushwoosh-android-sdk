package com.pushwoosh.notification;

/**
 * Value of the {@code push_permission_status} request field and the pure rules that derive it.
 */
public final class PushPermissionStatus {
    public static final String GRANTED_EXPLICIT = "granted_explicit";
    public static final String GRANTED_DEFAULT = "granted_default";
    public static final String DENIED = "denied";
    public static final String NOT_DETERMINED = "not_determined";

    static final int RUNTIME_PERMISSION_API = 33;

    private PushPermissionStatus() {}

    public static String compute(
            int apiLevel,
            int targetSdk,
            boolean enabled,
            boolean grantedViaSdkPrompt,
            boolean everGranted,
            boolean rationaleSeen,
            boolean rationaleNow) {
        if (enabled) {
            return grantedViaSdkPrompt && isRuntimePermission(apiLevel, targetSdk) ? GRANTED_EXPLICIT : GRANTED_DEFAULT;
        }
        if (apiLevel < RUNTIME_PERMISSION_API) {
            return DENIED;
        }
        if (everGranted || rationaleSeen || rationaleNow) {
            return DENIED;
        }
        return NOT_DETERMINED;
    }

    /**
     * Whether a denied result of the SDK prompt means the SDK must not prompt again: the user's choice rather than
     * the dialog dismissed without one, or any denial when the OS owns the dialog (targetSdk &lt; 33).
     */
    public static boolean isDeniedByUser(
            int apiLevel, int targetSdk, boolean granted, boolean rationaleNow, boolean rationaleSeen) {
        if (granted) {
            return false;
        }
        // Without a runtime prompt the request is denied with no UI; asking again would only repeat that.
        if (!isRuntimePermission(apiLevel, targetSdk)) {
            return true;
        }
        // Dismissal leaves rationale false; so does a permanent denial, but only after a first one was seen.
        return rationaleNow || rationaleSeen;
    }

    public static boolean isGrantedViaSdkPrompt(
            int apiLevel, int targetSdk, boolean granted, boolean firstObservedGranted) {
        return granted && !firstObservedGranted && isRuntimePermission(apiLevel, targetSdk);
    }

    // With targetSdk < 33 the OS shows its own dialog, so SDK prompt results say nothing about the user.
    private static boolean isRuntimePermission(int apiLevel, int targetSdk) {
        return apiLevel >= RUNTIME_PERMISSION_API && targetSdk >= RUNTIME_PERMISSION_API;
    }
}
