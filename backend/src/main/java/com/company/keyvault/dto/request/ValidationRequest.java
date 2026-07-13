package com.company.keyvault.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequest {

    @NotBlank(message = "License key is required")
    private String key;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Hardware ID is required")
    private String hardwareId;

    private String machineName;
}
