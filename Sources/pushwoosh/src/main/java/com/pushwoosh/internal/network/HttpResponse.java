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
}
