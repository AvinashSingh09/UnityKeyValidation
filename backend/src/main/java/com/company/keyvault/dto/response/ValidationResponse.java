package com.company.keyvault.dto.response;

import com.company.keyvault.model.enums.KeyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {

    private boolean valid;
    private String reason;
    private KeyStatus status;
    private Instant validUntil;
    private String productCode;
    private String customerName;
}
