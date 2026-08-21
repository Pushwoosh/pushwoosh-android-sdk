package com.pushwoosh.internal.network;

class HttpResponse {
    final int statusCode;
    final String statusMessage;
    final String body;

    HttpResponse(int statusCode, String statusMessage, String body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;
    }

    static boolean isErrorCode(int code) {
        return code >= 400 && code < 600;
    }

    boolean isError() {
        return isErrorCode(statusCode);
    }
}
