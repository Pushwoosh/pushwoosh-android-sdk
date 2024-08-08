package com.pushwoosh.internal.utils.security;

import android.content.ContentProvider;
import android.content.Context;
import android.os.Build;

import com.pushwoosh.internal.platform.resource.ResourceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class CallingPackageCheckerTest {
    String[] trustedPackages = {"com.pushwoosh.trusted_package"};
    String[] trustedPackages2 = {"com.pushwoosh.trusted_package",
            "com.pushwoosh.trusted_package2", "com.pushwoosh.trusted_package3"};
    String[] trustedPackages3 = {""};
    String[] trustedPackages4 = {" ", " "};
    String[] trustedPackages5 = {"ж", "$@!%"};
    String[] trustedPackages6 = {"a", "a", "a", "a", "a", "com.pushwoosh.trusted_package", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a"};

    ContentProvider contentProvider = Mockito.mock(ContentProvider.class);
    Context context = Mockito.mock(Context.class);


    @Before
    public void setUp() throws Exception {
        Mockito.when(contentProvider.getContext()).thenReturn(context);
        Mockito.when(context.getPackageName()).thenReturn("com.pushwoosh.test");

        ResourceProvider resourceProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getIdentifier("pw_trusted_package_names","array"))
                .thenReturn(1);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void checkCallingPackageTest() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.test");
        CallingPackageChecker.checkCallingPackage(contentProvider);
    }

    @Test(expected = SecurityException.class)
    public void checkCallingPackageFailsTest() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.untrusted_package");
        CallingPackageChecker.checkCallingPackage(contentProvider);
    }

    @Test
    public void checkIfTrustedPackageTest() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages);
    }

    @Test
    public void checkIfTrustedPackageTest2() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages2);
    }

    @Test(expected = SecurityException.class)
    public void checkIfTrustedPackageTest3() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages3);
    }

    @Test(expected = SecurityException.class)
    public void checkIfTrustedPackageTest4() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages4);
    }

    @Test(expected = SecurityException.class)
    public void checkIfTrustedPackageTest5() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages5);
    }

    @Test
    public void checkIfTrustedPackageTest6() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.trusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages6);
    }

    @Test(expected = SecurityException.class)
    public void checkIfTrustedPackageFailsTest() {
        Mockito.when(contentProvider.getCallingPackage()).thenReturn("com.pushwoosh.untrusted_package");
        CallingPackageChecker.checkIfTrustedPackage(contentProvider, trustedPackages);
    }
}