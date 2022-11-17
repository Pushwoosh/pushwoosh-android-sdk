package com.pushwoosh.internal.preference;

import android.location.Location;

public class NonSerializableGeoZone {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private Location location;
    private Object object;

    public NonSerializableGeoZone(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = locationOf(latitude, longitude);
        this.object = new Object();
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
