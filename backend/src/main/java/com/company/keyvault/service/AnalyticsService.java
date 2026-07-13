package com.company.keyvault.service;

import com.company.keyvault.dto.response.DashboardResponse;
import com.company.keyvault.dto.response.AnalyticsInsightsResponse;
import com.company.keyvault.dto.response.GeographyResponse;
import com.company.keyvault.dto.response.KeyResponse;
import com.company.keyvault.dto.response.ProductOverviewResponse;
import com.company.keyvault.model.Activation;
import com.company.keyvault.model.GeoLocation;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.Product;
import com.company.keyvault.model.ValidationLog;
import com.company.keyvault.model.enums.KeyStatus;
import com.company.keyvault.model.enums.ValidationResult;
import com.company.keyvault.repository.LicenseKeyRepository;
import com.company.keyvault.repository.ProductRepository;
import com.company.keyvault.repository.ValidationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

    public AnalyticsInsightsResponse getInsights(int requestedDays) {
        int days = Math.max(7, Math.min(requestedDays, 90));
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant start = today.minusDays(days - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<ValidationLog> logs = logRepository.findByTimestampBetween(start, now);
        List<LicenseKey> keys = keyRepository.findAll();
        List<Product> products = productRepository.findAll();

        long successes = logs.stream()
                .filter(log -> log.getResult() == ValidationResult.SUCCESS).count();
        long failures = logs.size() - successes;

        Map<LocalDate, AnalyticsInsightsResponse.DailyMetric> daily = new HashMap<>();
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            daily.put(date, AnalyticsInsightsResponse.DailyMetric.builder()
                    .date(date.toString()).build());
        }
        for (ValidationLog log : logs) {
            if (log.getTimestamp() == null) continue;
            LocalDate date = log.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
            AnalyticsInsightsResponse.DailyMetric metric = daily.get(date);
            if (metric == null) continue;
            if (log.getResult() == ValidationResult.SUCCESS) {
                metric.setSuccesses(metric.getSuccesses() + 1);
                if ("ACTIVATE".equals(log.getAction()) && "ACTIVATED".equals(log.getReason())) {
                    metric.setActivations(metric.getActivations() + 1);
                }
            } else {
                metric.setFailures(metric.getFailures() + 1);
            }
        }

        Map<String, Long> failureCounts = logs.stream()
                .filter(log -> log.getResult() == ValidationResult.FAILED)
                .map(log -> log.getReason() == null ? "UNKNOWN" : log.getReason())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<AnalyticsInsightsResponse.ProductMetric> productMetrics = products.stream()
                .map(product -> buildProductMetric(product, keys, logs))
                .sorted(Comparator.comparingLong(AnalyticsInsightsResponse.ProductMetric::getValidations)
                        .thenComparingLong(AnalyticsInsightsResponse.ProductMetric::getActiveDevices)
                        .reversed())
                .toList();

        return AnalyticsInsightsResponse.builder()
                .periodDays(days)
                .generatedAt(now)
                .totalValidations(logs.size())
                .successfulValidations(successes)
                .failedValidations(failures)
                .successRate(percentage(successes, logs.size()))
                .activeDevices24h(countActiveDevices(keys, now.minus(24, ChronoUnit.HOURS)))
                .activeDevices7d(countActiveDevices(keys, now.minus(7, ChronoUnit.DAYS)))
                .activeDevices30d(countActiveDevices(keys, now.minus(30, ChronoUnit.DAYS)))
                .expiringIn7Days(countExpiring(keys, now, now.plus(7, ChronoUnit.DAYS)))
                .expiringIn30Days(countExpiring(keys, now, now.plus(30, ChronoUnit.DAYS)))
                .dailyMetrics(daily.values().stream()
                        .sorted(Comparator.comparing(AnalyticsInsightsResponse.DailyMetric::getDate))
                        .toList())
                .failureReasons(failureCounts.entrySet().stream()
                        .map(entry -> AnalyticsInsightsResponse.ReasonMetric.builder()
                                .reason(entry.getKey()).count(entry.getValue()).build())
                        .sorted(Comparator.comparingLong(AnalyticsInsightsResponse.ReasonMetric::getCount).reversed())
                        .limit(8)
                        .toList())
                .productMetrics(productMetrics)
                .build();
    }

    public ProductOverviewResponse getProductOverview(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.company.keyvault.exception.ResourceNotFoundException(
                        "Product not found with id: " + productId));
        List<LicenseKey> keys = keyRepository.findByProductId(productId);
        keys.sort(Comparator.comparing(LicenseKey::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        List<ValidationLog> periodLogs = logRepository.findByProductCodeAndTimestampBetween(
                product.getProductCode(), thirtyDaysAgo, now);
        List<ValidationLog> recentActivity = logRepository.findByProductCodeOrderByTimestampDesc(
                product.getProductCode(), PageRequest.of(0, 15)).getContent();
        long successes = periodLogs.stream()
                .filter(log -> log.getResult() == ValidationResult.SUCCESS).count();

        long devices = 0;
        long locatedDevices = 0;
        for (LicenseKey key : keys) {
            if (key.getActivations() == null) continue;
            devices += key.getActivations().size();
            locatedDevices += key.getActivations().stream()
                    .filter(activation -> activation.getLocation() != null
                            && activation.getLocation().getLatitude() != null
                            && activation.getLocation().getLongitude() != null)
                    .count();
        }

        return ProductOverviewResponse.builder()
                .product(product)
                .totalKeys(keyRepository.countByProductId(productId))
                .activeKeys(keyRepository.countByProductIdAndStatus(productId, KeyStatus.ACTIVE))
                .expiredKeys(keyRepository.countByProductIdAndStatus(productId, KeyStatus.EXPIRED))
                .suspendedKeys(keyRepository.countByProductIdAndStatus(productId, KeyStatus.SUSPENDED))
                .revokedKeys(keyRepository.countByProductIdAndStatus(productId, KeyStatus.REVOKED))
                .activeDevices(devices)
                .locatedDevices(locatedDevices)
                .validations30d(periodLogs.size())
                .failedValidations30d(periodLogs.stream()
                        .filter(log -> log.getResult() == ValidationResult.FAILED).count())
                .successRate30d(percentage(successes, periodLogs.size()))
                .expiringIn30Days(keys.stream()
                        .filter(key -> key.getStatus() == KeyStatus.ACTIVE)
                        .map(LicenseKey::getValidUntil)
                        .filter(Objects::nonNull)
                        .filter(expiry -> expiry.isAfter(now) && !expiry.isAfter(now.plus(30, ChronoUnit.DAYS)))
                        .count())
                .lastActivityAt(recentActivity.isEmpty() ? null : recentActivity.get(0).getTimestamp())
                .recentKeys(keys.stream().limit(25)
                        .map(key -> KeyResponse.from(key, product.getName())).toList())
                .recentActivity(recentActivity)
                .build();
    }

    private AnalyticsInsightsResponse.ProductMetric buildProductMetric(
            Product product, List<LicenseKey> keys, List<ValidationLog> logs) {
        List<LicenseKey> productKeys = keys.stream()
                .filter(key -> product.getId().equals(key.getProductId()))
                .toList();
        List<ValidationLog> productLogs = logs.stream()
                .filter(log -> Objects.equals(product.getProductCode(), log.getProductCode()))
                .toList();
        long productSuccesses = productLogs.stream()
                .filter(log -> log.getResult() == ValidationResult.SUCCESS).count();
        long devices = productKeys.stream()
                .map(LicenseKey::getActivations)
                .filter(Objects::nonNull)
                .mapToLong(List::size)
                .sum();

        return AnalyticsInsightsResponse.ProductMetric.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getName())
                .totalKeys(productKeys.size())
                .activeKeys(productKeys.stream().filter(key -> key.getStatus() == KeyStatus.ACTIVE).count())
                .activeDevices(devices)
                .validations(productLogs.size())
                .successRate(percentage(productSuccesses, productLogs.size()))
                .build();
    }

    private long countActiveDevices(List<LicenseKey> keys, Instant since) {
        return keys.stream()
                .map(LicenseKey::getActivations)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(activation -> {
                    Instant lastSeen = activation.getLastValidatedAt() != null
                            ? activation.getLastValidatedAt() : activation.getActivatedAt();
                    return lastSeen != null && !lastSeen.isBefore(since);
                })
                .count();
    }

    private long countExpiring(List<LicenseKey> keys, Instant start, Instant end) {
        return keys.stream()
                .filter(key -> key.getStatus() == KeyStatus.ACTIVE)
                .map(LicenseKey::getValidUntil)
                .filter(Objects::nonNull)
                .filter(expiry -> expiry.isAfter(start) && !expiry.isAfter(end))
                .count();
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) return 0;
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }
}
