package com.company.keyvault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsInsightsResponse {

    private int periodDays;
    private Instant generatedAt;
    private long totalValidations;
    private long successfulValidations;
    private long failedValidations;
    private double successRate;
    private long activeDevices24h;
    private long activeDevices7d;
    private long activeDevices30d;
    private long expiringIn7Days;
    private long expiringIn30Days;
    private List<DailyMetric> dailyMetrics;
    private List<ReasonMetric> failureReasons;
    private List<ProductMetric> productMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private String date;
        private long successes;
        private long failures;
        private long activations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReasonMetric {
        private String reason;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMetric {
        private String productId;
        private String productCode;
        private String productName;
        private long totalKeys;
        private long activeKeys;
        private long activeDevices;
        private long validations;
        private double successRate;
    }
}
