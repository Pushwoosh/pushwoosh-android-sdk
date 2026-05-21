package com.pushwoosh.internal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pushwoosh.internal.utils.UUIDFactory;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class BaseRequestStorageTest {
    private static final String TEST_UUID = "test-uuid-1";
    private static final String TEST_UUID_2 = "test-uuid-2";
    private static final String TEST_UUID_3 = "test-uuid-3";
    private static final String TEST_METHOD = "POST_TAGS";
    private static final String TEST_BODY = "{\"k\":\"v\"}";

    @Mock
    private UUIDFactory uuidFactory;

    @Mock
    private PushRequest<?> requestMock;

    private Context context;
    private RequestStorage storage;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.application;
        when(uuidFactory.createUUID()).thenReturn(TEST_UUID);
        when(requestMock.getMethod()).thenReturn(TEST_METHOD);
        when(requestMock.getParams()).thenReturn(new JSONObject(TEST_BODY));
        storage = new RequestStorage(context, uuidFactory);
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
        context.deleteDatabase("request.db");
    }

    private PushRequest<?> mockRequest(String method, String jsonBody) throws Exception {
        PushRequest<?> request = mock(PushRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getParams()).thenReturn(new JSONObject(jsonBody));
        return request;
    }

    @Test
    public void add_savesRequestAndGetReturnsCachedRequestWithSameFields() throws Exception {
        long rowId = storage.add(requestMock);

        assertTrue("rowId must be > 0 after insert", rowId > 0);
        CachedRequest cached = storage.get(rowId);
        assertNotNull(cached);
        assertEquals(TEST_UUID, cached.getKey());
        assertEquals(TEST_METHOD, cached.getMethod());
        JSONAssert.assertEquals(new JSONObject(TEST_BODY), cached.getParams(), false);
    }

    @Test
    public void getAll_afterThreeAdds_returnsAllCachedRequests() throws Exception {
        when(uuidFactory.createUUID()).thenReturn(TEST_UUID, TEST_UUID_2, TEST_UUID_3);

        storage.add(mockRequest("M1", "{\"i\":1}"));
        storage.add(mockRequest("M2", "{\"i\":2}"));
        storage.add(mockRequest("M3", "{\"i\":3}"));

        List<CachedRequest> all = storage.getAll();
        assertEquals(3, all.size());

        Set<String> keys = new HashSet<>();
        Set<String> methods = new HashSet<>();
        for (CachedRequest cached : all) {
            keys.add(cached.getKey());
            methods.add(cached.getMethod());
        }
        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add(TEST_UUID);
        expectedKeys.add(TEST_UUID_2);
        expectedKeys.add(TEST_UUID_3);
        Set<String> expectedMethods = new HashSet<>();
        expectedMethods.add("M1");
        expectedMethods.add("M2");
        expectedMethods.add("M3");
        assertEquals(expectedKeys, keys);
        assertEquals(expectedMethods, methods);
    }

    @Test
    public void remove_deletesOnlyMatchingKey() throws Exception {
        when(uuidFactory.createUUID()).thenReturn(TEST_UUID, TEST_UUID_2);

        storage.add(mockRequest("M1", "{\"i\":1}"));
        storage.add(mockRequest("M2", "{\"i\":2}"));

        storage.remove(TEST_UUID);

        List<CachedRequest> all = storage.getAll();
        assertEquals(1, all.size());
        assertEquals(TEST_UUID_2, all.get(0).getKey());
    }

    @Test
    public void clear_emptiesTable() throws Exception {
        when(uuidFactory.createUUID()).thenReturn(TEST_UUID, TEST_UUID_2);

        storage.add(mockRequest("M1", "{\"i\":1}"));
        storage.add(mockRequest("M2", "{\"i\":2}"));

        storage.clear();

        List<CachedRequest> all = storage.getAll();
        assertTrue(all.isEmpty());
    }

    @Test
    public void get_unknownRowId_returnsNull() {
        CachedRequest cached = storage.get(999L);
        assertNull(cached);
    }

    @Test
    public void getAll_dbThrows_isRethrown() {
        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.query(anyString(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));
        RequestStorage failing = new RequestStorageWithMockDb(context, uuidFactory, mockDb);

        assertThrows(RuntimeException.class, failing::getAll);
    }

    @Test
    public void add_dbInsertThrows_swallowsAndReturnsMinusOne() {
        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        doThrow(new RuntimeException("boom")).when(mockDb).insert(anyString(), eq(null), any(ContentValues.class));
        RequestStorage failing = new RequestStorageWithMockDb(context, uuidFactory, mockDb);

        long rowId = failing.add(requestMock);

        assertEquals(-1L, rowId);
    }

    private static class RequestStorageWithMockDb extends RequestStorage {
        private final SQLiteDatabase mockDb;

        RequestStorageWithMockDb(Context context, UUIDFactory uuidFactory, SQLiteDatabase mockDb) {
            super(context, uuidFactory);
            this.mockDb = mockDb;
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            return mockDb;
        }
    }
}
