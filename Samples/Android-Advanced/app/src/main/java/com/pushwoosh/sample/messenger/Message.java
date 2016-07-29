package com.pushwoosh.sample.messenger;

import java.io.Serializable;

public class Message implements Serializable
{
    private final String mText;
    private final long mTs;
    private final String mSender;

    public Message(String text, long ts, String sender)
    {
        mText = text;
        mTs = ts;
        mSender = sender;
    }

    public String getText()
    {
        return mText;
    }

    public long getTs()
    {
        return mTs;
    }

    public String getSender()
    {
        return mSender;
    }
}
