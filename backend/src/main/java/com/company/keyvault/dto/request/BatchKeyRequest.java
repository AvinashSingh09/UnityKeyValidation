package com.company.keyvault.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchKeyRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @Min(value = 1, message = "Count must be at least 1")
    private int count = 1;

    private String customerName;

    private com.company.keyvault.model.enums.KeyType type =
            com.company.keyvault.model.enums.KeyType.TIME_LIMITED;

    @Min(value = 1, message = "Max activations must be at least 1")
    private int maxActivations = 1;

    private Instant validFrom;

    private Instant validUntil;

    private String notes;
}
