package com.company.keyvault.service;

import com.company.keyvault.model.GeoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String FIELDS =
            "success,country,country_code,region,city,latitude,longitude,timezone,connection";

    private final boolean enabled;
    private final boolean localFallbackEnabled;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeoIpService(
            @Value("${app.geo-ip.enabled:true}") boolean enabled,
            @Value("${app.geo-ip.local-fallback-enabled:true}") boolean localFallbackEnabled,
            @Value("${app.geo-ip.base-url:https://ipwho.is/}") String baseUrl,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.localFallbackEnabled = localFallbackEnabled;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Optional<GeoLocation> lookup(String ipAddress) {
        if (!enabled) {
            return Optional.empty();
        }

        boolean publicIp = isPublicIp(ipAddress);
        if (!publicIp && !localFallbackEnabled) return Optional.empty();
        String cacheKey = publicIp ? ipAddress : "__local_public_ip__";

        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.location();
        }

        // With no explicit IP, ipwho.is resolves the public address used by this backend.
        // This is useful only when the game and API are running on the same local machine.
        Optional<GeoLocation> location = fetch(publicIp ? ipAddress : null);
        Duration ttl = location.isPresent() ? CACHE_TTL : Duration.ofMinutes(10);
        cache.put(cacheKey, new CacheEntry(location, Instant.now().plus(ttl)));
        return location;
    }

    private Optional<GeoLocation> fetch(String ipAddress) {
        try {
            String encodedIp = ipAddress == null ? ""
                    : URLEncoder.encode(ipAddress, StandardCharsets.UTF_8);
            URI uri = URI.create(baseUrl + encodedIp + "?fields=ip," + FIELDS);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.debug("Geo-IP lookup returned HTTP {} for {}", response.statusCode(),
                        ipAddress == null ? "local public IP" : ipAddress);
                return Optional.empty();
            }

            JsonNode json = objectMapper.readTree(response.body());
            if (!json.path("success").asBoolean(false)) {
                return Optional.empty();
            }

            JsonNode connection = json.path("connection");
            return Optional.of(GeoLocation.builder()
                    .publicIpAddress(text(json, "ip"))
                    .country(text(json, "country"))
                    .countryCode(text(json, "country_code"))
                    .region(text(json, "region"))
                    .city(text(json, "city"))
                    .latitude(number(json, "latitude"))
                    .longitude(number(json, "longitude"))
                    .timezone(json.path("timezone").path("id").asText(null))
                    .isp(connection.path("isp").asText(null))
                    .resolvedAt(Instant.now())
                    .build());
        } catch (Exception ex) {
            log.debug("Geo-IP lookup failed for {}: {}",
                    ipAddress == null ? "local public IP" : ipAddress, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isPublicIp(String value) {
        if (value == null || value.isBlank() || !value.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isMulticastAddress()) {
                return false;
            }
            byte[] bytes = address.getAddress();
            return bytes.length != 4
                    || (Byte.toUnsignedInt(bytes[0]) != 100
                    || Byte.toUnsignedInt(bytes[1]) < 64
                    || Byte.toUnsignedInt(bytes[1]) > 127);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private Double number(JsonNode node, String field) {
        return node.hasNonNull(field) && node.get(field).isNumber()
                ? node.get(field).asDouble() : null;
    }

    private record CacheEntry(Optional<GeoLocation> location, Instant expiresAt) { }
}
