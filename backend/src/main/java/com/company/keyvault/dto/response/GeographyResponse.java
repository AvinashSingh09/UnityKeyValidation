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
public class GeographyResponse {

    private long activeDevices;
    private long locatedDevices;
    private long countries;
    private List<CountrySummary> countrySummary;
    private List<LocationPoint> locations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountrySummary {
        private String country;
        private String countryCode;
        private long devices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationPoint {
        private String keyId;
        private String licenseKey;
        private String productName;
        private String customerName;
        private String machineName;
        private String hardwareId;
        private String ipAddress;
        private String city;
        private String region;
        private String country;
        private String countryCode;
        private Double latitude;
        private Double longitude;
        private String isp;
        private Instant lastSeenAt;
    }
}
