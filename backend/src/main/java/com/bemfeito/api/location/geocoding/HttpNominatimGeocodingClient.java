package com.bemfeito.api.location.geocoding;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class HttpNominatimGeocodingClient implements NominatimGeocodingClient {
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private final ObjectMapper json;
    private final URI baseUri;

    HttpNominatimGeocodingClient(ObjectMapper json,
            @Value("${app.geocoding.nominatim-base-url:https://nominatim.openstreetmap.org}") String baseUrl) {
        this.json = json;
        this.baseUri = validateBase(baseUrl);
    }

    @Override public List<NominatimPlace> search(AddressSearchRequest request) throws IOException {
        URI uri = UriComponentsBuilder.fromUri(baseUri).path("/search")
            .queryParam("format", "jsonv2").queryParam("addressdetails", "1")
            .queryParam("limit", "5").queryParam("countrycodes", "br")
            .queryParam("accept-language", "pt-BR")
            .queryParam("q", searchText(request)).encode(StandardCharsets.UTF_8).build().toUri();

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(6000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Language", "pt-BR");
        connection.setRequestProperty("User-Agent", "Bem-Feito/1.0 (+https://github.com/Bryan221x/bem-feito)");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("Nominatim returned HTTP " + status);
            byte[] bytes;
            try (var input = connection.getInputStream()) { bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1); }
            if (bytes.length > MAX_RESPONSE_BYTES) throw new IOException("Nominatim response exceeded the size limit");
            var root = json.readTree(bytes);
            if (!root.isArray()) throw new IOException("Unexpected Nominatim response");
            var places = new ArrayList<NominatimPlace>();
            for (var item : root) {
                var address = item.path("address");
                String lat = item.path("lat").asText(), lon = item.path("lon").asText();
                try {
                    double latitude = Double.parseDouble(lat), longitude = Double.parseDouble(lon);
                    if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180)
                        places.add(new NominatimPlace(latitude, longitude, limit(item.path("display_name").asText("Endereço sem descrição"), 600),
                            address.path("house_number").asText(null), item.path("osm_type").asText("unknown")));
                } catch (NumberFormatException ignored) { /* Ignora registros sem coordenadas numéricas. */ }
            }
            return List.copyOf(places);
        } finally {
            connection.disconnect();
        }
    }

    private static URI validateBase(String value) {
        URI uri = URI.create(value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Nominatim base URL must be an HTTPS origin/path without credentials or query");
        return uri;
    }

    private static String searchText(AddressSearchRequest request) {
        String street = request.number().isBlank() ? request.street() : request.street() + ", " + request.number();
        return java.util.stream.Stream.of(street, request.neighborhood(), request.postalCode(), request.city(), request.state(), "Brasil")
            .filter(value -> value != null && !value.isBlank()).reduce((a, b) -> a + ", " + b).orElse("");
    }
    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
}
