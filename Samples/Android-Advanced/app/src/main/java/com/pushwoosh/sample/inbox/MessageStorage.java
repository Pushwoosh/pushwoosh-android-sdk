package com.pushwoosh.sample.inbox;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.ArrayList;

public class MessageStorage
{
    private static final String PREFERENCE = "com.pushwoosh.sample";
    private static final String PROPERTY_MESSAGE_HISTORY = "message_history";

    public static void addMessage(Context context, Message message)
    {
        ArrayList<Message> messages = getHistory(context);
        messages.add(message);

        try
        {
            SharedPreferences prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_MESSAGE_HISTORY, ObjectSerializer.serialize(messages));
            editor.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void clear(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_MESSAGE_HISTORY, null);
        editor.commit();
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Message> getHistory(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        try
        {
            String emptyVal = ObjectSerializer.serialize(new ArrayList<String>());
            return (ArrayList<Message>) ObjectSerializer.deserialize(prefs.getString(PROPERTY_MESSAGE_HISTORY, emptyVal));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<Message>();
    }
}
