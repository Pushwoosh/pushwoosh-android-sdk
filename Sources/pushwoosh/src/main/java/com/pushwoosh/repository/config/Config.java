package com.pushwoosh.repository.config;

import androidx.annotation.NonNull;

import java.util.List;

public class Config {
    private List<Channel> channels;
    private List<Event> events;
    private String publicKey;
    private int logger;

    public Config(@NonNull List<Channel> channels, @NonNull List<Event> events, @NonNull String publicKey, int logger) {
        this.channels = channels;
        this.events = events;
        this.publicKey = publicKey;
        this.logger = logger;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public int getLogger() {
        return logger;
    }
}
