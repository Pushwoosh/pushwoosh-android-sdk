package com.pushwoosh.sample.inbox

import android.content.Context
import android.content.SharedPreferences

import java.io.IOException
import java.util.ArrayList

object MessageStorage {
    private val PREFERENCE = "com.pushwoosh.sample"
    private val PROPERTY_MESSAGE_HISTORY = "message_history"

    fun addMessage(context: Context, message: Message) {
        val messages = getHistory(context)
        messages?.add(message)

        try {
            val prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(PROPERTY_MESSAGE_HISTORY, ObjectSerializer.serialize(messages))
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PROPERTY_MESSAGE_HISTORY, null)
        editor.commit()
    }

    fun getHistory(context: Context): ArrayList<Message>? {
        val prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        try {
            val emptyVal = ObjectSerializer.serialize(ArrayList<String>())
            return ObjectSerializer.deserialize(prefs.getString(PROPERTY_MESSAGE_HISTORY, emptyVal)!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ArrayList()
    }
}
