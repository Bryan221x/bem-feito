package com.bemfeito.api.location.geocoding;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeocodingController {
    private final NominatimGeocodingService service;

    public GeocodingController(NominatimGeocodingService service) { this.service = service; }

    @PostMapping("/api/units/geocode")
    public List<GeocodingCandidate> search(@Valid @RequestBody AddressSearchRequest request) {
        return service.search(request);
    }
}
