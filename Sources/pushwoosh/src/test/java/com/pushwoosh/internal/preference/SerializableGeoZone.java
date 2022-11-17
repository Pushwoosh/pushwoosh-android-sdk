package com.pushwoosh.internal.preference;

import android.location.Location;

import java.io.Serializable;

public class SerializableGeoZone implements Serializable {
    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private transient final Location location;

    public SerializableGeoZone(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = locationOf(latitude, longitude);
    }

    private static Location locationOf(double lat, double lng) {
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Location getLocation() {
        return location;
    }
}
