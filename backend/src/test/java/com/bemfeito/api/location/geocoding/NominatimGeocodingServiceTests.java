package com.bemfeito.api.location.geocoding;

import static org.junit.jupiter.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NominatimGeocodingServiceTests {
    @Test void userTriggeredSearchUsesCacheAndEnforcesGlobalOnePerSecondInterval() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        AtomicInteger calls = new AtomicInteger();
        NominatimGeocodingService service = new NominatimGeocodingService(request -> {
            calls.incrementAndGet();
            return List.of(new NominatimPlace(-23.55, -46.63, "Rua A, 10, São Paulo", "10", "node"),
                new NominatimPlace(-23.56, -46.64, "Rua A, São Paulo", null, "way"));
        }, clock::get);
        var request = new AddressSearchRequest("Rua A", "10", "Centro", "01000-000", "São Paulo", "SP");

        var first = service.search(request);
        assertEquals(2, first.size());
        assertFalse(first.get(0).approximate());
        assertTrue(first.get(1).approximate());
        assertEquals(first, service.search(request));
        assertEquals(1, calls.get(), "cache hit must not contact the external provider");

        var differentAddress = new AddressSearchRequest("Rua B", "20", "Centro", "01000-000", "São Paulo", "SP");
        var limited = assertThrows(ResponseStatusException.class, () -> service.search(differentAddress));
        assertEquals(429, limited.getStatusCode().value());
        assertEquals(1, calls.get());

        clock.addAndGet(1_000_000_000L);
        service.search(differentAddress);
        assertEquals(2, calls.get());
    }

    @Test void malformedOrIncompleteAddressIsRejectedBeforeExternalRequest() {
        AtomicInteger calls = new AtomicInteger();
        var service = new NominatimGeocodingService(request -> { calls.incrementAndGet(); return List.of(); });
        var error = assertThrows(ResponseStatusException.class,
            () -> service.search(new AddressSearchRequest("", "", "", "", "São Paulo", "SP")));
        assertEquals(400, error.getStatusCode().value());
        assertEquals(0, calls.get());
        assertThrows(ResponseStatusException.class,
            () -> service.search(new AddressSearchRequest("Rua A", "1", "", "", "São Paulo", "<script>")));
        assertEquals(0, calls.get());
    }

    @Test void providerUrlMustUseHttpsAndCannotContainCredentialsOrQueryParameters() {
        var mapper = new ObjectMapper();
        assertThrows(IllegalArgumentException.class, () -> new HttpNominatimGeocodingClient(mapper, "http://nominatim.openstreetmap.org"));
        assertThrows(IllegalArgumentException.class, () -> new HttpNominatimGeocodingClient(mapper, "https://user@example.org"));
        assertThrows(IllegalArgumentException.class, () -> new HttpNominatimGeocodingClient(mapper, "https://nominatim.openstreetmap.org?next=example"));
        assertDoesNotThrow(() -> new HttpNominatimGeocodingClient(mapper, "https://nominatim.openstreetmap.org"));
    }
}
