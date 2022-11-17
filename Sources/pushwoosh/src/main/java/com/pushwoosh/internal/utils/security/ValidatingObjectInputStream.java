package com.pushwoosh.internal.utils.security;


import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.List;

public class ValidatingObjectInputStream extends ObjectInputStream {
    private final List<ClassNameMatcher> acceptMatchers = new ArrayList<ClassNameMatcher>();
    private final List<ClassNameMatcher> rejectMatchers = new ArrayList<ClassNameMatcher>();


    public ValidatingObjectInputStream(InputStream input) throws IOException {
        super(input);
    }

    private void validateClassName(String name) throws InvalidClassException {
        // Reject has precedence over accept
        for (ClassNameMatcher m : rejectMatchers) {
            if (m.matches(name)) {
                invalidClassNameFound(name);
            }
        }

        boolean ok = false;
        for (ClassNameMatcher m : acceptMatchers) {
            if (m.matches(name)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            invalidClassNameFound(name);
        }
    }


    protected void invalidClassNameFound(String className) throws InvalidClassException {
        throw new InvalidClassException("Class name not accepted: " + className);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
        validateClassName(osc.getName());
        return super.resolveClass(osc);
    }


    public ValidatingObjectInputStream accept(Class<?>... classes) {
        for (Class<?> c : classes) {
            acceptMatchers.add(new FullClassNameMatcher(c.getName()));
        }
        return this;
    }

}

