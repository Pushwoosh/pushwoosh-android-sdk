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

package com.pushwoosh.inbox.storage.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.function.Function;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.internal.data.InboxMessageSource;
import com.pushwoosh.inbox.internal.data.InboxMessageStatus;
import com.pushwoosh.inbox.storage.data.MergeResult;
import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InboxDbHelper extends SQLiteOpenHelper {
	private static final String DELIMITER = "', '";

	private static final String INBOX_DB = "PwInbox.db";
	private static final int VERSION = 1;

	private final Object mutex = new Object();

	public InboxDbHelper(Context context) {
		super(context, INBOX_DB, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String createInAppsTable =
				"CREATE TABLE " + InboxTable.NAME + " (" +
						InboxTable.Column.ID + " text primary key, " +
						InboxTable.Column.ORDER + " integer default 0, " +
						InboxTable.Column.EXPIRED_DATE + " integer default 0, " +
						InboxTable.Column.SEND_DATE + " integer default 0, " +
						InboxTable.Column.TITLE + " text, " +
						InboxTable.Column.HASH + " text, " +
						InboxTable.Column.MESSAGE + " text, " +
						InboxTable.Column.IMAGE + " text, " +
						InboxTable.Column.TYPE + " integer default -1, " +
						InboxTable.Column.ACTION_PARAMS + " text, " +
						InboxTable.Column.STATUS + " integer default -1, " +
						InboxTable.Column.SOURCE + " integer default -1" +
						");";

		db.execSQL(createInAppsTable);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	void removeItems(Collection<String> ids) {
		executeSqlRaw(getDeletedRaw(ids));
	}

	Collection<String> changeStatus(Collection<String> ids, InboxMessageStatus status) {
		Collection<String> copyIds = new ArrayList<>(ids);

		List<InboxMessageInternal> byId = getById(ids);
		if (byId == null) {
			return Collections.emptyList();
		}

		for (InboxMessageInternal inboxMessageInternal : byId) {
			if (inboxMessageInternal.getInboxMessageStatus() == status) {
				copyIds.remove(inboxMessageInternal.getId());
			}
		}
		if (copyIds.isEmpty()) {
			return Collections.emptyList();
		}

		String raw = "UPDATE " + InboxTable.NAME +
				" SET " + InboxTable.Column.STATUS + " = " + status.getCode() +
				" WHERE " + InboxTable.Column.ID +
				" IN ('" + TextUtils.join(DELIMITER, copyIds) + "')";

		executeSqlRaw(raw);

		return copyIds;
	}

	@Nullable
	Collection<InboxMessageInternal> getAllActualWithStatus(Collection<InboxMessageStatus> statusList) {
		String table = InboxTable.NAME;
		String selection = InboxTable.Column.STATUS +
				" IN ( " + getCollectionSelectionParams(statusList) + " )" +
				" AND " + InboxTable.Column.EXPIRED_DATE + " > ?" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

		String[] selectionArgs = toStringArray(toStringList(statusList), String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
		String orderBy = InboxTable.Column.ORDER + " DESC";
		return query(table, null, selection, selectionArgs, null, null, orderBy, null, new Function<Cursor, Collection<InboxMessageInternal>>() {
			@Override
			public Collection<InboxMessageInternal> apply(Cursor cursor) {
				List<InboxMessageInternal> result = new ArrayList<>();
				if (cursor.moveToFirst()) {
					do {
						result.add(getInboxMessage(cursor));
					} while (cursor.moveToNext());
				}
				return result;
			}
		});
	}

	private String getCollectionSelectionParams(Collection collection) {
		if (collection == null || collection.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < collection.size(); ++i) {
			sb.append("?,");
		}
		String result = sb.toString();
		return result.substring(0, result.length() - 1);
	}

	private String[] toStringArray(List<String> stringList, String... params) {
		if (stringList == null && params == null) {
			return new String[]{};
		}
		String[] result = new String[stringList != null && params != null ? stringList.size() + params.length : (stringList == null ? params.length : stringList.size())];
		if (stringList != null) {
			for (int i = 0; i < stringList.size(); ++i) {
				result[i] = stringList.get(i);
			}
		}
		if (params != null) {
			for (int i = 0; i < params.length; ++i) {
				result[stringList != null ? stringList.size() + i : i] = params[i];
			}
		}
		return result;
	}

	@Nullable
	Collection<InboxMessageInternal> getAllPushMessages() {
		String tableName = InboxTable.NAME;
		String selection = InboxTable.Column.SOURCE + " = ?";
		String[] selectionArgs = new String[]{String.valueOf(InboxMessageSource.PUSH.getCode())};
		return query(tableName, null, selection, selectionArgs, null, null, null, null, new Function<Cursor, Collection<InboxMessageInternal>>() {
			@Override
			public Collection<InboxMessageInternal> apply(Cursor cursor) {
				List<InboxMessageInternal> result = new ArrayList<>();
				if (cursor.moveToFirst()) {
					do {
						result.add(getInboxMessage(cursor));
					} while (cursor.moveToNext());
				}
				return result;
			}
		});
	}

	@Nullable
	Collection<InboxMessageInternal> getActualMessagesWithStatus(Collection<InboxMessageStatus> statusList, long sortOrder, int limit) {
		String table = InboxTable.NAME;
		String selection = InboxTable.Column.STATUS +
				" IN ( " + getCollectionSelectionParams(statusList) + " )" +
				" AND " + InboxTable.Column.EXPIRED_DATE + " > ?" +
				" AND " + InboxTable.Column.ORDER + " < ?";
		String[] selectionArgs = toStringArray(toStringList(statusList),
				String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())),
				String.valueOf(sortOrder));
		String orderBy = InboxTable.Column.ORDER + " DESC ";
		String limitCondition = limit == -1 ? null : String.valueOf(limit);

		return query(table, null, selection, selectionArgs, null, null, orderBy, limitCondition, new Function<Cursor, Collection<InboxMessageInternal>>() {
			@Override
			public Collection<InboxMessageInternal> apply(Cursor cursor) {
				List<InboxMessageInternal> result = new ArrayList<>();
				if (cursor.moveToFirst()) {
					do {
						result.add(getInboxMessage(cursor));
					} while (cursor.moveToNext());
				}
				return result;
			}
		});
	}

	@Nullable
	Integer getActualCountWithStatus(Collection<InboxMessageStatus> statusList) {
		final String countColumn = "COUNT";
		String table = InboxTable.NAME;
		String[] columns = new String[]{"count(*) AS " + countColumn};
		String selection = InboxTable.Column.STATUS +
				" IN ( " + getCollectionSelectionParams(statusList) + " )" +
				" AND " + InboxTable.Column.EXPIRED_DATE + " > ?";
		String[] selectionArgs = toStringArray(toStringList(statusList),
				String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));

		return query(table, columns, selection, selectionArgs, null, null, null, null, new Function<Cursor, Integer>() {
			@Override
			public Integer apply(Cursor cursor) {
				if (cursor.moveToFirst()) {
					return cursor.getInt(cursor.getColumnIndex(countColumn));
				}

				return -1;
			}
		});
	}

	@Nullable
	InboxMessageInternal getById(String id) {
		String table = InboxTable.NAME;
		String selection = InboxTable.Column.ID + " = ?" +
				" AND " + InboxTable.Column.EXPIRED_DATE + " > ?";
		String[] selectionArgs = new String[]{id, String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))};
		return query(table, null, selection, selectionArgs, null, null, null, null, new Function<Cursor, InboxMessageInternal>() {
			@Override
			public InboxMessageInternal apply(Cursor cursor) {
				if (cursor.moveToFirst()) {
					return getInboxMessage(cursor);
				}

				return null;
			}
		});
	}

	@Nullable
	List<InboxMessageInternal> getById(Collection<String> ids) {
		String table = InboxTable.NAME;
		String selection = InboxTable.Column.EXPIRED_DATE + " > ?";
		String[] selectionArgs = new String[]{String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))};
		String orderBy = InboxTable.Column.ORDER + " DESC";
		return query(table, null, selection, selectionArgs, null, null, orderBy, null, new Function<Cursor, List<InboxMessageInternal>>() {
			@Override
			public List<InboxMessageInternal> apply(Cursor cursor) {
				List<InboxMessageInternal> result = new ArrayList<>();
				if (cursor.moveToFirst()) {
					do {
						String id = cursor.getString(cursor.getColumnIndex(InboxTable.Column.ID));
						if (ids != null && !ids.isEmpty() && ids.contains(id)) {
							result.add(getInboxMessage(cursor));
						}
					} while (cursor.moveToNext());
				}
				return result;
			}
		});
	}

	@NonNull
	MergeResult createOrUpdate(Collection<InboxMessageInternal> inboxMessageInternals, boolean fullList) {
		MergeResult mergeResult = MergeResult.createEmpty();
		List<String> inboxMessagesIds = new ArrayList<>();
		synchronized (mutex) {
			final SQLiteDatabase db = getWritableDatabase();
			try {
				db.beginTransaction();
				try {
					removeExpired(db);
					for (InboxMessageInternal inboxMessageInternal : inboxMessageInternals) {
						inboxMessagesIds.add(inboxMessageInternal.getId());
						createOrUpdate(db, inboxMessageInternal, mergeResult);
					}

					if (fullList) {
						mergeResult.getDeletedItems().addAll(selectNotFromList(inboxMessagesIds, db));
						db.execSQL(getDeletedRaw(mergeResult.getDeletedItems()));
					}

					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			} catch (Exception e) {
				PWLog.error("Failed work with db", e);
			} finally {
				db.close();
			}
		}
		return mergeResult;
	}

	private void removeExpired(SQLiteDatabase database) {
		database.delete(InboxTable.NAME,
						InboxTable.Column.EXPIRED_DATE + " < ?",
						new String[]{ String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())) });
	}

	private void createOrUpdate(SQLiteDatabase db, InboxMessageInternal inboxMessageInternal, MergeResult mergeResult) {
		String[] columns = new String[]{InboxTable.Column.STATUS, InboxTable.Column.SOURCE};
		String table = InboxTable.NAME;
		String selection = InboxTable.Column.ID + " = ?";
		String[] selectionArgs = DbUtils.getSelectionArgs(inboxMessageInternal.getId());
		final Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, null);
		try {
			if (cursor.moveToFirst()) {
				boolean needToChangeStatus = true;
				final InboxMessageStatus byCode = InboxMessageStatus.getByCode(cursor.getInt(cursor.getColumnIndex(InboxTable.Column.STATUS)));
				final InboxMessageSource source = InboxMessageSource.getByCode(cursor.getInt(cursor.getColumnIndex(InboxTable.Column.SOURCE)));
				if (source == InboxMessageSource.PUSH || byCode == null || byCode.isLowerStatus(inboxMessageInternal.getInboxMessageStatus())) {
					mergeResult.getUpdatedItems().add(inboxMessageInternal.getId());
				} else if (inboxMessageInternal.getInboxMessageStatus().isLowerStatus(byCode)) {
					mergeResult.getIncorrectNetworkStatus().put(inboxMessageInternal.getId(), byCode);
					needToChangeStatus = false;
				}

				final ContentValues inboxMessageContentValues = getInboxMessageContentValues(inboxMessageInternal);
				if (!needToChangeStatus) {
					inboxMessageContentValues.remove(InboxTable.Column.STATUS);
				}
				db.update(InboxTable.NAME, inboxMessageContentValues, InboxTable.Column.ID + " = ?", new String[]{ inboxMessageInternal.getId() });
			} else {
				mergeResult.getNewItems().add(inboxMessageInternal.getId());
				addInboxMessage(db, inboxMessageInternal);
			}
		} finally {
			cursor.close();
		}
	}

	private List<String> selectNotFromList(List<String> inboxMessagesIds, SQLiteDatabase db) {
		List<String> ids = new ArrayList<>();
		String[] columns = new String[]{InboxTable.Column.ID};
		String table = InboxTable.NAME;
		final Cursor cursor = db.query(table, columns, null, null, null, null, null);
		try {
			if (cursor.moveToFirst()) {
				do {
					String id = cursor.getString(cursor.getColumnIndex(InboxTable.Column.ID));
					if (inboxMessagesIds == null || inboxMessagesIds.isEmpty() || !inboxMessagesIds.contains(id))
					ids.add(id);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		return ids;
	}

	private void addInboxMessage(SQLiteDatabase db, InboxMessageInternal inboxMessageInternal) {
		ContentValues contentValues = getInboxMessageContentValues(inboxMessageInternal);
		final long insert = db.insert(InboxTable.NAME, null, contentValues);
		if (insert == 0L) {
			PWLog.warn("Not stored " + inboxMessageInternal.getId());
		}
	}

	@NonNull
	private ContentValues getInboxMessageContentValues(InboxMessageInternal inboxMessageInternal) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(InboxTable.Column.ID, inboxMessageInternal.getId());
		contentValues.put(InboxTable.Column.ORDER, inboxMessageInternal.getOrder());
		contentValues.put(InboxTable.Column.EXPIRED_DATE, inboxMessageInternal.getExpiredDate());
		contentValues.put(InboxTable.Column.SEND_DATE, inboxMessageInternal.getSendDate());
		contentValues.put(InboxTable.Column.TITLE, inboxMessageInternal.getTitle());
		contentValues.put(InboxTable.Column.HASH, inboxMessageInternal.getHash());
		contentValues.put(InboxTable.Column.MESSAGE, inboxMessageInternal.getMessage());
		contentValues.put(InboxTable.Column.IMAGE, inboxMessageInternal.getImage());
		contentValues.put(InboxTable.Column.TYPE, inboxMessageInternal.getInboxMessageType().getCode());
		contentValues.put(InboxTable.Column.ACTION_PARAMS, inboxMessageInternal.getActionParams());
		contentValues.put(InboxTable.Column.STATUS, inboxMessageInternal.getInboxMessageStatus().getCode());
		contentValues.put(InboxTable.Column.SOURCE, inboxMessageInternal.getSource().getCode());
		return contentValues;
	}

	private <T> T query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, Function<Cursor, T> function) {
		synchronized (mutex) {
			try {
				final SQLiteDatabase db = getReadableDatabase();
				try {
					T apply;
					db.beginTransaction();
					final Cursor cursor = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
					try {
						apply = function.apply(cursor);
						db.setTransactionSuccessful();
					} finally {
						cursor.close();
						db.endTransaction();
					}
					return apply;
				} catch (Exception e) {
					PWLog.error("Failed work with db", e);
					return null;
				} finally {
					db.close();
				}
			} catch (Exception e) {
				PWLog.error("Problem with db executing", e);
				return null;
			}
		}
	}

	private InboxMessageInternal getInboxMessage(Cursor cursor) {
		return new InboxMessageInternal.Builder()
				.setId(cursor.getString(cursor.getColumnIndex(InboxTable.Column.ID)))
				.setOrder(cursor.getLong(cursor.getColumnIndex(InboxTable.Column.ORDER)))
				.setExpiredDate(cursor.getLong(cursor.getColumnIndex(InboxTable.Column.EXPIRED_DATE)))
				.setSendDate(cursor.getLong(cursor.getColumnIndex(InboxTable.Column.SEND_DATE)))
				.setTitle(cursor.getString(cursor.getColumnIndex(InboxTable.Column.TITLE)))
				.setHash(cursor.getString(cursor.getColumnIndex(InboxTable.Column.HASH)))
				.setMessage(cursor.getString(cursor.getColumnIndex(InboxTable.Column.MESSAGE)))
				.setImage(cursor.getString(cursor.getColumnIndex(InboxTable.Column.IMAGE)))
				.setInboxMessageType(InboxMessageType.getByCode(cursor.getInt(cursor.getColumnIndex(InboxTable.Column.TYPE))))
				.setActionParams(cursor.getString(cursor.getColumnIndex(InboxTable.Column.ACTION_PARAMS)))
				.setInboxMessageStatus(InboxMessageStatus.getByCode(cursor.getInt(cursor.getColumnIndex(InboxTable.Column.STATUS))))
				.setSource(InboxMessageSource.getByCode(cursor.getInt(cursor.getColumnIndex(InboxTable.Column.SOURCE))))
				.build();
	}

	private List<String> toStringList(Collection<InboxMessageStatus> statusList) {
		List<String> ordinal = new ArrayList<>();

		for (InboxMessageStatus inboxMessageStatus : statusList) {
			ordinal.add(String.valueOf(inboxMessageStatus.getCode()));
		}

		return ordinal;
	}

	private void executeSqlRaw(String row) {
		synchronized (mutex) {
			try {
				final SQLiteDatabase writableDatabase = getWritableDatabase();
				try {
					writableDatabase.beginTransaction();
					try {
						writableDatabase.execSQL(row);
						writableDatabase.setTransactionSuccessful();
					} finally {
						writableDatabase.endTransaction();
					}
				} finally {
					writableDatabase.close();
				}
			} catch (Exception e) {
				PWLog.error("Problem with db executing", e);
			}
		}
	}

	@NonNull
	private static String getDeletedRaw(Collection<String> ids) {
		return "DELETE FROM " + InboxTable.NAME +
				" WHERE " + InboxTable.Column.ID +
				" IN ('" + TextUtils.join(DELIMITER, ids) + "')";
	}

	public void dropDb() {
		synchronized (mutex) {
			try {
				SQLiteDatabase database = getWritableDatabase();
				try {
					database.beginTransaction();
					try {
						database.delete(InboxTable.NAME, "", new String[]{ });
						database.setTransactionSuccessful();
					} finally {
						database.endTransaction();
					}
				} finally {
					database.close();
				}

			} catch (Exception e) {
				PWLog.error("Problem with db executing", e);
			}
		}
	}

	private static class InboxTable {
		static String NAME = "inboxMessage";

		static class Column {
			static final String ID = "inbox_id";
			static final String ORDER = "inbox_order";
			static final String EXPIRED_DATE = "expired_date";
			static final String SEND_DATE = "send_date";
			static final String TITLE = "title";
			static final String MESSAGE = "message";
			static final String IMAGE = "image";
			static final String TYPE = "type";
			static final String ACTION_PARAMS = "action_params";
			static final String STATUS = "status";
			static final String SOURCE = "source";
			static final String HASH = "hash";
		}
	}
}
