package org.mockito.configuration;

@SuppressWarnings("unused")
public class MockitoConfiguration extends DefaultMockitoConfiguration {

    @Override
    public boolean enableClassCache() {
        return false;
    }
}