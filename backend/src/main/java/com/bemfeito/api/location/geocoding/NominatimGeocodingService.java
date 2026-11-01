package com.bemfeito.api.location.geocoding;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NominatimGeocodingService {
    private static final long MIN_REQUEST_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long CACHE_TTL_NANOS = TimeUnit.HOURS.toNanos(24);
    private static final int MAX_CACHE_ENTRIES = 256;
    private final NominatimGeocodingClient client;
    private final LongSupplier nanoTime;
    private final LinkedHashMap<String, CacheEntry> cache = new LinkedHashMap<>(32, .75f, true);
    private long lastRequestNanos = Long.MIN_VALUE;

    @Autowired
    public NominatimGeocodingService(NominatimGeocodingClient client) { this(client, System::nanoTime); }

    NominatimGeocodingService(NominatimGeocodingClient client, LongSupplier nanoTime) {
        this.client = client;
        this.nanoTime = nanoTime;
    }

    public List<GeocodingCandidate> search(AddressSearchRequest request) {
        var safe = validate(request);
        String key = cacheKey(safe);
        long now = nanoTime.getAsLong();
        synchronized (cache) {
            var existing = cache.get(key);
            if (existing != null && now - existing.savedAtNanos < CACHE_TTL_NANOS) return existing.results;
            cache.remove(key);
            if (lastRequestNanos != Long.MIN_VALUE && now - lastRequestNanos < MIN_REQUEST_INTERVAL_NANOS)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde um instante antes de tentar outra busca.");
            lastRequestNanos = now;
        }

        final List<NominatimPlace> places;
        try { places = client.search(safe); }
        catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Busca de endereço temporariamente indisponível.");
        }
        String requestedNumber = normalize(safe.number());
        List<GeocodingCandidate> results = places.stream().limit(5).map(place -> {
            boolean approximate = requestedNumber.isBlank() || !requestedNumber.equals(normalize(place.houseNumber())) || !"node".equals(place.osmType());
            return new GeocodingCandidate(place.latitude(), place.longitude(), place.label(), place.houseNumber(), approximate);
        }).toList();
        synchronized (cache) {
            cache.put(key, new CacheEntry(nanoTime.getAsLong(), results));
            while (cache.size() > MAX_CACHE_ENTRIES) cache.remove(cache.keySet().iterator().next());
        }
        return results;
    }

    private static AddressSearchRequest validate(AddressSearchRequest request) {
        if (request == null) throw badRequest();
        String street = field(request.street(), 160), number = field(request.number(), 32);
        String neighborhood = field(request.neighborhood(), 100), postal = field(request.postalCode(), 9);
        String city = field(request.city(), 100), state = field(request.state(), 2);
        if (street.isBlank() || city.isBlank() || !state.matches("[A-Za-z]{2}") || (!postal.isBlank() && !postal.matches("[0-9]{8}|[0-9]{5}-[0-9]{3}"))) throw badRequest();
        return new AddressSearchRequest(street, number, neighborhood, postal, city, state.toUpperCase(Locale.ROOT));
    }

    private static String field(String value, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max || text.codePoints().anyMatch(Character::isISOControl)) throw badRequest();
        return text;
    }

    private static ResponseStatusException badRequest() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe logradouro, cidade e estado válidos para localizar o endereço.");
    }

    private static String cacheKey(AddressSearchRequest request) {
        return String.join("|", request.street(), request.number(), request.neighborhood(), request.postalCode(), request.city(), request.state())
            .transform(NominatimGeocodingService::normalize);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record CacheEntry(long savedAtNanos, List<GeocodingCandidate> results) { }
}
