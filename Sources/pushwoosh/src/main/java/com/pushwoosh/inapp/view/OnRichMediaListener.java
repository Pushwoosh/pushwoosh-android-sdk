package com.pushwoosh.inapp.view;

import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.model.HtmlData;

public interface OnRichMediaListener {
    void startLoadingRichMedia();

    void finishLoadingRichMedia();

    boolean successLoadingHtmlData(HtmlData htmlData);

    void failedLoadingHtmlData(ResourceParseException exception);
}
