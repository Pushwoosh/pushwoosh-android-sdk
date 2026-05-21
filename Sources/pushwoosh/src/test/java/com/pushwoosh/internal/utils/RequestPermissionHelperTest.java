package com.pushwoosh.internal.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class RequestPermissionHelperTest {

    private AutoCloseable mocks;

    @Mock
    private Context context;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that Activity is not launched when every permission is already granted.
    @Test
    public void requestPermissionsForClass_allGranted_doesNotStartActivity() {
        String[] permissions = {"android.permission.POST_NOTIFICATIONS", "android.permission.ACCESS_FINE_LOCATION"};

        try (MockedStatic<ContextCompat> contextCompat = mockStatic(ContextCompat.class)) {
            contextCompat
                    .when(() -> ContextCompat.checkSelfPermission(eq(context), anyString()))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);

            RequestPermissionHelper.requestPermissionsForClass(PermissionActivity.class, context, permissions);
        }

        verify(context, never()).startActivity(any(Intent.class));
    }

    // Verifies that Activity is launched with target class, NEW_TASK flag and EXTRA_PERMISSIONS payload when no
    // permission is granted.
    @Test
    public void requestPermissionsForClass_noneGranted_startsActivityWithIntent() {
        String[] permissions = {"android.permission.POST_NOTIFICATIONS"};

        try (MockedStatic<ContextCompat> contextCompat = mockStatic(ContextCompat.class)) {
            contextCompat
                    .when(() -> ContextCompat.checkSelfPermission(eq(context), anyString()))
                    .thenReturn(PackageManager.PERMISSION_DENIED);

            RequestPermissionHelper.requestPermissionsForClass(PermissionActivity.class, context, permissions);
        }

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(1)).startActivity(intentCaptor.capture());

        Intent captured = intentCaptor.getValue();
        assertNotNull(captured.getComponent());
        assertEquals(PermissionActivity.class.getName(), captured.getComponent().getClassName());
        assertTrue((captured.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertArrayEquals(permissions, captured.getStringArrayExtra(RequestPermissionHelper.EXTRA_PERMISSIONS));
    }

    // Verifies that Activity is launched with all permissions in payload when at least one permission is missing.
    @Test
    public void requestPermissionsForClass_someGranted_startsActivityWithAllPermissions() {
        String permGranted = "android.permission.ACCESS_FINE_LOCATION";
        String permDenied = "android.permission.POST_NOTIFICATIONS";
        String[] permissions = {permGranted, permDenied};

        try (MockedStatic<ContextCompat> contextCompat = mockStatic(ContextCompat.class)) {
            contextCompat
                    .when(() -> ContextCompat.checkSelfPermission(context, permGranted))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);
            contextCompat
                    .when(() -> ContextCompat.checkSelfPermission(context, permDenied))
                    .thenReturn(PackageManager.PERMISSION_DENIED);

            RequestPermissionHelper.requestPermissionsForClass(PermissionActivity.class, context, permissions);
        }

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(1)).startActivity(intentCaptor.capture());

        String[] payload = intentCaptor.getValue().getStringArrayExtra(RequestPermissionHelper.EXTRA_PERMISSIONS);
        assertArrayEquals(permissions, payload);
    }

    // Verifies that Activity is not launched and no permission check happens when permissions array is empty.
    @Test
    public void requestPermissionsForClass_emptyPermissions_doesNotStartActivity() {
        String[] permissions = new String[0];

        try (MockedStatic<ContextCompat> contextCompat = mockStatic(ContextCompat.class)) {
            RequestPermissionHelper.requestPermissionsForClass(PermissionActivity.class, context, permissions);
            contextCompat.verifyNoInteractions();
        }

        verifyNoInteractions(context);
    }
}
