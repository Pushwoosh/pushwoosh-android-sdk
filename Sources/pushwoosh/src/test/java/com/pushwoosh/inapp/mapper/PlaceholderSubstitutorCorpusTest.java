package com.pushwoosh.inapp.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Golden check over stock Pushwoosh Rich Media templates with inline scripts. {@code expected.html} is the
 * substitution output with empty localization and tags, so every placeholder collapses to its default; any change to
 * which fragments are recognised as placeholders shows up as a diff.
 *
 * <p>{@code inline-script-pipe} pins an inline script that must survive substitution intact.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PlaceholderSubstitutorCorpusTest {

    private static final String ROOT = "/placeholder-corpus/";

    private static final String[] TEMPLATES = {
        "inline-script-pipe",
        "rich-media-pages__markup1",
        "rich-media-pages__markup7",
        "rich-media-pages__push-register",
    };

    @Test
    public void substitute_realTemplates_outputUnchanged() throws IOException {
        for (String name : TEMPLATES) {
            String in = read(ROOT + name + "/index.html");
            String expected = read(ROOT + name + "/expected.html");
            String out = PlaceholderSubstitutor.substitute(in, Collections.emptyMap(), Collections.emptyMap());
            assertEquals("template " + name, expected, out);
        }
    }

    private static String read(String path) throws IOException {
        try (InputStream in = PlaceholderSubstitutorCorpusTest.class.getResourceAsStream(path)) {
            assertNotNull("missing resource " + path, in);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
