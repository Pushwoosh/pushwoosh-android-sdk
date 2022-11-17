package com.pushwoosh.repository;

import androidx.annotation.NonNull;


public class GetTagsRequestWithOldHWID extends GetTagsRequest {
    private String hwid;

    public GetTagsRequestWithOldHWID(String hwid) {
        this.hwid = hwid;
    }

    @NonNull
    @Override
    protected String getHwid() {
        return hwid;
    }
}
