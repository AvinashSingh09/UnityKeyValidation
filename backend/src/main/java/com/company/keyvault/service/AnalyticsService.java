package com.company.keyvault.service;

import com.company.keyvault.dto.response.DashboardResponse;
import com.company.keyvault.dto.response.GeographyResponse;
import com.company.keyvault.model.Activation;
import com.company.keyvault.model.GeoLocation;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.Product;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.model.enums.ValidationResult;
import com.company.keyvault.repository.LicenseKeyRepository;
import com.company.keyvault.repository.ProductRepository;
import com.company.keyvault.repository.ValidationLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final LicenseKeyRepository keyRepository;
    private final ProductRepository productRepository;
    private final ValidationLogRepository logRepository;

    public AnalyticsService(LicenseKeyRepository keyRepository,
                             ProductRepository productRepository,
                             ValidationLogRepository logRepository) {
        this.keyRepository = keyRepository;
        this.productRepository = productRepository;
        this.logRepository = logRepository;
    }

    public DashboardResponse getDashboardStats() {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        return DashboardResponse.builder()
                .totalKeys(keyRepository.count())
                .activeKeys(keyRepository.countByStatus(KeyStatus.ACTIVE))
                .expiredKeys(keyRepository.countByStatus(KeyStatus.EXPIRED))
                .revokedKeys(keyRepository.countByStatus(KeyStatus.REVOKED))
                .suspendedKeys(keyRepository.countByStatus(KeyStatus.SUSPENDED))
                .totalProducts(productRepository.count())
                .validationsToday(logRepository.countByResultAndTimestampBetween(
                        ValidationResult.SUCCESS, startOfDay, endOfDay))
                .failedValidationsToday(logRepository.countByResultAndTimestampBetween(
                        ValidationResult.FAILED, startOfDay, endOfDay))
                .build();
    }

    public GeographyResponse getGeography() {
        Map<String, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName,
                        (first, second) -> first));
        List<LicenseKey> keys = keyRepository.findAll();
        List<GeographyResponse.LocationPoint> locations = new ArrayList<>();
        long activeDevices = 0;

        for (LicenseKey key : keys) {
            List<Activation> activations = key.getActivations();
            if (activations == null) continue;
            activeDevices += activations.size();
            for (Activation activation : activations) {
                GeoLocation geo = activation.getLocation();
                if (geo == null || geo.getLatitude() == null || geo.getLongitude() == null) {
                    continue;
                }
                locations.add(GeographyResponse.LocationPoint.builder()
                        .keyId(key.getId())
                        .licenseKey(key.getKey())
                        .productName(productNames.getOrDefault(key.getProductId(), "Unknown"))
                        .customerName(key.getCustomerName())
                        .machineName(activation.getMachineName())
                        .hardwareId(activation.getHardwareId())
                        .ipAddress(geo.getPublicIpAddress() != null
                                ? geo.getPublicIpAddress() : activation.getIpAddress())
                        .city(geo.getCity())
                        .region(geo.getRegion())
                        .country(geo.getCountry())
                        .countryCode(geo.getCountryCode())
                        .latitude(geo.getLatitude())
                        .longitude(geo.getLongitude())
                        .isp(geo.getIsp())
                        .lastSeenAt(activation.getLastValidatedAt())
                        .build());
            }
        }

        locations.sort(Comparator.comparing(
                GeographyResponse.LocationPoint::getLastSeenAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, GeographyResponse.CountrySummary> countries = new HashMap<>();
        for (GeographyResponse.LocationPoint point : locations) {
            String code = point.getCountryCode() == null ? "--" : point.getCountryCode();
            GeographyResponse.CountrySummary current = countries.get(code);
            if (current == null) {
                countries.put(code, GeographyResponse.CountrySummary.builder()
                        .country(point.getCountry() == null ? "Unknown" : point.getCountry())
                        .countryCode(code)
                        .devices(1)
                        .build());
            } else {
                current.setDevices(current.getDevices() + 1);
            }
        }
        List<GeographyResponse.CountrySummary> summary = countries.values().stream()
                .sorted(Comparator.comparingLong(GeographyResponse.CountrySummary::getDevices).reversed())
                .toList();

        return GeographyResponse.builder()
                .activeDevices(activeDevices)
                .locatedDevices(locations.size())
                .countries(countries.size())
                .countrySummary(summary)
                .locations(locations.stream().limit(100).toList())
                .build();
    }
}
