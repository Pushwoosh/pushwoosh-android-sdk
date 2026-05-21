package com.pushwoosh.internal.utils.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;

public class ValidatingObjectInputStreamTest {

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    private static ValidatingObjectInputStream streamOf(byte[] bytes) throws IOException {
        return new ValidatingObjectInputStream(new ByteArrayInputStream(bytes));
    }

    // Verifies that an accepted class deserializes successfully through the real readObject() path.
    @Test
    public void readObject_acceptedClass_returnsOriginalValue() throws Exception {
        byte[] bytes = serialize(Boolean.TRUE);

        try (ValidatingObjectInputStream stream = streamOf(bytes)) {
            stream.accept(Boolean.class);
            Object result = stream.readObject();
            assertEquals(Boolean.TRUE, result);
        }
    }

    // Verifies that accept(Class...) registers each vararg class so a multi-class chain payload deserializes.
    @Test
    public void readObject_acceptVarargRegistersAllClasses_deserializesClassAndSuperclass() throws Exception {
        byte[] bytes = serialize(Integer.valueOf(42));

        try (ValidatingObjectInputStream stream = streamOf(bytes)) {
            stream.accept(Integer.class, Number.class);
            Object result = stream.readObject();
            assertEquals(Integer.valueOf(42), result);
        }
    }

    // Verifies that a class outside the accept-list throws InvalidClassException with the class name in the message.
    @Test
    public void readObject_classNotAccepted_throwsInvalidClassExceptionWithClassName() throws Exception {
        byte[] bytes = serialize(Integer.valueOf(42));

        try (ValidatingObjectInputStream stream = streamOf(bytes)) {
            stream.accept(String.class);
            InvalidClassException ex = assertThrows(InvalidClassException.class, stream::readObject);
            assertTrue(
                    "message should contain class name: " + ex.getMessage(),
                    ex.getMessage().contains("Integer"));
            assertTrue(
                    "message should mention rejection: " + ex.getMessage(),
                    ex.getMessage().contains("not accepted"));
        }
    }

    // Verifies that an empty accept-list rejects every class (default-deny behavior).
    @Test
    public void readObject_emptyAcceptList_rejectsEveryClass() throws Exception {
        byte[] bytes = serialize(Integer.valueOf(1));

        try (ValidatingObjectInputStream stream = streamOf(bytes)) {
            InvalidClassException ex = assertThrows(InvalidClassException.class, stream::readObject);
            assertTrue(
                    "message should contain class name: " + ex.getMessage(),
                    ex.getMessage().contains("Integer"));
        }
    }

    // Verifies that a subclass overriding invalidClassNameFound can suppress rejection.
    @Test
    public void readObject_subclassSuppressesInvalidClassNameFound_returnsOriginalValue() throws Exception {
        byte[] bytes = serialize(Integer.valueOf(7));

        ValidatingObjectInputStream stream = new ValidatingObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected void invalidClassNameFound(String className) {
                // no-op: subclass chooses to permit anything.
            }
        };
        try {
            Object result = stream.readObject();
            assertEquals(Integer.valueOf(7), result);
        } finally {
            stream.close();
        }
    }

    // Verifies that the accept-list is per-instance and matches by exact class name, not assignability.
    @Test
    public void readObject_acceptListIsPerInstance_matchesByExactName() throws Exception {
        byte[] booleanBytes = serialize(Boolean.TRUE);
        byte[] integerBytes = serialize(Integer.valueOf(99));

        try (ValidatingObjectInputStream first = streamOf(booleanBytes)) {
            first.accept(Boolean.class);
            assertEquals(Boolean.TRUE, first.readObject());
        }

        try (ValidatingObjectInputStream second = streamOf(integerBytes)) {
            second.accept(Boolean.class);
            InvalidClassException ex = assertThrows(InvalidClassException.class, second::readObject);
            assertTrue(
                    "message should contain class name: " + ex.getMessage(),
                    ex.getMessage().contains("Integer") || ex.getMessage().contains("Number"));
        }
    }
}
