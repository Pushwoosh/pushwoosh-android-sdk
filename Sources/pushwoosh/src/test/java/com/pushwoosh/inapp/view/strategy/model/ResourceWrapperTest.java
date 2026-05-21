package com.pushwoosh.inapp.view.strategy.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.network.model.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class ResourceWrapperTest {

    @Mock
    private Resource resource;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    // Verifies that setResource with an in-app Resource derives IN_APP type from resource.isInApp().
    @Test
    public void setResource_inAppResource_derivesInAppType() {
        when(resource.isInApp()).thenReturn(true);

        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();

        assertSame(resource, wrapper.getResource());
        assertEquals(ResourceType.IN_APP, wrapper.getResourceType());
    }

    // Verifies that setResource with a non-in-app Resource derives RICH_MEDIA type.
    @Test
    public void setResource_nonInAppResource_derivesRichMediaType() {
        when(resource.isInApp()).thenReturn(false);

        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(resource).build();

        assertEquals(ResourceType.RICH_MEDIA, wrapper.getResourceType());
    }

    // Verifies that setResource(null) keeps the default IN_APP type and does not enter the derivation branch.
    @Test
    public void setResource_null_keepsDefaultInAppType() {
        ResourceWrapper wrapper =
                new ResourceWrapper.Builder().setResource(null).build();

        assertNull(wrapper.getResource());
        assertEquals(ResourceType.IN_APP, wrapper.getResourceType());
    }

    // Parameterized: setRichMedia(null) and setRichMedia("") are both no-ops (TextUtils.isEmpty guard).
    @Test
    public void setRichMedia_nullOrEmpty_isNoop() {
        String[] inputs = new String[] {null, ""};
        for (String input : inputs) {
            String label = input == null ? "null" : "empty";
            ResourceWrapper wrapper =
                    new ResourceWrapper.Builder().setRichMedia(input).build();

            assertNull("setRichMedia(" + label + ") must not assign a resource", wrapper.getResource());
            assertEquals(
                    "setRichMedia(" + label + ") must keep default IN_APP type",
                    ResourceType.IN_APP,
                    wrapper.getResourceType());
        }
    }

    // Verifies that setRichMedia delegates to Resource.parseRichMedia and applies the derived type.
    @Test
    public void setRichMedia_validPayload_parsesAndAssignsResource() throws ResourceParseException {
        Resource parsed = mock(Resource.class);
        when(parsed.isInApp()).thenReturn(false);

        try (MockedStatic<Resource> resourceStatic = mockStatic(Resource.class)) {
            resourceStatic.when(() -> Resource.parseRichMedia("payload")).thenReturn(parsed);

            ResourceWrapper wrapper =
                    new ResourceWrapper.Builder().setRichMedia("payload").build();

            assertSame(parsed, wrapper.getResource());
            assertEquals(ResourceType.RICH_MEDIA, wrapper.getResourceType());
        }
    }

    // Verifies that setRichMedia swallows ResourceParseException and leaves the Builder in default state.
    @Test
    public void setRichMedia_parseThrows_swallowsExceptionAndKeepsDefaults() throws ResourceParseException {
        try (MockedStatic<Resource> resourceStatic = mockStatic(Resource.class)) {
            resourceStatic.when(() -> Resource.parseRichMedia("bad")).thenThrow(new ResourceParseException("boom"));

            ResourceWrapper wrapper =
                    new ResourceWrapper.Builder().setRichMedia("bad").build();

            assertNull(wrapper.getResource());
            assertEquals(ResourceType.IN_APP, wrapper.getResourceType());
        }
    }
}
