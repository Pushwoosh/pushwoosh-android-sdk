package com.pushwoosh.sample.inbox;

import java.io.Serializable;

public class Message implements Serializable
{
    static final long serialVersionUID = 503643406077571017L;

    private final CharSequence mText;
    private final long mTs;
    private final String mSender;

    public Message(CharSequence text, long ts, String sender)
    {
        mText = text;
        mTs = ts;
        mSender = sender;
    }

    public CharSequence getText()
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
