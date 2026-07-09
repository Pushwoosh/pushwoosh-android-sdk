package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.notification.LocalNotificationRequest;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

// Regression guard for crash-removedblocalnotification-uncaught-sqlite. removeDbLocalNotificationInternal
// (DbLocalNotificationHelper:299-312) used to be the ONLY db method in the class whose
// try (SQLiteDatabase db = getWritableDatabase()) had no catch, so a SQLiteException from the DB layer
// escaped straight onto the caller's thread. The public, un-gated entry LocalNotificationRequest.cancel()
// reaches it through LocalNotificationStorage.getLocalNotificationShown, which itself has no try/catch.
// The fix added catch(Exception) matching all eight guarded siblings: the DB error is now logged and
// swallowed (no-op), closing both public callers (removeDbLocalNotificationShown + removeDbLocalNotification).
// Was: assertThrows(SQLiteException) escaping through removeDbLocalNotificationInternal; now: graceful
// no-op at the DB method and silent success at cancel() (matching the JavaDoc "silently succeeds" promise).
//
// Stand-in for the real trigger (full/corrupt disk): getWritableDatabase() throws the exact
// SQLiteException family the signal predicts (SQLiteFullException = "database or disk is full"),
// injected uniformly so guarded and unguarded callers face an identical DB failure. Substituting a
// deterministic throw for a physically full disk keeps the outcome faithful; only the source of the
// exception is the stand-in. A full disk genuinely surfaces at getWritableDatabase (journal write).
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class DbLocalNotificationRemoveUncaughtSqliteCrashTest {

    private PlatformTestManager platformTestManager;
    private Context context;

    private static class DiskFullDbHelper extends DbLocalNotificationHelper {
        DiskFullDbHelper(Context context) {
            super(context);
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            throw new SQLiteFullException("database or disk is full (harness stand-in)");
        }
    }

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        context = AndroidPlatformModule.getApplicationContext();
    }

    @After
    public void tearDown() {
        // PrefsHelper teardown dereferences getLocalNotificationStorage(); restore a working one.
        RepositoryModule.setLocalNotificationStorage(
                new LocalNotificationStorage(new DbLocalNotificationHelper(context)));
        platformTestManager.tearDown();
    }

    // Point of defect, now guarded: both public callers that route through
    // removeDbLocalNotificationInternal swallow the DB-layer SQLiteException and return as a no-op,
    // instead of letting it escape uncaught onto the caller's thread.
    @Test
    public void removeDbLocalNotification_dbThrows_swallowedGracefully() {
        DbLocalNotificationHelper helper = new DiskFullDbHelper(context);

        helper.removeDbLocalNotificationShown(11);
        helper.removeDbLocalNotification(11);
    }

    // Non-vacuousness / sibling asymmetry: the SAME injected DB failure is swallowed by every guarded
    // sibling — each returns its sentinel instead of throwing. With the fix in place,
    // removeDbLocalNotificationInternal has joined this guarded family; before the fix it was the lone
    // exception. Proves the fault injection is real and the swallow is the catch, not ambient wiring.
    @Test
    public void guardedSiblings_sameDbFailure_swallowException() {
        DbLocalNotificationHelper helper = new DiskFullDbHelper(context);

        assertTrue(helper.getAllRequestIds().isEmpty());
        assertNull(helper.getDbLocalNotificationShown(21, "tag"));
        assertEquals(0, helper.nextRequestId());
        // insert/enumerate paths must not throw either
        helper.putDbLocalNotification(new DbLocalNotification(11, 11, "tag", 0L, new Bundle()));
        helper.enumerateDbLocalNotificationShownList(n -> {});
    }

    // Full public path from the documented entry point LocalNotificationRequest.cancel():
    // unschedule() goes through the WRAPPED removeLocalNotification (swallows), then cancel():202 ->
    // getLocalNotificationShown (NOT wrapped) -> removeDbLocalNotificationShown ->
    // removeDbLocalNotificationInternal. With the catch in place the whole cancel() completes as a
    // silent success under a full/corrupt DB, honoring the JavaDoc promise for an invalid request.
    @Test
    public void cancel_publicEntry_dbThrows_silentlySucceeds() {
        RepositoryModule.setLocalNotificationStorage(new LocalNotificationStorage(new DiskFullDbHelper(context)));

        new LocalNotificationRequest(11).cancel();
    }
}
