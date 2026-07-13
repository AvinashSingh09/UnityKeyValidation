package com.company.keyvault.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceUpdateRequest {

    @Size(max = 120)
    private String machineName;

    private Boolean trusted;
}
