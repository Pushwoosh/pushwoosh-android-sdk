/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.internal.utils;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ObjectSerializerTest {

    private static class Entry implements Serializable {
        public String stringField;
        public int intField;

        public Entry(String strVal, int intVal) {
            stringField = strVal;
            intField = intVal;
        }
    }

    private static class NotSerializableEntry {
        public String stringField;
        public int intField;

        public NotSerializableEntry(String strVal, int intVal) {
            stringField = strVal;
            intField = intVal;
        }
    }

    @Test
    public void testSerialization() throws Exception {
        String serialized = ObjectSerializer.serialize(new Entry("test string", 42));
        Entry deserialized = ObjectSerializer.deserialize(serialized, Entry.class);

        assertThat(deserialized.stringField, is(equalTo("test string")));
        assertThat(deserialized.intField, is(42));
    }

    @Test(expected = IOException.class)
    public void testDeserializationShouldNotWorkIfWrongClassName() throws Exception {
        String serialized = ObjectSerializer.serialize(new Entry("test string", 42));
        ObjectSerializer.deserialize(serialized, String.class);
    }


    @Test
    public void testListSerialization() throws Exception {
        List<Entry> original = new ArrayList<>();
        original.add(new Entry("test string", 42));
        original.add(new Entry("another string", 88));

        String serialized = ObjectSerializer.serialize(original);

        List<Entry> deserialized = ObjectSerializer.deserialize(serialized,  original.getClass(), Entry.class);

        assertThat(deserialized, hasSize(2));
        Entry entry1 = deserialized.get(0);
        Entry entry2 = deserialized.get(1);

        assertThat(entry1.stringField, is(equalTo("test string")));
        assertThat(entry1.intField, is(42));
        assertThat(entry2.stringField, is(equalTo("another string")));
        assertThat(entry2.intField, is(88));
    }

    @Test(expected = IOException.class)
    public void testListDeserializationShouldNotWorkIfWasAddedNotAllClasses() throws Exception {
        List<Entry> original = new ArrayList<>();
        original.add(new Entry("test string", 42));
        original.add(new Entry("another string", 88));

        String serialized = ObjectSerializer.serialize(original);

        ObjectSerializer.deserialize(serialized,  original.getClass());
    }

    @Test
    public void testNullSerialization() throws Exception {
        String serialized = ObjectSerializer.serialize(null);
        Entry deserialized = ObjectSerializer.deserialize(serialized, (Class<?>[]) null);

        assertThat(deserialized, is(nullValue()));
    }

    @Test(expected = IOException.class)
    public void testNonSerializableSerialization() throws Exception {
        ObjectSerializer.serialize(new NotSerializableEntry("test string", 42));
    }

    @Test(expected = IOException.class)
    public void testGarbageDerialization() throws Exception {
        ObjectSerializer.deserialize("garbage", String.class);
    }

    @Test(expected = IOException.class)
    public void testGarbage2Derialization() throws Exception {
        ObjectSerializer.deserialize("garbage2", String.class);
    }
}
