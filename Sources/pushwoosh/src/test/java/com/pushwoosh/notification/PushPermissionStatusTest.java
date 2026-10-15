package com.pushwoosh.notification;

import static com.pushwoosh.notification.PushPermissionStatus.DENIED;
import static com.pushwoosh.notification.PushPermissionStatus.GRANTED_DEFAULT;
import static com.pushwoosh.notification.PushPermissionStatus.GRANTED_EXPLICIT;
import static com.pushwoosh.notification.PushPermissionStatus.NOT_DETERMINED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PushPermissionStatusTest {

    private static String compute(
            int api, int target, boolean enabled, boolean viaPrompt, boolean everGranted, boolean rationaleSeen) {
        return PushPermissionStatus.compute(api, target, enabled, viaPrompt, everGranted, rationaleSeen, false);
    }

    @Test
    public void api32Enabled_isGrantedDefault() {
        assertEquals(GRANTED_DEFAULT, compute(32, 36, true, false, true, false));
    }

    @Test
    public void api31Disabled_isDenied() {
        assertEquals(DENIED, compute(31, 36, false, false, false, false));
    }

    @Test
    public void api33GrantedViaSdkPrompt_isGrantedExplicit() {
        assertEquals(GRANTED_EXPLICIT, compute(33, 33, true, true, true, false));
    }

    @Test
    public void api33GrantedWithoutSdkPrompt_isGrantedDefault() {
        // OS 12 -> 13 pre-grant, OS dialog with targetSdk < 33, or the app's own request
        assertEquals(GRANTED_DEFAULT, compute(33, 36, true, false, true, false));
    }

    @Test
    public void legacyTargetSdk_neverGrantedExplicit() {
        assertEquals(GRANTED_DEFAULT, compute(34, 32, true, true, true, false));
    }

    @Test
    public void api33NeverAsked_isNotDetermined() {
        assertEquals(NOT_DETERMINED, compute(33, 36, false, false, false, false));
    }

    @Test
    public void api33LegacyTargetDeniedOsDialog_isNotFalselyDenied() {
        assertEquals(NOT_DETERMINED, compute(33, 32, false, false, false, false));
    }

    @Test
    public void api33RevokedInSettings_isDenied() {
        assertEquals(DENIED, compute(33, 36, false, true, true, false));
    }

    @Test
    public void api33RationaleSeen_isDenied() {
        assertEquals(DENIED, compute(33, 36, false, false, false, true));
    }

    @Test
    public void api33RationaleNow_isDenied() {
        assertEquals(DENIED, PushPermissionStatus.compute(35, 36, false, false, false, false, true));
    }

    @Test
    public void deniedOnce_isUserChoice() {
        assertTrue(PushPermissionStatus.isDeniedByUser(33, 36, false, true, false));
    }

    @Test
    public void deniedPermanentlyAfterFirstDenial_isUserChoice() {
        assertTrue(PushPermissionStatus.isDeniedByUser(33, 36, false, false, true));
    }

    @Test
    public void dialogDismissed_isNotUserChoice() {
        assertFalse(PushPermissionStatus.isDeniedByUser(33, 36, false, false, false));
    }

    @Test
    public void legacyTargetSdk_anyDenialStopsPrompting() {
        assertTrue(PushPermissionStatus.isDeniedByUser(33, 32, false, false, false));
    }

    @Test
    public void granted_isNotUserDenial() {
        assertFalse(PushPermissionStatus.isDeniedByUser(33, 36, true, true, true));
    }

    @Test
    public void grantFromSdkPrompt_marksExplicit() {
        assertTrue(PushPermissionStatus.isGrantedViaSdkPrompt(33, 36, true, false));
    }

    @Test
    public void grantWhenFirstObservedGranted_staysDefault() {
        assertFalse(PushPermissionStatus.isGrantedViaSdkPrompt(33, 36, true, true));
    }

    @Test
    public void grantWithLegacyTargetSdk_staysDefault() {
        assertFalse(PushPermissionStatus.isGrantedViaSdkPrompt(33, 32, true, false));
    }

    @Test
    public void denialFromSdkPrompt_doesNotMarkExplicit() {
        assertFalse(PushPermissionStatus.isGrantedViaSdkPrompt(33, 36, false, false));
    }
}
