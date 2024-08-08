package com.pushwoosh.internal.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;


import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.network.TriggerInAppActionRequest;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.UUIDFactory;
import com.pushwoosh.testutil.PlatformTestManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class RequestStorageTest {
    private static final String APP_ID_REQUEST_BODY =
            "{\"application\":\"testAppId\"," +
                    "\"data\":{" +
                    "\"phone_type\":1," +
                    "\"device_model\":\"unknown\"," +
                    "\"notification_enabled\":1" +
                    ",\"manufacturer\":\"unknown\"," +
                    "\"v\":\"test_version\"," +
                    "\"device_type\":0," +
                    "\"hwid\":\"test_hwid\"}";

    private static final String TEST_BODY = "{\"param1\":1,\"param2\":true,\"param3\":\"3\"}";
    private static final String TEST_METHOD = "method";
    public static final String TEST_UUID = "test uuid";
    public static final String APP_CODE = "app_code";
    public static final String MESSAGE_HASH = "message_hash";
    public static final String RICH_MEDIA_CODE = "rich_media_code";
    public static final String TEST_KEY = "key";
    public static final String NOT_VALID_BODY = "$$#@2#%^%^^^";


    private RequestStorage requestStorage;

    @Mock
    private SQLiteDatabase db;
    @Mock
    private PushRequest<?> requestMock;
    @Mock
    private UUIDFactory uuidFactory;
    @Mock
    private Cursor cursorMock;

    private

    @Captor
    ArgumentCaptor<ContentValues> contentValuesArgumentCaptor;

    private Context context;
    private TriggerInAppActionRequest request;
    private PlatformTestManager platformTestManager;

    public static class RequestStorageTestable extends RequestStorage {
        SQLiteDatabase db;

        public RequestStorageTestable(Context context, UUIDFactory uuidFactory, SQLiteDatabase db) {
            super(context, uuidFactory);
            this.db = db;
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            return db;
        }
    }

    @Before
    public void setUp() throws Exception {

        context = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);
        requestStorage = new RequestStorageTestable(context, uuidFactory, db);

        when(uuidFactory.createUUID()).thenReturn(TEST_UUID);
        when(requestMock.getMethod()).thenReturn(TEST_METHOD);
        when(requestMock.getParams()).thenReturn(new JSONObject(TEST_BODY));
    }

    @Test
    @SmallTest
    public void shouldCreateTableIfCallOnCreate() {
        requestStorage.onCreate(db);

        verify(db)
                .execSQL("create table REQUEST (requestId TEXT primary key, method TEXT, body TEXT)");
    }

    @Test
    @SmallTest
    public void shouldDoNothingIfCallOnUpgrade() {
        requestStorage.onUpgrade(db, 1, 2);
        requestStorage.onUpgrade(db, 1, 3);

        verify(db, never()).execSQL(anyString());
    }

    @Test
    @SmallTest
    public void shouldSaveRequestToDBAndClodeIt() throws JSONException {
        requestStorage.add(requestMock);

        verify(db).insert(eq("REQUEST"), eq(null), contentValuesArgumentCaptor.capture());
        verify(db).close();
        ContentValues contentValues = contentValuesArgumentCaptor.getValue();
        String bodyString = contentValues.getAsString("body");
        JSONObject jsonObject = new JSONObject(bodyString);
        JSONAssert.assertEquals(TEST_BODY, jsonObject, false);
        String methodString = contentValues.getAsString("method");
        assertEquals(TEST_METHOD, methodString);
        assertEquals(TEST_UUID, contentValues.getAsString("requestId"));
    }

    @Test
    @SmallTest
    public void shouldCloseDbAfterGetAll() {
        requestStorage.get(0);
        verify(db).close();
    }

    @Test
    @SmallTest
    public void shouldCloseDbAfterRemove() {
        requestStorage.remove(TEST_KEY);
        verify(db).close();
    }


    @Test
    @SmallTest
    public void shouldSetEmptyBodyIfParsingBodyThrowExceptionGetAll() throws JSONException {
        String TABLE_NAME = "REQUEST";
        long rowId = 0;
        String selection = "rowid = ?";
        String[] selectionArgs = DbUtils.getSelectionArgs(String.valueOf(rowId));
        when(db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null)).thenReturn(cursorMock);
        when(cursorMock.getColumnIndex("requestId")).thenReturn(1);
        when(cursorMock.getColumnIndex("method")).thenReturn(2);
        when(cursorMock.getColumnIndex("body")).thenReturn(3);
        when(cursorMock.getString(1)).thenReturn(TEST_UUID);
        when(cursorMock.getString(2)).thenReturn(TEST_METHOD);
        when(cursorMock.getString(3)).thenReturn(NOT_VALID_BODY);
        when(cursorMock.moveToFirst()).thenReturn(true, false);

        CachedRequest cachedRequest = requestStorage.get(rowId);
        JSONObject params = cachedRequest.getParams();
        JSONObject expected = new JSONObject();
        JSONAssert.assertEquals(expected, params, false);
        assertEquals(TEST_METHOD, cachedRequest.getMethod());
    }

    @Test
    @SmallTest
    public void shouldSaveRequestIfParseBodyFail() throws JSONException, InterruptedException {
        doThrow(JSONException.class)
                .when(requestMock)
                .getParams();
        requestStorage.add(requestMock);

        verify(db).insert(eq("REQUEST"), eq(null), contentValuesArgumentCaptor.capture());

        ContentValues contentValues = contentValuesArgumentCaptor.getValue();
        String bodyString = contentValues.getAsString("body");
        assertNull(bodyString);
        String methodString = contentValues.getAsString("method");
        assertEquals(TEST_METHOD, methodString);
        assertEquals(TEST_UUID, contentValues.getAsString("requestId"));
    }

    @Test
    @MediumTest
    public void shouldReturnRequestIfBodyCantParsed() throws JSONException, InterruptedException {
        setUpIntegrationTest();
        doThrow(JSONException.class)
                .when(requestMock)
                .getParams();

        long rowId = requestStorage.add(requestMock);

        CachedRequest cachedRequest = requestStorage.get(rowId);
        assertEquals("method", cachedRequest.getMethod());
    }

    @Test
    @SmallTest
    public void shouldNotFailIfDbThrowExceptiondAdd() {
        doThrow(RuntimeException.class)
                .when(db)
                .insert(eq("REQUEST"), eq(null), any());

        requestStorage.add(requestMock);
    }

    @Test
    @SmallTest
    public void shouldNotFailIfDbThrowExceptiondGetAll() {
        doThrow(RuntimeException.class)
                .when(db)
                .rawQuery(anyString(), any());
        requestStorage.get(0);
    }

    @Test
    @SmallTest
    public void shouldNotFailIfDbThrowExceptiondRemove() {
        doThrow(RuntimeException.class)
                .when(db)
                .delete(anyString(), anyString(), any());
        requestStorage.remove(TEST_KEY);
    }

    private void tearDownIntegrationTest() {
        platformTestManager.tearDown();
    }

    private void setUpIntegrationTest() {
        platformTestManager = new PlatformTestManager();
        UUIDFactory uuidFactory = new UUIDFactory();
        requestStorage = new RequestStorage(context, uuidFactory);
    }

    private List<Long> addRequests() {
        request = new TriggerInAppActionRequest(APP_CODE, MESSAGE_HASH, RICH_MEDIA_CODE);
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");

        List<Long> rowIds = new ArrayList<>();
        rowIds.add(requestStorage.add(request));
        return rowIds;
    }

    @Test
    @SmallTest
    public void shouldRemoveCachedRequestByKey() {
        requestStorage.remove(TEST_KEY);

        verify(db).delete(eq("REQUEST"), eq("requestId=?"), eq(new String[]{TEST_KEY}));
    }

    @Test
    @MediumTest
    public void shouldNotReturnIfWasRemoved() {
        setUpIntegrationTest();
        List<Long> rowIds = addRequests();

        List<CachedRequest> cachedRequestList = new ArrayList<>();
        for (Long rowId : rowIds) {
            CachedRequest cachedRequest = requestStorage.get(rowId);
            if (cachedRequest != null) {
                cachedRequestList.add(cachedRequest);
            }
        }
        requestStorage.remove(cachedRequestList.get(0).getKey());

        CachedRequest cachedRequest = requestStorage.get(rowIds.get(0));
        assertNull(cachedRequest);

        tearDownIntegrationTest();
    }

    @Test
    @SmallTest
    public void shouldDropDbIfCallClear() {
        requestStorage.clear();
        verify(db).execSQL("delete from REQUEST");
    }

    @Test
    @MediumTest
    public void shouldReturnNothingAfterClear() {
        setUpIntegrationTest();
        List<Long> rowIds = addRequests();
        requestStorage.clear();

        for (Long rowId : rowIds) {
            assertNull(requestStorage.get(rowId));
        }

        tearDownIntegrationTest();
    }


}