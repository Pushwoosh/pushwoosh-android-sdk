package com.pushwoosh.internal.event;

public class InitHwidEvent implements Event {
    private String hwid;

    public InitHwidEvent(String hwid) {
        this.hwid = hwid;
    }

    public String getHwid() {
        return hwid;
    }
}
