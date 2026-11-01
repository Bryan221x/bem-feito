package com.bemfeito.api.location.geocoding;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
interface NominatimGeocodingClient {
    List<NominatimPlace> search(AddressSearchRequest request) throws IOException;
}
