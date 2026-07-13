package com.company.keyvault.dto.response;

import com.company.keyvault.model.Product;
import com.company.keyvault.model.ValidationLog;
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
public class ProductOverviewResponse {

    private Product product;
    private long totalKeys;
    private long activeKeys;
    private long expiredKeys;
    private long suspendedKeys;
    private long revokedKeys;
    private long activeDevices;
    private long locatedDevices;
    private long validations30d;
    private long failedValidations30d;
    private double successRate30d;
    private long expiringIn30Days;
    private Instant lastActivityAt;
    private List<KeyResponse> recentKeys;
    private List<ValidationLog> recentActivity;
}
