package com.pushwoosh.tags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class TagsBundlePutListNullListNpeTest {

    // Regression guard for crash-tagsbundle-putlist-null-list. putList now null-guards value:
    // a null List is skipped (logged + no-op) instead of crashing the host app inside the bare
    // ConcurrentHashMap.put (CHM refuses a null value by contract). Was: assertThrows(NPE) reaching
    // TagsBundle$Builder.putList; now: graceful skip, the tag is simply not stored. Mirrors the twin
    // putDate fix (21ac44b6) -- putList was its missed sibling.

    // Verifies that the direct builder entry with a null List is skipped gracefully.
    @Test
    public void builderPutList_nullList_isSkippedGracefully() {
        TagsBundle bundle = new TagsBundle.Builder().putList("interests", null).build();

        assertNotNull(bundle);
        assertNull("null list must not be stored", bundle.getList("interests"));
        assertFalse("null list tag must be skipped, not added", bundle.getMap().containsKey("interests"));
    }

    // Verifies that the public Tags.listTag entry with a null List is skipped gracefully (the host
    // case: Tags.listTag("Interests", user.getInterests()) where getInterests() returns null).
    @Test
    public void tagsListTag_nullList_isSkippedGracefully() {
        TagsBundle bundle = Tags.listTag("Interests", null);

        assertNotNull(bundle);
        assertNull("null list must not be stored", bundle.getList("Interests"));
        assertFalse("null list tag must be skipped, not added", bundle.getMap().containsKey("Interests"));
    }

    // Sibling-consistency control: the reference-typed sibling putString also declines a null value
    // without crashing (its try/catch swallows the identical CHM.put NPE). Kept green to document that
    // every nullable builder entry now tolerates a null value; putList joins putString and putDate.
    @Test
    public void putString_nullValue_isSwallowedBySibling() {
        TagsBundle bundle = new TagsBundle.Builder().putString("name", null).build();

        assertNotNull(bundle);
        assertFalse(
                "null string is not stored (CHM.put NPE swallowed)",
                bundle.getMap().containsKey("name"));
    }

    // Negative control / discriminator: a non-null List stores fine. This is the necessary condition
    // removed -- only a null value triggered the old crash; the guard does not touch the happy path.
    @Test
    public void builderPutList_nonNullList_isStored() {
        List<String> values = Arrays.asList("technology", "sports");

        TagsBundle bundle =
                new TagsBundle.Builder().putList("interests", values).build();

        assertEqualsList(values, bundle.getList("interests"));
    }

    private static void assertEqualsList(List<String> expected, List<String> actual) {
        assertNotNull("non-null list must be stored", actual);
        org.junit.Assert.assertEquals(expected, actual);
    }
}
