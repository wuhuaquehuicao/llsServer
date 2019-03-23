package com.lssservlet.utils;

public class ShipCoordinates {
    public final double latitude;
    public final double longitude;

    public final double VERY_CLOSE_LOCATION_DELTA = 500;
    public final double SECOND_CLOSE_LOCATION_DELTA = 1500;

    public ShipCoordinates(Double lat, Double lon) {
        latitude = lat;
        longitude = lon;
    }

    public ShipCoordinates(String latString, String lonString) {

        if (!StringUtil.isBlank(latString) && !StringUtil.isBlank(lonString)) {
            latitude = Double.valueOf(latString);
            longitude = Double.valueOf(lonString);
        } else {
            latitude = Double.MAX_VALUE;
            longitude = Double.MAX_VALUE;
        }
    }

    public boolean isValid() {
        return (Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180);
    }

    public boolean isCloseToCoordinate(ShipCoordinates other) {
        return distanceFrom(other) <= VERY_CLOSE_LOCATION_DELTA;
    }

    public boolean isSecondCloseToCoordinate(ShipCoordinates other) {
        return distanceFrom(other) <= SECOND_CLOSE_LOCATION_DELTA;
    }

    public String toString() {
        return latitude + "," + longitude;
    }

    public double distanceFrom(ShipCoordinates another) {

        if (another == null || !another.isValid()) {
            return Double.MAX_VALUE;
        }

        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(latitude - another.latitude);
        double dLng = Math.toRadians(longitude - another.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(another.latitude)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }
}
