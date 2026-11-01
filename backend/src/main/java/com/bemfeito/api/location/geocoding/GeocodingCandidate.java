package com.bemfeito.api.location.geocoding;

public record GeocodingCandidate(double latitude, double longitude, String label, String houseNumber, boolean approximate) { }
