package com.pushwoosh.demoapp.qa

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Debug-only hook that registers [InlineQaReceiver] at process start, so debug sources need no call
 * from DemoApplication and the release build contains no receiver.
 */
class InlineQaInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        InlineQaReceiver.register(context!!.applicationContext)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
