package com.pushwoosh.inapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import android.content.Intent;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class WebActivityTest {

    // Verifies that for an in-app resource the builder writes IN_APP_CODE, leaves RICH_MEDIA_CODE empty and sets
    // SINGLE_TOP | NEW_TASK flags.
    @Test
    public void applyIntentParams_inAppNotRequired_writesInAppCodeAndSingleTopFlags() {
        Intent intent = new Intent();
        Resource resource = new Resource("inapp-123", "url", "hash", 0L, InAppLayout.FULLSCREEN, null, false, 0);

        Intent result = WebActivity.applyIntentParams(intent, resource, "chime", InAppView.MODE_DEFAULT);

        assertSame(intent, result);
        assertEquals(resource, result.getSerializableExtra(WebActivity.EXTRA_INAPP));
        assertEquals("chime", result.getStringExtra(WebActivity.EXTRA_SOUND));
        assertEquals(InAppView.MODE_DEFAULT, result.getIntExtra(WebActivity.EXTRA_MODE, -1));
        assertEquals("inapp-123", result.getStringExtra(WebActivity.IN_APP_CODE));
        assertEquals("", result.getStringExtra(WebActivity.RICH_MEDIA_CODE));
        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP);
        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    // Verifies that for a rich-media resource the builder strips the "r-" prefix into RICH_MEDIA_CODE, leaves
    // IN_APP_CODE empty and sets SINGLE_TOP | NEW_TASK.
    @Test
    public void applyIntentParams_richMediaNotRequired_writesRichMediaCodeWithoutPrefix() {
        Intent intent = new Intent();
        Resource resource = new Resource("r-abc", "url", "hash", 0L, InAppLayout.FULLSCREEN, null, false, 0);

        Intent result = WebActivity.applyIntentParams(intent, resource, "chime", InAppView.MODE_DEFAULT);

        assertEquals("abc", result.getStringExtra(WebActivity.RICH_MEDIA_CODE));
        assertEquals("", result.getStringExtra(WebActivity.IN_APP_CODE));
        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP);
        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    // Verifies that when resource is required only NEW_TASK is added and SINGLE_TOP is not set.
    @Test
    public void applyIntentParams_requiredResource_setsOnlyNewTaskFlag() {
        Intent intent = new Intent();
        Resource resource = new Resource("r-xyz", "url", "hash", 0L, InAppLayout.FULLSCREEN, null, true, 0);

        Intent result = WebActivity.applyIntentParams(intent, resource, "chime", InAppView.MODE_DEFAULT);

        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
        assertEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP);
        assertEquals("xyz", result.getStringExtra(WebActivity.RICH_MEDIA_CODE));
        assertEquals("", result.getStringExtra(WebActivity.IN_APP_CODE));
    }
}
