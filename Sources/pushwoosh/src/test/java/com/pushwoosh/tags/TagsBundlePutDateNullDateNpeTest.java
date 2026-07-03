package com.pushwoosh.tags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class TagsBundlePutDateNullDateNpeTest {

    // Regression guard for crash-tagsbundle-putdate-null-date. putDate now null-guards value:
    // a null Date is skipped (logged + no-op) instead of crashing the host app inside
    // SimpleDateFormat.format(null). Was: assertThrows(NullPointerException) reaching
    // TagsBundle$Builder.putDate; now: graceful skip, the tag is simply not stored.

    // Verifies that the public Tags.dateTag entry with a null Date is skipped gracefully (the host
    // case: Tags.dateTag("Last_Login", user.getLastLogin()) where getLastLogin() returns null).
    @Test
    public void tagsDateTag_nullDate_isSkippedGracefully() {
        TagsBundle bundle = Tags.dateTag("Last_Login", null);

        assertNotNull(bundle);
        assertNull("null date must not be stored", bundle.getString("Last_Login"));
        assertFalse("null date tag must be skipped, not added", bundle.getMap().containsKey("Last_Login"));
    }

    // Verifies that the direct builder entry with a null Date is skipped gracefully.
    @Test
    public void builderPutDate_nullDate_isSkippedGracefully() {
        TagsBundle bundle =
                new TagsBundle.Builder().putDate("Last_Purchase", null).build();

        assertNull("null date must not be stored", bundle.getString("Last_Purchase"));
        assertFalse("null date tag must be skipped, not added", bundle.getMap().containsKey("Last_Purchase"));
    }

    // Negative control / discriminator: a non-null Date is still formatted and stored. This is the
    // necessary condition removed — only a null value triggered the old crash, and the guard does not
    // touch the non-null path.
    @Test
    public void builderPutDate_nonNullDate_isStored() {
        TagsBundle bundle =
                new TagsBundle.Builder().putDate("Last_Purchase", new Date(0L)).build();

        assertNotNull("non-null Date must format and store", bundle.getString("Last_Purchase"));
    }
}
