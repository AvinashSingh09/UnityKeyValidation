package com.company.keyvault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private long totalKeys;
    private long activeKeys;
    private long expiredKeys;
    private long revokedKeys;
    private long suspendedKeys;
    private long totalProducts;
    private long validationsToday;
    private long failedValidationsToday;
}
